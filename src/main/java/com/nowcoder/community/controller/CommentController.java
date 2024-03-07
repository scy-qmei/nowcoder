package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.CheckLogin;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("comment")
public class CommentController implements CommunityConstants {
    @Autowired
    private CommentService commentService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private HostHolder hostHolder;
    @RequestMapping(value = "add/{postId}",method = RequestMethod.POST)
    @CheckLogin
    public String addComment(@PathVariable int postId, Comment comment) {
        //这里controller在插入数据之前，要对comment的信息做一个完善！
        comment.setUserId(hostHolder.getUser().getId());
        comment.setCreateTime(new Date());
        comment.setStatus(0);
        commentService.insertComment(comment);
        //评论成功之后，触发响应的评论时间的系统通知
        //因为触发事件要传入event对象，这里先封装一个事件对象
        //这里通过链式编程，更加简便
        //这里加入帖子id是为了前端点击系统通知可以跳转到相应的页面，注意这里实体的id可能是评论也可能是帖子，所以不能把帖子id作为值赋给实体id！
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setEntityType(comment.getEntityType())
                .setUserId(hostHolder.getUser().getId())
                .setEntityId(comment.getEntityId())
                .setData("postId",postId);
        //这里要获取评论的实体的发布者的id，而这个实体可能是帖子，也可能是评论，所以要做不同的判断从而从不同的表里获取数据
        DiscussPost discussPost = discussPostService.selectDiscussPostById(comment.getEntityId());
        if (comment.getEntityType() == COMMENT_TYPE_POST) {
            //如果评论的实体是帖子，就从帖子表获取其发布者
            event.setEntityUserId(discussPost.getUserId());
        } else if (comment.getEntityType() == COMMENT_TYPE_REPLY) {
            //如果评论的实体是评论，那么从评论表中查询评论的用户id，即通知该用户被回复了，但是注意帖子的发布者也会受到回复消息，所以这里也要加入帖子的拥有者！
            Comment comment1 = commentService.selectCommentById(comment.getEntityId());
            event.setEntityUserId(comment1.getUserId());
            event.setData("discussPostOwner", discussPost.getUserId());
        }
        //实体封装完成，触发事件即可
        eventProducer.fireEvent(event);
        return "redirect:/discuss/detail/" + postId;
    }
}
