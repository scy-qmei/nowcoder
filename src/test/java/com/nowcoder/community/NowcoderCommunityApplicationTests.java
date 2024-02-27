package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.mapper.UserMapper;
import com.nowcoder.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.LinkedList;
import java.util.List;
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
        Context context = new Context();
        context.setVariable("username", "scy");
        String process = templateEngine.process("/mail/demo", context);

        mailClient.sentMail("996812464@qq.com", "test", process);


    }


}
