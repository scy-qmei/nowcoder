package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.CheckLogin;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("user")
@Slf4j
public class UserController implements CommunityConstants {
    @Autowired
    private HostHolder hostHolder;
    @Value("${server.servlet.context-path}")
    private String proPath;
    @Value("${community.path.domain}")
    private String proDomain;
    @Value("${community.path.load}")
    private String loadPath;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FollowService followService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private CommentService commentService;

    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.header.name}")
    private String headerName;
    @Value("${qiniu.bucket.header.url}")
    private String headerUrl;

    /**
     * 在服务端将文件直接上传给云服务器，这里当浏览器跳转到setting页面，就生成一些访问七牛云需要的数据
     * @return
     */
    @CheckLogin
    @RequestMapping(value = "setting",method = RequestMethod.GET)
    public String jumpToSettingPage(Model model) {
        //生成随机文件名
        String fileName = CommunityUtil.generateRandomStr();
        //告诉七牛云期望的返回信息以及信息类型
        StringMap policy = new StringMap();
        policy.put("returnBody",CommunityUtil.getJsonString(0));
        //生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        //生成凭证的几个参数，存储空间名，文件名，token的有效期单位是s，期望的响应信息及其类型
        String uploadToken = auth.uploadToken(headerName, fileName, 3600, policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName", fileName);
        return "/site/setting";
    }

    /**
     * 注意该方法返回的是json字符串，应对的是异步请求，所以一定加上responsebody注解！！
     *
     * @param fileName
     * @return
     */
    @RequestMapping(value = "header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJsonString(1,"文件名不能为空");
        }
        String newUrl = headerUrl + "/" + fileName;
        userService.updateUserHeaderUrl(hostHolder.getUser().getId(), newUrl);
        return CommunityUtil.getJsonString(0);
    }

    /**
     * 该方法就是通过springmvc的multipartfile来获取用户上传的头像，将上传的头像保存在服务器本地
     * @param headImage
     * @param model
     * @return
     *
     * 最新情况，已经弃用，开发了上传到云服务器的功能
     */
    @Deprecated
    @RequestMapping(value = "header",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headImage, Model model) {
        //首先对用户上传的图片判空
        if (headImage == null) {
            model.addAttribute("error","您还没有上传头像");
            return "/site/setting";
        }
        //如果图片不为空，就判断其后缀名是否满足图片格式
        String filename = headImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf('.'));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error","图片格式不正确");
            return "/site/setting";
        }
        //如果都满足，则给图片重命名一个随机的名字，防止用户上传重名图片导致覆盖
        filename = CommunityUtil.generateRandomStr() + "." + suffix;
        File file = new File(loadPath + filename);
        try {
            headImage.transferTo(file);
        } catch (IOException e) {
            log.error("上传图片失败，服务器出错了，错误信息为：" + e.getMessage());
        }
        //头像图片上传成功，就更新数据库中的用户的头像访问链接
        User user = hostHolder.getUser();
        //注意这里对于用户来说访问链接应该是web路径而不是磁盘路径，所以自定义一个
        String headerUrl = proDomain + proPath + "/user/header/" + filename;
        userService.updateUserHeaderUrl(user.getId(), headerUrl);
        return "redirect:/index";
    }

    /**
     * 该方法就是当用户成功上传图片到本地服务器时，需要从服务器获取该图片进行头像地的更新
     * 注意上传成功后，头像图片由对应的web路径了，所以该方法的请求路径应该与其匹配
     * 注意因为该方法响应给客户端的是一个图片，所以需要手动的用response进行处理，不需要返回字符串进行页面跳转了！
     * @param fileName 头像图片的web链接中的最后一项的文件名称，通过路径变量获取
     * @param response 响应给客户端一个图片
     *
     * 最新情况，已经弃用，开发了上传到云服务器的功能
     *
     */
    @Deprecated
    @RequestMapping(value = "header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable String fileName, HttpServletResponse response) {
        String suffix = fileName.substring(fileName.lastIndexOf('.'));
        response.setContentType("image" + suffix);
        String filePath = loadPath + fileName;
        //注意这里是要从服务器读取头像图片然后响应给客户端，所以应该是输入字节流
        FileInputStream fileInputStream = null;
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            fileInputStream = new FileInputStream(filePath);
            //注意字节流读取文件的写法
            byte[] b = new byte[1024];
            int index = 0;
            while( (index = fileInputStream.read(b)) != -1) {
                outputStream.write(b, 0, index);
            }
        } catch (IOException e) {
            log.error("读取头像失败，错误信息为：" + e.getMessage());
        } finally{
            try {
                fileInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 该方法接受修改密码表单提交过来的旧密码，新密码以及确认密码，对齐进行校验后进行密码的更新
     * @param oldPassword
     * @param newPassword
     * @param rePassword
     * @param model
     * @return
     */
    @RequestMapping(value = "password",method = RequestMethod.POST)
    public String changePassword(String oldPassword, String newPassword, String rePassword, Model model) {
        if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword) || StringUtils.isBlank(rePassword)) {
            model.addAttribute("passError", "密码为空，请重新输入");
            return "/site/setting";
        }
//        if (oldPassword.length() < 8 || newPassword.length() < 8) {
//            model.addAttribute("error", "密码的长度不能小于8位！");
//        }
        if (!newPassword.equals(rePassword)) {
            model.addAttribute("rePassError", "两次输入的密码不一致");
            return "/site/setting";
        }
        User user = hostHolder.getUser();
        user.setPassword(newPassword);
        userService.updateUserPassword(user);

        return "redirect:/logout";
    }
    @RequestMapping(value = "profile/{userId}",method = RequestMethod.GET)
    public String jumpToProfilePage(@PathVariable("userId") int userId,Model model) {
        User user = hostHolder.getUser();
        //查询个人主页的用户信息与其点赞信息
        User targetUser = userService.getUserById(userId);
        //对数据进行合法判断后再操作！
        if (targetUser == null) {
           throw new IllegalArgumentException("用户为空");
        }
        int userLikeCount = likeService.getUserLikeCount(targetUser.getId());
        model.addAttribute("likeCount",userLikeCount);
        model.addAttribute("target",targetUser);
        //是否可以关注该用户
        boolean hasFollowed = false;
        if (user != null) {
            hasFollowed = followService.getFollowStatus(user.getId(), ENTITY_TYPE_USER, targetUser.getId());
        }
        model.addAttribute("hasFollowed", hasFollowed);

        //获取主页用户的关注数量
        long followeeCount = followService.getFolloweeCount(targetUser.getId(), ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //获取主页用户的被关注数量
        long followerCount = followService.getFollowerCount(ENTITY_TYPE_USER, targetUser.getId());
        model.addAttribute("followerCount",followerCount);
        model.addAttribute("user",user);
        return "/site/profile";
    }

    @RequestMapping(value = "post",method = RequestMethod.GET)
    public String getUserPostList(Model model, Page page) {
        User user = hostHolder.getUser();
        int discussPostRows = discussPostService.getDiscussPostRows(user.getId());

        page.setLimit(5);
        page.setPath("/user/post");
        page.setRows(discussPostRows);

        List<DiscussPost> discussPosts = discussPostService.selectDiscussPosts(user.getId(), page.getOffset(), page.getLimit(), 0);
        List<Map<String,Object>> postVoList = new ArrayList<>();
        for (DiscussPost discussPost : discussPosts) {
            Map<String,Object> postVo = new HashMap<>();
            postVo.put("post", discussPost);

            Long likeCount = likeService.getLikeCount(COMMENT_TYPE_POST, discussPost.getId());
            postVo.put("likeCount", likeCount);
            postVoList.add(postVo);
        }
        model.addAttribute("posts", postVoList);
        model.addAttribute("count", discussPostRows);
        model.addAttribute("user", user);

        return "/site/my-post";
    }
    @RequestMapping(value = "reply",method = RequestMethod.GET)
    public String getUserReplyList(Model model, Page page) {
        User user = hostHolder.getUser();
        int count = commentService.selectCommentCountByUser(user.getId());

        page.setRows(count);
        page.setLimit(10);
        page.setPath("/user/reply");

        List<Comment> comments = commentService.selectCommentByUser(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>>  commentVoList = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String,Object> commentVo = new HashMap<>();
            commentVo.put("comment",comment);

            DiscussPost discussPost = discussPostService.selectDiscussPostById(comment.getEntityId());
            commentVo.put("post", discussPost);
            commentVoList.add(commentVo);
        }

        model.addAttribute("user", user);
        model.addAttribute("comments", commentVoList);
        model.addAttribute("count", count);

        return "/site/my-reply";

    }
}
