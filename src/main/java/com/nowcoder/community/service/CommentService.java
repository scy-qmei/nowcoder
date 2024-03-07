package com.nowcoder.community.service;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.mapper.CommentMapper;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstants {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    SensitiveFilter sensitiveFilter;

    public List<Comment> selectCommentByEntity(int entityType,
                                               int entityId,
                                               int offset,
                                               int limit) {
        return commentMapper.selectCommentByEntity(entityType,entityId,offset,limit);
    }
    public int selectCommentCountByEntity(int entityType, int entityId) {
        return commentMapper.selectCommentCountByEntity(entityType,entityId);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public void insertComment(Comment comment) {
        if (comment == null) throw new IllegalArgumentException("参数不能为空");
        //过滤评论的内容
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.sensitiveReplace(comment.getContent()));
        //插入评论
        commentMapper.insertComment(comment);
        //注意这里除了插入评论，还要判断如果评论的是帖子，就更新帖子的评论数量,所以此方法要开启事务
        if (comment.getEntityType() == COMMENT_TYPE_POST) {
            DiscussPost discussPost = discussPostService.selectDiscussPostById(comment.getEntityId());
            discussPostService.updateDiscussPosyComment(comment.getEntityId(), discussPost.getCommentCount() + 1);
        }
    }

    public Comment selectCommentById(int commentId) {
        return commentMapper.selectCommentById(commentId);
    }
}
