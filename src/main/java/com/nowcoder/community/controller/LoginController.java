package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;


@Controller
@Slf4j
public class LoginController implements CommunityConstants {
    @Autowired
    private UserService userService;
    @Autowired
    private Producer kaptchaProducer;
    @Value("server.servlet.context-path")
    private String proPath;
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

    /**
     * 该方法就是接受浏览器的生成验证码请求，生成一个随机的验证码图片并返回给客户端
     * 该方法由于不用进行页面的跳转，只用响应给浏览器一个图片，所以方法就不需要返回值
     * @param session session用来存储生成的验证码，以便后续客户登录后进行验证码的验证，因为这是一个跨请求（先生成验证码，再登录）
     *                的连续的交互操作，且验证码是敏感内容，所以用到session
     * @param response 这里就是用来设置响应数据，以响应给客户端生成的验证码图片
     *
     */
    @RequestMapping(value = "kaptcha",method = RequestMethod.GET)
    public void kaptchaProducer(HttpSession session, HttpServletResponse response) {
        //调用IOC容器中的验证码生成类生成验证码图片
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        //session中存储生成的验证码的内容，为了后续用户登录时做验证码的比对！
        session.setAttribute("kaptcha", text);

        //设置响应的类型是png格式的图片
        response.setContentType("image/png");
        try {
            //这里因为是要传输图片，所以选取字节流是更好的
            ServletOutputStream outputStream = response.getOutputStream();
            //牢记图片发送的使用类是ImageIO
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            log.error("生成验证码失败，错误原因是" + e.getMessage());
        }
    }

    /**
     * 该方法用来调用用户登录业务进行用户的登录
     * @param username 输入用户名
     * @param password 输入用户密码
     * @param rememberMe 是否记住该用户
     * @param code 输入的验证码
     * @param session 从session中取出验证码比较
     * @param response 响应给用户ticket
     * @return
     */
    @RequestMapping(value = "login",method = RequestMethod.POST)
    public String login(String username, String password, boolean rememberMe,String code,
                        HttpSession session, HttpServletResponse response, Model model) {
        String kaptcha = session.getAttribute("kaptcha").toString();
        //首先判断验证码是否正确
        if (StringUtils.isBlank(code) || StringUtils.isBlank(kaptcha) || !StringUtils.equalsAnyIgnoreCase(code, kaptcha)) {
            model.addAttribute("codeMsg", "验证码有误，请重新输入");
            return "/site/login";
        }
        //如果正确，再调用登录校验业务
        int expriedTime = rememberMe ? REMEMBER_USER : NOT_REMEMBER_USER;
        Map<String, String> login = userService.login(username, password, expriedTime);
        //如果登录成功，就将ticket设置为cookie响应给客户端，让其保存，并重定向到社区的首页
        if (login.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", login.get("ticket"));
            //这里把项目名设置为变量，方便之后的修改
            cookie.setPath(proPath);
            cookie.setMaxAge(expriedTime);
            response.addCookie(cookie);
            //注意这里直接返回/index是找不到的，所以响应给浏览器让其重新请求/index，匹配对应的handler进行页面跳转
            return "redirect:/index";
        } else {
            //这里如果登录失败，就保留在当前页面，因为要显示错误信息，所以将信息加入model进行动态渲染
            //注意如果没有用户名错误，那么就为null，前端逻辑中对于null值就不会显示
            model.addAttribute("usernameMsg", login.get("usernameMsg"));
            model.addAttribute("passwordMsg", login.get("passwordMsg"));
            return "/site/login";
        }
    }

    /**
     *
     * @param ticket 牢记获取cookie的方法！！！！
     * @return
     */
    @RequestMapping(value = "logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/login";
    }
}
