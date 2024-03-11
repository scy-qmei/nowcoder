package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.CheckLogin;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstants {
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 该业务就是执行实体的点赞业务，由于点赞业务通常是异步请求的方式，所以此时返回的是提示信息而不是html页面
     *
     * @param entityType
     * @param entityId
     * @return
     */
    @RequestMapping(value = "like",method = RequestMethod.POST)
    @ResponseBody
    @CheckLogin
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();
        //执行点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        //点赞完毕，查询点赞状态与点赞数量，返回给前端进行显示
        Long likeCount = likeService.getLikeCount(entityType, entityId);
        int likeStatus = likeService.getLikeStatus(user.getId(), entityType, entityId);
        //放入map，以生成最后的结果json串
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        //触发点赞事件，发送响应的系统通知
        //注意这里点赞的系统通知，也需要有点击通知跳转到点赞所在的帖子的功能，所以这里也需要帖子id
        //而在这个方法中获取帖子id是比较麻烦的，所以可以通过前段额外穿过来一个帖子id的参数
        //注意因为点赞有两种状态，点赞或者取消赞，这里只有点赞才发送系统通知，取消赞就不发了，影响不好！
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setUserId(user.getId())
                    .setData("postId", postId);
            //触发事件
            eventProducer.fireEvent(event);
        }

        //对帖子进行点赞，需要涉及分数的修改，加入缓存等待被修改
        if (entityType == COMMENT_TYPE_POST) {
            String postScorekey = RedisKeyUtil.getPostScorekey();
            redisTemplate.opsForSet().add(postScorekey, entityId);
        }


        return CommunityUtil.getJsonString(0,null,map);
    }
}
