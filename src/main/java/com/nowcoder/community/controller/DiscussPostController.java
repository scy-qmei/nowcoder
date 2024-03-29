package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.CheckLogin;
import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Path;
import java.util.*;

@RequestMapping("discuss")
@Controller
public class DiscussPostController implements CommunityConstants {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        discussPost.setUserId(hostHolder.getUser().getId());
        discussPost.setType(0);
        discussPost.setStatus(0);
        discussPost.setCommentCount(0);
        discussPostService.addDiscussPost(discussPost);

        //添加帖子成功，创建一个事件对象放入消息队列，供消费者消费将帖子更新到ES
        Event event = new Event()
                .setTopic(ES_DISCUSSPOST_UPDATE)
                .setEntityType(COMMENT_TYPE_POST)
                .setEntityId(discussPost.getId())
                .setEntityUserId(discussPost.getUserId())
                .setUserId(hostHolder.getUser().getId());

        eventProducer.fireEvent(event);
        //添加新帖子，给帖子一个初始化分数
        String postScorekey = RedisKeyUtil.getPostScorekey();
        redisTemplate.opsForSet().add(postScorekey, discussPost.getId());

        return CommunityUtil.getJsonString(0, "发送帖子成功");
    }
    @RequestMapping(value = "detail/{id}",method = RequestMethod.GET)
    public String getDiscussPostDetail(@PathVariable("id") int id, Model model, Page page) {
        //显示帖子详情
        //这里添加显示的帖子内容以及发布帖子的用户信息
        DiscussPost discussPost = discussPostService.selectDiscussPostById(id);
        User userById = userService.getUserById(discussPost.getUserId());
        model.addAttribute("discussPost", discussPost);
        model.addAttribute("author", userById);
        //获取帖子的点赞信息
        User user = hostHolder.getUser();
        Long postLikeCount = likeService.getLikeCount(COMMENT_TYPE_POST, discussPost.getId());;
        int postLikeStatus = 0;
        if (user != null) {
             postLikeStatus = likeService.getLikeStatus(user.getId(), COMMENT_TYPE_POST, discussPost.getId());
        }
        model.addAttribute("likeCount",postLikeCount);
        model.addAttribute("likeStatus", postLikeStatus);
        //帖子的评论以及评论的回复信息

        //帖子的评论要分页显示，这里设置分页信息
        page.setLimit(5);
        page.setRows(discussPost.getCommentCount());
        page.setPath("/discuss/detail/" + id);
        //获取帖子的一页的评论集合
        List<Comment> comments = commentService.selectCommentByEntity(COMMENT_TYPE_POST, discussPost.getId(),
                page.getOffset(), page.getLimit());
        //这里的VO指的是view object即显示对象，表示该集合存储的是用于模版显示的信息
        //这里的一个map存储的就是一个类型的信息，比如评论，评论的用户等
        List<Map<String,Object>> commentVoList = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String,Object> commentMap = new HashMap<>();
            User userById1 = userService.getUserById(comment.getUserId());
            //集合中放置当前评论以及评论的用户，为后续的显示作准备
            commentMap.put("comment", comment);
            commentMap.put("user", userById1);
            //评论的点赞信息
            Long commentLikeCount = likeService.getLikeCount(COMMENT_TYPE_REPLY, comment.getId());
            int commentLikeStatus = 0;
            if (user != null) {
                likeService.getLikeStatus(user.getId(), COMMENT_TYPE_REPLY, comment.getId());
            }
            commentMap.put("likeCount",commentLikeCount);
            commentMap.put("likeStatus",commentLikeStatus);
            //这里切记每条评论还有他们的回复，所以评论的回复也要放入Vo中进行显示，这里的逻辑与评论的逻辑一样
            List<Map<String, Object>> replyVoList = new ArrayList<>();
            //这里因为评论的回复数量不需要做分页处理，所以直接显示所有的回复！
            List<Comment> comments1 = commentService.selectCommentByEntity(COMMENT_TYPE_REPLY, comment.getId(),
                    0, Integer.MAX_VALUE);
            for (Comment reply : comments1) {
                Map<String,Object> replyMap = new HashMap<>();
                User userById2 = userService.getUserById(reply.getUserId());
                //这里放置的是每个评论的回复以及回复的用户，供后面的显示做准备
                replyMap.put("reply", reply);
                replyMap.put("user", userById2);
                //回复的点赞消息
                Long replyLikeCount = likeService.getLikeCount(COMMENT_TYPE_REPLY, reply.getId());
                int replyLikeStatus = 0;
                if (user != null) {
                    likeService.getLikeStatus(user.getId(), COMMENT_TYPE_REPLY, reply.getId());
                }
                replyMap.put("likeCount",replyLikeCount);
                replyMap.put("likeStatus", replyLikeStatus);
                //这里要根据回复是回复别人的回复还是回复的帖子做不同的显示，所以需要把回复对象也加入vo
                User targetUser = reply.getTargetId() == 0 ? null : userService.getUserById(reply.getTargetId());
                replyMap.put("target", targetUser);
                replyVoList.add(replyMap);
            }
            //将评论的回复放入vo，以供显示
            commentMap.put("replys", replyVoList);
            //这里放置的每个评论的回复的数量，以供后面进行显示
            commentMap.put("replyCount", comments1.size());
            commentVoList.add(commentMap);

        }
        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail";
    }

    /**
     * 对帖子进行置顶，这里采用的是异步请求
     * @param postId
     * @return
     */
    @RequestMapping(value = "top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int postId) {
        discussPostService.updateDiscussPostType(postId, 1);

        //对帖子进行更新后，要及时的同步到es中，方便搜索
        Event event = new Event()
                .setTopic(ES_DISCUSSPOST_UPDATE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(COMMENT_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJsonString(0);
    }
    /**
     * 对帖子进行加精，这里采用的是异步请求
     * @param postId
     * @return
     */
    @RequestMapping(value = "wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int postId) {
        discussPostService.updateDiscussPostStatus(postId, 1);

        //对帖子进行更新后，要及时的同步到es中，方便搜索
        Event event = new Event()
                .setTopic(ES_DISCUSSPOST_UPDATE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(COMMENT_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);
        //对帖子进行加精了，也需要计算分数，加入缓存
        String postScorekey = RedisKeyUtil.getPostScorekey();
        redisTemplate.opsForSet().add(postScorekey, postId);
        return CommunityUtil.getJsonString(0);
    }
    /**
     * 对帖子进行删除，这里采用的是异步请求
     * @param postId
     * @return
     */
    @RequestMapping(value = "delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int postId) {
        discussPostService.updateDiscussPostStatus(postId, 2);

        //对帖子进行删除后，要及时的同步到es中，但这里是删除事件，要额外编写
        Event event = new Event()
                .setTopic(ES_DISCUSSPOST_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(COMMENT_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJsonString(0);
    }
}
