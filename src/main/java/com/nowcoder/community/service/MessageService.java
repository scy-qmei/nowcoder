package com.nowcoder.community.service;

import com.nowcoder.community.entity.Message;
import com.nowcoder.community.mapper.MessageMapper;
import com.nowcoder.community.util.SensitiveFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> selectConversationList(int userId, int offset, int limit) {
        return messageMapper.selectConversationList(userId, offset, limit);
    }
    public int selectConversationCount(int userId) {
        return messageMapper.selectConversionCount(userId);
    }
    public List<Message> selectMessageList(String conversationId, int offset, int limit) {
        return messageMapper.selectMessageList(conversationId, offset, limit);
    }
    public int selectMessageCount(String conversationId) {
        return messageMapper.selectMessageCount(conversationId);
    }
    public int selectUnReadMessageCount(int userId, String conversationId) {
        return messageMapper.selectUnReadMessageCount(userId, conversationId);
    }
    public void insertMessage(Message message) {
        if (message == null) throw new IllegalArgumentException("参数为空");
        //在向数据库插入内容之前一定要对文本内容进行过滤
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.sensitiveReplace(message.getContent()));
        messageMapper.insertMessage(message);
    }

//    public void updateReadMessage(Message message) {
//        messageMapper.updateReadMessage(message);
//    }
    public void updateReadMessage(List<Integer> ids) {
        messageMapper.updateReadMessage(ids);
    }

    public Message selectLastestNotice(int toId, String topic) {
        return messageMapper.selectLatestNotice(toId, topic);
    }
    public int selectTotalNoticeCount(int toId, String topic) {
        return messageMapper.selectTotalNoticeCount(toId, topic);
    }
    public int selectUnreadNoticeCount(int toId, String topic){
        return messageMapper.selectUnreadNoticeCount(toId, topic);
    }
    public List<Message> selectNoticeList(int toId, String conversationId, int offset, int limit) {
        return messageMapper.selectNoticeList(toId, conversationId, offset, limit);
    }
}
