package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
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
}
