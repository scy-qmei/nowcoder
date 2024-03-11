package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private MessageService messageService;

    /**
     * 在每次controller执行完毕，发送模版之前，查询当前用户的所有未读消息，加入模版进行显示
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //使用数据先判空
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            int totalUnreadMsg = messageService.selectUnReadMessageCount(user.getId(), null);
            int totalUnreadNotice = messageService.selectUnreadNoticeCount(user.getId(), null);
            modelAndView.addObject("allUnread", totalUnreadNotice + totalUnreadMsg);
        }
    }
}
