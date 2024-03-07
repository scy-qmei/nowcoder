package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.CheckLogin;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("message")
public class MessageController {
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
}
