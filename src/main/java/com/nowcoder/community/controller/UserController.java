package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.CheckLogin;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Controller
@RequestMapping("user")
@Slf4j
public class UserController {
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
    @CheckLogin
    @RequestMapping(value = "setting",method = RequestMethod.GET)
    public String jumpToSettingPage() {
        return "/site/setting";
    }

    /**
     * 该方法就是通过springmvc的multipartfile来获取用户上传的头像，将上传的头像保存在服务器本地
     * @param headImage
     * @param model
     * @return
     */
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
     * 该方法就是当用户成功上传图片到服务器时，需要从服务器获取该图片进行头像地的更新
     * 注意上传成功后，头像图片由对应的web路径了，所以该方法的请求路径应该与其匹配
     * 注意因为该方法响应给客户端的是一个图片，所以需要手动的用response进行处理，不需要返回字符串进行页面跳转了！
     * @param fileName 头像图片的web链接中的最后一项的文件名称，通过路径变量获取
     * @param response 响应给客户端一个图片
     *
     */
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
}
