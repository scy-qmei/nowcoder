package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.annotation.CheckLogin;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * 该拦截器是为了在涉及用户权限的请求中去判断用户是否登录的拦截器，如果用户登录了，那么请求就放行，否则就不放行！
 */
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolder hostHolder;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截的请求对象是方法才继续
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            //获取请求的方法上的checklogin注解
            CheckLogin annotation = method.getAnnotation(CheckLogin.class);
            //如果注解不为空，代表是需要检查登录状态的请求
            if (annotation != null) {
                User user = hostHolder.getUser();
                //如果没有登录，就请求重定向到登录页面，进行登录，请求不进行放行
                if (user == null) {
                    response.sendRedirect(request.getContextPath() + "/login");
                    return false;
                }
            }
        }
        //否则就放行请求
        return true;
    }
}
