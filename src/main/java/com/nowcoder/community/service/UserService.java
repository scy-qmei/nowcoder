package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.UserMapper;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstants{
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String proPath;

    public User getUserById(int userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 根据前段表单的信息完成对用户的注册，并返回结果信息
     * @param user
     * @return map为空代表无异常，不为空代表出现了错误
     */
    public Map<String, String> register(User user) {
        Map<String, String> res = new HashMap();
        if (user == null) {
            return null;
        }
        //首先先对表单传过来的数据判断是否非空
        if (StringUtils.isBlank(user.getUsername())) {
            res.put("userErrorMsg", "用户名不能为空，请重新输入");
            return res;
        }

        String password = user.getPassword();
        if (StringUtils.isBlank(password)) {
            res.put("passwordErrorMsg", "密码不能为空，请重新输入");
            return res;
        }

        if (StringUtils.isBlank(user.getEmail())) {
            res.put("emailErrorMsg", "邮箱不能为空，请重新输入");
            return res;
        }
        //然后再判断用户/邮箱是否已经被注册
        User user1 = userMapper.selectByName(user.getUsername());
        if (user1 != null) {
            res.put("userErrorMsg", "用户名已被注册，请重新输入");
            return res;
        }

        User user2 = userMapper.selectByEmail(user.getEmail());
        if(user2 != null) {
            res.put("emailErrorMsg", "该邮箱已被注册，请重新输入");
            return res;
        }

        //满足条件，即可进行用户的注册
        String salt = CommunityUtil.generateRandomStr().substring(0, 5);
        user.setSalt(salt);
        user.setPassword(CommunityUtil.md5(user.getPassword() + salt));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateRandomStr());
        user.setCreateTime(new Date());
        //使用表达式字符串的方式来随机的选取初始头像
        user.setHeaderUrl(String.format("https://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        userMapper.insertUser(user);

        //注册成功，给用户发送激活账号的邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        String url = domain + proPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sentMail(user.getEmail(), "牛客网注册激活邮件", content);

        //如果res为空，代表上述整个过程没有出现错误！
        return res;
    }

    /**
     * 该方法的用途就是用户注册成功后收到激活邮件，进行账号的激活功能，用户点击激活链接，服务器根据链接的用户id和激活码来进行用户的激活
     * @param userId 用户的id
     * @param code 用户的激活码
     * @return 返回的是激活状态
     */
    public int activate(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user == null) return ACTIVATE_FAILURE;
        if (user.getStatus() == 1) {
           return ACTIVATE_REPEATE;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            return ACTIVATE_SUCCESS;
        } else return ACTIVATE_FAILURE;
    }
}
