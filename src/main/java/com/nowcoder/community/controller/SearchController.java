package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.ElasticSearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstants {
    @Autowired
    private ElasticSearchService elasticSearchService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "search",method = RequestMethod.GET)
    public String searchByKeyword(String keyword, Model model, Page page) {
        //根据关键字查询帖子分页数据以及符合条件的帖子的总量
        Map<String,Object> map = elasticSearchService.searchByKeyword(keyword, page.getCurrent() - 1, page.getLimit());
        List<DiscussPost> discussPosts = (List<DiscussPost>) map.get("discussPosts");
        long totalCount = (Long) map.get("count");
        //封装数据，发送给前段进行显示
        List<Map<String,Object>> searchVo = new ArrayList<>();
        if (!discussPosts.isEmpty()) {
            for (DiscussPost discussPost : discussPosts) {
                Map<String,Object> discussPostVo = new HashMap<>();
                discussPostVo.put("post", discussPost);
                //获取帖子的用户信息
                User userById = userService.getUserById(discussPost.getUserId());
                discussPostVo.put("user", userById);
                //获取帖子的点赞信息
                Long likeCount = likeService.getLikeCount(COMMENT_TYPE_POST, discussPost.getId());
                discussPostVo.put("likeCount", likeCount);

                searchVo.add(discussPostVo);
            }
        }
        model.addAttribute("searchs", searchVo);
        model.addAttribute("keyword",keyword);

        //设置分页信息，注意这里关键词是通过请求路径携带的，所以要拼接完整的页面访问路径！！
        page.setPath("/search?keyword=" + keyword);
        page.setRows((int) totalCount);
        return "/site/search";
    }
}
