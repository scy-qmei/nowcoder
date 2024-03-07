package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstants{
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private FollowService followService;
    @Autowired
    private EventProducer eventProducer;
    /**
     * 这里点赞也是一个异步请求，不需要刷新浏览器进行的请求，所以返回的不是html页面
     * @param entityType 关注的实体类型
     * @param entityId 关注的实体的id
     * @return
     */
    @RequestMapping(value = "follow",method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId, Model model) {
        User user = hostHolder.getUser();

        followService.follow(user.getId(), entityType, entityId);
        //与点赞同理，这里只有关注再发送系统通知
        //这里目前只能关注人，所以就将实体的拥有者的id也设置为实体的id
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setEntityType(entityType)
                .setUserId(user.getId())
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJsonString(0, "关注成功");
    }
    @RequestMapping(value = "unfollow",method = RequestMethod.POST)
    @ResponseBody
    public String unFollow(int entityType, int entityId, Model model) {
        User user = hostHolder.getUser();

        followService.unfollow(user.getId(), entityType, entityId);


        return CommunityUtil.getJsonString(0, "取消关注成功");
    }

    @RequestMapping(value = "followees/{userId}",method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Model model, Page page) {
        //存储用户的信息，以供前端显示,这里不能是获取当前登录用户，应该获取页面传过来的id的用户，以显示正确的信息
        User user = userService.getUserById(userId);
        model.addAttribute("user", user);
        //设置分页属性
        page.setLimit(5);
        page.setRows((int) followService.getFolloweeCount(userId, CommunityConstants.ENTITY_TYPE_USER));
        page.setPath("/followees/" + userId);
        //获取用户的关注列表
        List<Map<String, Object>> followeeList = followService.getFolloweeList(userId, page.getOffset(), page.getLimit());
        //因为前端还需要显示每一个用户的关注状态，所以这里遍历集合，去查询用户的关注状态放入map
        for (Map<String, Object> map : followeeList) {
            User user1 = (User) map.get("followee");
            boolean hasFollowed = false;
            if (hasFollowed(user.getId(), ENTITY_TYPE_USER, user1.getId())) {
                hasFollowed = true;
            }
            map.put("hasFollowed", hasFollowed);
        }
        model.addAttribute("followees",followeeList);
        return "/site/followee";
    }
    @RequestMapping(value = "followers/{userId}",method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Model model, Page page) {
        //存储当前登录用户的信息，以供前端显示
        User user = userService.getUserById(userId);
        model.addAttribute("user", user);
        //设置分页属性
        page.setLimit(5);
        page.setRows((int) followService.getFollowerCount(CommunityConstants.ENTITY_TYPE_USER, userId));
        page.setPath("/followers/" + userId);
        //获取用户的关注列表
        List<Map<String, Object>> followerList = followService.getFollowerList(userId, page.getOffset(), page.getLimit());
        //因为前端还需要显示每一个用户的关注状态，所以这里遍历集合，去查询用户的关注状态放入map
        for (Map<String, Object> map : followerList) {
            User user1 = (User) map.get("follower");
            boolean hasFollowed = false;
            if (hasFollowed(user.getId(), ENTITY_TYPE_USER, user1.getId())) {
                hasFollowed = true;
            }
            map.put("hasFollowed", hasFollowed);
        }
        model.addAttribute("followers",followerList);
        return "/site/follower";
    }
    //查询用户关注状态封装为一个方法，方便复用
    private boolean hasFollowed(int userId, int entityType, int entityId) {
        User user = hostHolder.getUser();
        if (user == null) {
            throw new IllegalArgumentException("用户为空");
        }
        return followService.getFollowStatus(userId,entityType,entityId);
    }

}
