package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.mapper.LoginTicketMapper;
import com.nowcoder.community.mapper.MessageMapper;
import com.nowcoder.community.mapper.UserMapper;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.function.Supplier;

@SpringBootTest
class NowcoderCommunityApplicationTests {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private MessageMapper messageMapper;
    @Test
    void contextLoads() {
//        User user = userMapper.selectByName("liubei");
//        System.out.println(user);
//        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, 0, 10);
//        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, 0, 10);
//        for (DiscussPost discussPost : discussPosts) {
//            System.out.println(discussPost);
////        }
//        Logger logger = LoggerFactory.getLogger(NowcoderCommunityApplicationTests.class);
//        logger.error(new Supplier<String>() {
//            @Override
//            public String get() {
//                return "sdfdsfs";
//            }
//        });
        mailClient.sentMail("996812464@qq.com", "test", "ssfffddfs");

    }
    @Test
    public void mailThymeleaf() {
//        Context context = new Context();
//        context.setVariable("username", "scy");
//        String process = templateEngine.process("/mail/demo", context);
//
//        mailClient.sentMail("996812464@qq.com", "test", process);
//        LoginTicket loginTicket = new LoginTicket();
//        loginTicket.setUserId(11);
//        loginTicket.setTicket("ss");
//        loginTicket.setStatus(0);
//        loginTicket.setExpired(new Date());
//        loginTicketMapper.insertTicket(loginTicket);
//        LoginTicket ss = loginTicketMapper.selectTicketByTicket("ss");
//        System.out.println(ss);
//        loginTicketMapper.updateTicketByStatus("ss",1);
//        LoginTicket ss1 = loginTicketMapper.selectTicketByTicket("ss");
//        System.out.println(ss1);
//        String s = sensitiveFilter.sensitiveReplace("我爱☆嫖☆娼☆，我爱☆赌☆博☆，我爱☆开☆票☆，我爱☆吸☆毒☆，哈哈");
//        System.out.println(s);

        List<Message> messages = messageMapper.selectConversationList(111, 0, 20);
        for (Message message : messages) {
            System.out.println(message);
        }

        int count = messageMapper.selectConversionCount(111);
        System.out.println(count);

        List<Message> messages1 = messageMapper.selectMessageList("111_112", 0, 20);
        for (Message message : messages1) {

            System.out.println(message);
        }
        int count1 = messageMapper.selectMessageCount("111_112");
        System.out.println(count1);
        int count2 = messageMapper.selectUnReadMessageCount(111, "111_131");
        System.out.println(count2);


    }


}
