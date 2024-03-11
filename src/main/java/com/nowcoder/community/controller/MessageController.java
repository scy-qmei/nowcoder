package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.annotation.CheckLogin;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
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
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
@RequestMapping("message")
public class MessageController implements CommunityConstants {
    @Autowired
    private MessageService messageService;
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(value = "list",method = RequestMethod.GET)
    @CheckLogin
    public String getMessageList(Model model, Page page) {
        //获取当前登录的用户
        User user = hostHolder.getUser();
        //设置分页属性
        page.setLimit(5);
        page.setPath("/message/list");
        page.setRows(messageService.selectConversationCount(user.getId()));
        //获取当前用户的会话列表
        List<Message> conversationList = messageService.selectConversationList(user.getId(), page.getOffset(), page.getLimit());
        //与帖子详情也类似，这里有大量需要展示的数据 ，所以创建一个map集合来存储
        List<Map<String, Object>> messageVoList = new ArrayList<>();
        int totalUnreadCount = 0;
        for (Message message : conversationList) {
            Map<String, Object> conversationMap = new HashMap<>();
            //加入当前会话的最后一条消息
            conversationMap.put("conversion", message);
            //加入当前会话的对话用户，注意该用户可能是最后一条消息的发送者/接受者，所以先判断当前用户的id是发送者/还是接受者，然后对话用户就是另一个
            int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
            User target = userService.getUserById(targetId);
            conversationMap.put("target", target);
            //当前会话的消息数量
            int count = messageService.selectMessageCount(message.getConversationId());
            conversationMap.put("count", count);
            //当前会话的未读消息
            int unReadMessageCount = messageService.selectUnReadMessageCount(user.getId(),message.getConversationId());
            conversationMap.put("unread", unReadMessageCount);
            messageVoList.add(conversationMap);
            totalUnreadCount += unReadMessageCount;
        }
        int totalUnreadNotice = messageService.selectUnreadNoticeCount(user.getId(), null);

        model.addAttribute("totalUnreadNotice", totalUnreadNotice);
        model.addAttribute("totalUnread",totalUnreadCount);
        model.addAttribute("convers", messageVoList);
        return "/site/letter";
    }
    @RequestMapping(value = "detail/{conversionId}",method = RequestMethod.GET)
    public String getMessageDetail(Model model, Page page, @PathVariable String conversionId) {
        //获取当前登录的用户
        User user = hostHolder.getUser();
        //设置分页属性
        page.setLimit(5);
        page.setPath("/message/detail/" + conversionId);
        page.setRows(messageService.selectMessageCount(conversionId));
        //查找当前会话的当前页的消息列表
        List<Message> messageList = messageService.selectMessageList(conversionId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> messagesVo = new ArrayList<>();
        for (Message message : messageList) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", message);
            int fromId = message.getFromId();
            User userById = userService.getUserById(fromId);
            map.put("sender", userById);
            messagesVo.add(map);
        }

        model.addAttribute("messages", messagesVo);
        User userByConversionId = getUserByConversionId(conversionId);
        model.addAttribute("target", userByConversionId);

        List<Integer> unreadMessage = getUnreadMessage(messageList);
        //逻辑严谨一些，使用集合/数据前先判断是否非空！
        if (!unreadMessage.isEmpty()) {
            messageService.updateReadMessage(unreadMessage);
        }
        return "/site/letter-detail";
    }
    private User getUserByConversionId(String conversionId) {
        User user = hostHolder.getUser();
        String[] s = conversionId.split("_");
        User target = null;
        for (String s1 : s) {
            if (user.getId() == Integer.parseInt(s1)) continue;
            else target = userService.getUserById(Integer.parseInt(s1));
        }
        return target;
    }
    private List<Integer> getUnreadMessage(List<Message> messages) {
        List<Integer> res = new ArrayList<>();
        for (Message message : messages) {
            //确保当前用户是消息的接收方，并且消息的状态是未读，才对消息进行更新！！！
            if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                res.add(message.getId());
            }
        }
        return res;
    }

    /**
     * 该handler是用来实现异步发送私信的功能的，因为这里采用了异步，所以通常是保存请求提交的数据，然后响应给浏览器一个提示消息，所以这里返回的不是html
     * 因此用responsebody注解
     * @param toName
     * @param content
     * @return
     */
    @RequestMapping(value = "add",method = RequestMethod.POST)
    @ResponseBody
    public String addMessage(String toName, String content) {
        User target = userService.getUserByName(toName);
        if (target == null) {
            return CommunityUtil.getJsonString(1,"你要发送消息的用户不存在");
        }
        Message message = new Message();
        message.setContent(content);
        message.setFromId(hostHolder.getUser().getId());
        message.setCreateTime(new Date());
        message.setToId(target.getId());
        if (message.getToId() > message.getFromId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        messageService.insertMessage(message);

        return CommunityUtil.getJsonString(0,"发送成功!");
    }

    @RequestMapping(value = "notice/list",method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        User user = hostHolder.getUser();

        //封装评论数据
        Map<String, Object> commentMap = new HashMap<>();
        Message commentMsg = messageService.selectLastestNotice(user.getId(), TOPIC_COMMENT);
        //逻辑要严谨，使用数据前首先判空！！！
        int unreadCommentCount = 0;
        commentMap.put("message", commentMsg);
        if (commentMsg != null) {

            int totalCommentCount = messageService.selectTotalNoticeCount(user.getId(), TOPIC_COMMENT);
            commentMap.put("totalCount",totalCommentCount);

            unreadCommentCount = messageService.selectUnreadNoticeCount(user.getId(), TOPIC_COMMENT);
            commentMap.put("unreadCount",unreadCommentCount);

            //注意我们的message的内容存入数据库的时候经过了HTML的转义处理，这里要转义回来
            String commentContent = HtmlUtils.htmlUnescape(commentMsg.getContent());
            Map<String, Object> commentDataMap = JSONObject.parseObject(commentContent, Map.class);

            User commentUser = userService.getUserById((Integer) commentDataMap.get("userId"));
            commentMap.put("user",commentUser);
            commentMap.put("entityType",commentDataMap.get("entityType"));
            commentMap.put("entityId",commentDataMap.get("entityId"));
            commentMap.put("postId", commentDataMap.get("postId"));
        }


        //封装点赞数据
        int unreadlikeCount = 0;
        Map<String, Object> likeMap = new HashMap<>();
        Message likeMsg = messageService.selectLastestNotice(user.getId(), TOPIC_LIKE);
        likeMap.put("message", likeMsg);
        if (likeMsg != null) {

            int totallikeCount = messageService.selectTotalNoticeCount(user.getId(), TOPIC_LIKE);
            likeMap.put("totalCount",totallikeCount);

            unreadlikeCount = messageService.selectUnreadNoticeCount(user.getId(), TOPIC_LIKE);
            likeMap.put("unreadCount",unreadlikeCount);

            //注意我们的message的内容存入数据库的时候经过了HTML的转义处理，这里要转义回来
            String likeContent = HtmlUtils.htmlUnescape(likeMsg.getContent());
            Map<String, Object> likeDataMap = JSONObject.parseObject(likeContent, Map.class);

            User likeUser = userService.getUserById((Integer) likeDataMap.get("userId"));
            likeMap.put("user",likeUser);
            likeMap.put("entityType",likeDataMap.get("entityType"));
            likeMap.put("entityId",likeDataMap.get("entityId"));
            likeMap.put("postId", likeDataMap.get("postId"));
        }


        //封装关注数据
        int unreadfollowCount = 0;
        Map<String, Object> followMap = new HashMap<>();
        Message followMsg = messageService.selectLastestNotice(user.getId(), TOPIC_FOLLOW);
        //这里一定先放入message，再判断是否非空，因为在前端的页面判断中是需要用到message属性的！
        followMap.put("message", followMsg);
        if (followMsg != null) {

            int totalfollowCount = messageService.selectTotalNoticeCount(user.getId(), TOPIC_FOLLOW);
            followMap.put("totalCount",totalfollowCount);

            unreadfollowCount = messageService.selectUnreadNoticeCount(user.getId(), TOPIC_FOLLOW);
            followMap.put("unreadCount",unreadfollowCount);

            //注意我们的message的内容存入数据库的时候经过了HTML的转义处理，这里要转义回来
            String followContent = HtmlUtils.htmlUnescape(followMsg.getContent());
            Map<String, Object> followDataMap = JSONObject.parseObject(followContent, Map.class);

            User followUser = userService.getUserById((Integer) followDataMap.get("userId"));
            followMap.put("user",followUser);
            followMap.put("entityType",followDataMap.get("entityType"));
            followMap.put("entityId",followDataMap.get("entityId"));
        }

        int totalUnreadMsg = messageService.selectUnReadMessageCount(user.getId(), null);

        model.addAttribute("comment",commentMap);
        model.addAttribute("like",likeMap);
        model.addAttribute("follow",followMap);
        model.addAttribute("totalUnreadMsg",totalUnreadMsg);
        model.addAttribute("totalUnread", unreadCommentCount + unreadfollowCount + unreadlikeCount);
        return "/site/notice";
    }

    @RequestMapping(value = "notice/detail/{topic}",method = RequestMethod.GET)
    public String getCommentNoticeDetail(@PathVariable("topic") String topic,Model model, Page page) {
        User user = hostHolder.getUser();
        //设置分页属性
        page.setLimit(5);
        page.setRows(messageService.selectTotalNoticeCount(user.getId(), topic));
        page.setPath("/message/notice/detail/" + topic);

        List<Message> noticeList = messageService.selectNoticeList(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        for(Message message : noticeList) {
            Map<String,Object> noticeVo = new HashMap<>();

            noticeVo.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> msgContent = JSONObject.parseObject(content, Map.class);

            User userId = userService.getUserById((Integer) msgContent.get("userId"));
            noticeVo.put("user", userId);
            noticeVo.put("entityType", msgContent.get("entityType"));
            noticeVo.put("entityId", msgContent.get("entityId"));
            noticeVo.put("postId", msgContent.get("postId"));
            //添加系统用户的信息,这里因为系统通知的发送者都是系统用户，即id为1的！
            noticeVo.put("fromUser", userService.getUserById(message.getFromId()));

            noticeVoList.add(noticeVo);
        }
        List<Integer> unreadList = getUnreadMessage(noticeList);
        //这里已经实例化了，所以肯定不为null，所以判断条件是判断集合是否为空
        if (!unreadList.isEmpty()) {
            messageService.updateReadMessage(unreadList);
        }
        model.addAttribute("topic", topic);
        model.addAttribute("notices", noticeVoList);
        return "/site/notice-detail";
    }
}
