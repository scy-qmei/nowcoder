package com.nowcoder.community.jog;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticSearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 该类用来定义帖子分数重新计算的任务！
 */
@Slf4j
public class PostScoreRefreshJob implements Job, CommunityConstants {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private LikeService likeService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticSearchService elasticSearchService;

    private static final Date epoch;
    //在静态代码块中实例化epoch，保证只被实例化一次
    static {
        try {
            //对牛客创建的日期进行格式化为年月日赋予epoch
            epoch = new SimpleDateFormat("yyyy-MM-dd").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //任务的执行，首先从缓存中获取待修改的帖子
        String postScorekey = RedisKeyUtil.getPostScorekey();
        //将redistemplate和对应的key进行绑定，以简化api的调用
        BoundSetOperations operations = redisTemplate.boundSetOps(postScorekey);
        //如果待修改的帖子的数量为0，就不执行任务
        if (operations.size() == 0) {
            log.info("停止执行帖子分数刷新任务，没有待更新分数的帖子");
            return;
        }
        log.info("[开始更新帖子的分数]");

        while(operations.size() != 0) {
            //如果有待修改的帖子，就从set中弹出一个帖子id,当所有的帖子处理完毕了，此时set集合就为空了！！！！！
            Integer id = (Integer) operations.pop();
            //对帖子进行分数刷新操作
            this.refresh(id);
        }
        log.info("[更新帖子的分数操作已经完成]");
    }
    public void refresh(int postId) {
        DiscussPost discussPost = discussPostService.selectDiscussPostById(postId);
        //如果要修改分数的帖子已经被管理员删除了，就不进行刷新操作，在日志中打印错误
        if (discussPost == null) {
            log.error("要刷新分数的帖子不存在，帖子的id为:" + postId);
            return;
        }
        //如果帖子存在，就计算当前帖子的分数，进行更新
        //获取帖子加精状态
        boolean wonderful = discussPost.getStatus() == 1;
        //获取帖子的评论数量
        int commentCount = discussPost.getCommentCount();
        //获取帖子的点赞数量
        long likeCount = likeService.getLikeCount(COMMENT_TYPE_POST, postId);
        //计算帖子的权重
        double weight = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        //计算帖子发布离牛客纪元的天数
        //即帖子发布的毫秒数-牛客创建日期的毫秒数，然后除以一天所有的毫秒数，即可得到天数
        long days = (discussPost.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        //计算帖子的分数
        //注意这里weight可能小于1，这样log以10为底得到的分数为负数，我们不想为负数，此时给一个0分即可！
        double score = Math.log10(Math.max(1, weight)) + days;

        //更新数据库信息
        discussPostService.updateDiscussPostScore(postId, score);
        //更新这里查询出来的实体的信息，以同步到es
        discussPost.setScore(score);
        elasticSearchService.insertDiscussPost(discussPost);
    }
}
