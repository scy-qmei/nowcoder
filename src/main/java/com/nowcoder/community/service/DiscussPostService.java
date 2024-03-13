package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.SensitiveFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;
    @Value("${caffeine.posts.expire-time}")
    private int expireTime;
    //存储热帖列表的缓存，一个列表就是一页数据
    private LoadingCache<String, List<DiscussPost>> postListCache;
    //存储贴子总数量的缓存
    private LoadingCache<Integer, Integer> postRowsCache;
    //缓存的初始化一次就够了，所以这里设置一个初始化方法
    @PostConstruct
    public void init() {
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(maxSize, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    //该方法是如果缓存里没有数据，从哪里去寻找数据放入缓存，这里的key就是缓存的数据对应的key
                    public @Nullable List<DiscussPost> load(String key) throws Exception {
                        if (key == null || StringUtils.isBlank(key)) {
                            throw new IllegalArgumentException("参数错误");
                        }
                        String[] split = key.split(":");
                        if (split == null || split.length != 2) {
                            throw new IllegalArgumentException("参数错误");
                        }
                        int offset = Integer.parseInt(split[0]);
                        int limit = Integer.parseInt(split[1]);

                        //这里可以查询二级缓存redis
                        String postListKey = RedisKeyUtil.getPostListKey(offset, limit);
                        List<DiscussPost> discussPostsRedis = (List<DiscussPost>) redisTemplate.opsForValue().get(postListKey);
                        if (discussPostsRedis!=null) {
                            log.info("loading post data from redis");
                            return discussPostsRedis;
                        }
                        //如果都没有，从数据库中查询，返回结果
                        log.info("loading post data from DB");
                        //注意这里如果没有缓存，需要从数据库里获取数据更新到缓存，而访问数据库应该调用DAO层，而不是service层，否则会陷入死递归！
                        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                        //缓存同步到redis
                        redisTemplate.opsForValue().set(postListKey, discussPosts);
                        return discussPosts;
                    }
                });
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireTime, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(Integer integer) throws Exception {
                        if (integer == null || integer != 0) {
                            throw new IllegalArgumentException("参数错误");
                        }
                        //redis
                        String postRowsKey = RedisKeyUtil.getPostRowsKey(integer);
                        Integer rows = (Integer) redisTemplate.opsForValue().get(postRowsKey);
                        if (rows != null) {
                            log.info("loading post data from redis");
                            return rows;
                        }

                        log.info("loading post data from DB");
                        int discussPostRows = discussPostMapper.getDiscussPostRows(0);
                        redisTemplate.opsForValue().set(postRowsKey, discussPostRows);
                        return discussPostRows;
                    }
                });
    }

    public List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode) {
        //如果查询的是热帖，启用缓存
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(offset + ":" + limit);
        }
        //查询缓存失败，查找数据库
        log.info("loading post data from DB");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }
    public int getDiscussPostRows(int userId) {
        //如果查询帖子总数，就启动缓存
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        log.info("loading post data from DB");
        return discussPostMapper.getDiscussPostRows(userId);
    }
    public int addDiscussPost(DiscussPost discussPost) {
        if (discussPost == null) {
            throw new IllegalArgumentException("参数错误");
        }
        //这里调用html的工具包来完成对标题/文本内容中包含的html语法的转义，防止恶意的格式显示
        HtmlUtils.htmlEscape(discussPost.getTitle());
        HtmlUtils.htmlEscape(discussPost.getContent());
        //敏感词过滤
        discussPost.setTitle(sensitiveFilter.sensitiveReplace(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.sensitiveReplace(discussPost.getContent()));
        return discussPostMapper.addDiscussPort(discussPost);
    }
    public DiscussPost selectDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }
    public void updateDiscussPosyComment(int id, int commentCount) {
        discussPostMapper.updateDiscussPostComment(id, commentCount);
    }
    public void updateDiscussPostType(int postId, int type) {
        discussPostMapper.updateDiscussPostType(postId, type);
    }
    public void updateDiscussPostStatus(int postId, int status){
        discussPostMapper.updateDiscussPostStatus(postId, status);
    }
    public void updateDiscussPostScore(int postId, double score) {
        discussPostMapper.updateDiscussPostScore(postId, score);
    }
}
