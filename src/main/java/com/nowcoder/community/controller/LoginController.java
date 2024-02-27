package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;


@Controller
@Slf4j
public class LoginController implements CommunityConstants {
    @Autowired
    private UserService userService;
    @RequestMapping(value = "register",method = RequestMethod.GET)
    public String jumpToRegistPage() {
        return "site/register";
    }

    @RequestMapping(value = "register", method = RequestMethod.POST)
    //注意这里springmvc会自动的将前端送过来的参数对user进行赋值，然后将user加入model
    public String register(Model model, User user) {
        Map<String, String> register = userService.register(user);
        //根据注册的结果跳转不同的页面
        if (register.isEmpty()) {
            model.addAttribute("msg", "您已经注册成功，我们给您的邮箱发送了一封激活邮件，请点击进行激活");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("userErrorMsg", register.get("userErrorMsg"));
            model.addAttribute("passwordErrorMsg", register.get("passwordErrorMsg"));
            model.addAttribute("emailErrorMsg", register.get("emailErrorMsg"));
            return "/site/register";
        }
    }

    /**
     * 该handler调用激活业务完成用户账号的激活，注意这里的请求路径要和注册用户时发送的激活路径一致
     * @param model 存储视图变量
     * @param userId 用户的id
     * @param code 用户的激活码
     * @return
     */
    @RequestMapping(value = "activation/{userId}/{code}", method = RequestMethod.GET)
    public String activate(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int activate = userService.activate(userId, code);
        if (activate == ACTIVATE_SUCCESS) {
            model.addAttribute("msg","您已经激活成功，即将跳转到登录页面");
            model.addAttribute("target","/login");
        } else if (activate == ACTIVATE_REPEATE) {
            model.addAttribute("msg","您已经激活过了，请勿重复激活");
            model.addAttribute("target","/index");
        } else {
            model.addAttribute("msg","抱歉，您激活失败");
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(value = "login",method = RequestMethod.GET)
    public String jumpToLoginPage() {
        return "/site/login";
    }
}
