package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.DiscussPostMapper;
import com.nowcoder.community.mapper.UserMapper;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.HostHolder;
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
public class HomeController implements CommunityConstants {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LikeService likeService;
    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(value = "index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,
                               @RequestParam(value = "orderMode",defaultValue = "0") int orderMode) {


        page.setRows(discussPostMapper.getDiscussPostRows(0));
        page.setPath("index");
        page.setLimit(10);
        int offset = page.getOffset();

        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, offset, page.getLimit(), orderMode);
        List<Map<String, Object>> dps = new ArrayList<>();
        if (discussPosts != null) {
            for (DiscussPost discussPost : discussPosts) {
                Map<String, Object> map = new HashMap<>();
                User user = userMapper.selectById(discussPost.getUserId());
                map.put("post", discussPost);
                map.put("user", user);
                //获取当前帖子的点赞数量
                Long likeCount = likeService.getLikeCount(COMMENT_TYPE_POST, discussPost.getId());
                //获取当前帖子的点赞状态
                User user1 = hostHolder.getUser();
                int likeStatus = 0;
                if (user1 != null) {
                    likeStatus = likeService.getLikeStatus(user1.getId(), COMMENT_TYPE_POST, discussPost.getId());
                }
                map.put("likeCount",likeCount);
                map.put("likeStatus",likeStatus);
                dps.add(map);
            }
        }
        model.addAttribute("discussPosts", dps);
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }

    /**
     * 该方法用来实现服务器出错的页面跳转
     * @return
     */
    @RequestMapping(value = "error",method = RequestMethod.GET)
    public String jumpTo500Page() {
        return "/error/500";
    }

    /**
     * 该方法在普通请求访问时，如果权限不足，就直接跳转到404页面！
     * @return
     */
    @RequestMapping(value = "noAuthority",method = RequestMethod.GET)
    public String jumpTONoAuthorityPage() {
        return "/error/404";
    }
}
