package com.nowcoder.community.service;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.util.SensitiveFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode) {
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }
    public int getDiscussPostRows(int userId) {
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
