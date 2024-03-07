package com.nowcoder.community.service;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.mapper.UserMapper;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
//    @Autowired
//    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String proPath;

    //用redis进行重构,这里因为用户业务对外只提供了通过id查询用户数据的方法，所以对该方法进行重构即可
    //这样所有需要用到用户数据的请求都会先去查询缓存中是否有对应的数据！
    public User getUserById(int userId) {
//        return userMapper.selectById(userId);
        User user = getCache(userId);
        if (user == null) {
            user = userMapper.selectById(userId);
            addCache(user);
        }
        return user;
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
            //涉及缓存数据的修改，需要清除缓存
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATE_SUCCESS;
        } else return ACTIVATE_FAILURE;
    }

    /**
     * 该业务用来处理用户的登录，对输入的登录数据进行校验，校验成功则生成登录凭证，否则返回错误信息
     * @param username
     * @param password
     * @param expiredTime 这里的过期时间设置为long类型以防止后续的乘1000ms溢出int的最大值
     * @return
     */
    public Map<String,String> login(String username, String password, Long expiredTime) {
        Map<String,String> map = new HashMap<>();
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg","用户名为空");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg","密码为空");
            return map;
        }
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "您输入的用户名有误");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "用户未激活，请激活后再登录");
            return map;
        }
        //这里注意数据库中的密码是加密的，所以对密码加密后再做判断！
        password = CommunityUtil.md5(password + user.getSalt());
        if (!password.equals(user.getPassword())) {
            map.put("passwordMsg", "您输入的密码有误");
            return map;
        }
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateRandomStr());
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredTime * 1000));
        loginTicket.setStatus(0);
//        loginTicketMapper.insertTicket(loginTicket);
        //这里利用redis对方法进行重构，以提升项目的整体性能
        String ticketKey = RedisKeyUtil.getTicket(loginTicket.getTicket());
        //redistemplate的value的序列化方式，我们在配置类中已经指明了使用json的序列化方式
        //因此这里会将登录凭证序列化为json串存入redis
        redisTemplate.opsForValue().set(ticketKey, loginTicket);

        //这里往map中添加ticket是为了响应给客户端让其存储登录凭证以便后续的交互
        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    /**
     * 该业务用来更新登录凭证的状态为失效状态，以完成用户的退出登录
     * redis重构之后，逻辑就是根据ticket从redis中取出，然后更改之后再存入redis
     * @param ticket
     */
    public void logout(String ticket) {
//        loginTicketMapper.updateTicketByStatus(ticket,1);
        String ticket1 = RedisKeyUtil.getTicket(ticket);
        //这里因为存入redis是序列化为json串了，从redis中获取就直接可以转换为对应的对象了！！
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticket1);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticket1, loginTicket);
    }

    public LoginTicket selectLoginTicketByTicket(String ticket) {
        String ticket1 = RedisKeyUtil.getTicket(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticket1);
    }

    public void updateUserHeaderUrl(int userId, String headerUrl) {
        userMapper.updateHeader(userId,headerUrl);
        clearCache(userId);
    }

    public void updateUserPassword(User user) {
        String s = CommunityUtil.md5(user.getPassword() + user.getSalt());
        userMapper.updatePassword(user.getId(), s);
        clearCache(user.getId());
    }
    public User getUserByName(String username) {
        return userMapper.selectByName(username);
    }
    /**
     * 使用redis来缓存用户信息，从而避免频繁的访问mysql产生的性能消耗
     * 使用缓存有三大步
     * 使用数据先找缓存
     * 找不到缓存，从mysql找放入缓存
     * 数据更新，清除缓存
     */
    //1 使用用户数据先从缓存找
    private User getCache(int userId) {
        String userKey = RedisKeyUtil.getUser(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }
    //2 如果缓存里没有，就加入缓存
    public void addCache(User user) {
        String userKey = RedisKeyUtil.getUser(user.getId());
        redisTemplate.opsForValue().set(userKey, user);
    }
    //3 如果数据发生了更新，清除缓存
    public void clearCache(int userId) {
        String user = RedisKeyUtil.getUser(userId);
        //清除缓存，直接删除数据的key即可！
        redisTemplate.delete(user);
    }
}
