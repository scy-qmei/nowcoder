package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CookieUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * 这是一个拦截器，用来对该项目的所有请求进行拦截，以对用户登录/未登录的情况做导航栏的不同显示处理
 * 牢记拦截器编写完毕一定要通过配置类来添加配置拦截器，否则是不生效的！
 */
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;

    /**
     * 在请求被处理之前，拦截器需要根据请求携带的登录凭证来获取用户数据，存储用户数据方便后续的使用
     * 注意这里在高并发场景下，拦截器可能同一时间段拦截到多个请求，因此这时用户数据的存储就涉及到多线程场景了，所以这里用threadlocal代替session来存储用户
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取请求携带的登录凭证
        String ticket = CookieUtil.getValue(request, "ticket");
        //如果为空就不往下执行
        if (ticket != null) {
            //根据登录凭证获取对应的登录凭证对象
            LoginTicket loginTicket = userService.selectLoginTicketByTicket(ticket);
            //如果登录凭证不为空且状态是可用，并且没有到达过期时间，这里的a.after(b)就是判断a在b之前
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                User userById = userService.getUserById(loginTicket.getUserId());
                //在threadlocal中存储用户数据
                hostHolder.setUser(userById);
            }
        }
        //注意这里一定返回true，如果返回false请求就不会被执行了，只有为true请求才会被放行
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        //因为要使用user和model，所以先判空再使用，养成好习惯
        if (user != null && modelAndView != null ) {
            //user放到model方便前端动态渲染
            modelAndView.addObject("loginUser", user);
        }

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
    }
}
