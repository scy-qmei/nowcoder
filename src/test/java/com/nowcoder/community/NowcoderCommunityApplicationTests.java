package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.function.Supplier;

@SpringBootTest
class NowcoderCommunityApplicationTests {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    void contextLoads() {
//        User user = userMapper.selectByName("liubei");
//        System.out.println(user);
//        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, 0, 10);
//        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, 0, 10);
//        for (DiscussPost discussPost : discussPosts) {
//            System.out.println(discussPost);
//        }
        Logger logger = LoggerFactory.getLogger(NowcoderCommunityApplicationTests.class);
        logger.error(new Supplier<String>() {
            @Override
            public String get() {
                return "sdfdsfs";
            }
        });
    }


}
