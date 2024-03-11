package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DataService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 该拦截器是拦截所有的请求，进行UV和DAU数据的存储
 */
@Component
public class DataInterceptor implements HandlerInterceptor {
    @Autowired
    private DataService dataService;
    @Autowired
    private HostHolder hostHolder;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取当前访问的IP
        String remoteHost = request.getRemoteHost();
        //进行uv存储
        dataService.setUV(remoteHost);
        User user = hostHolder.getUser();
        //如果当前用户不为空，才进行dau的存储
        if (user != null) {
            dataService.setDAU(user.getId());
        }
        return true;
    }
}
