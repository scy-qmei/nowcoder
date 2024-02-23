package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.mapper.UserMapper;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private UserMapper userMapper;

    @RequestMapping(value = "index", method = RequestMethod.GET)

    public String getIndexPage(Model model, Page page) {
        page.setRows(discussPostMapper.getDiscussPostRows(0));
        page.setPath("index");
        page.setLimit(10);
        int offset = page.getOffset();

        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, offset, page.getLimit());
        List<Map<String, Object>> dps = new ArrayList<>();
        if (discussPosts != null) {
            for (DiscussPost discussPost : discussPosts) {
                Map<String, Object> map = new HashMap<>();
                User user = userMapper.selectById(discussPost.getUserId());
                map.put("post", discussPost);
                map.put("user", user);
                dps.add(map);
            }
        }
        model.addAttribute("discussPosts", dps);
        return "/index";

    }
}
