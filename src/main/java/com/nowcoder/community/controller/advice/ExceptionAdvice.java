package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 该类就是一个Controller的全局配置类，用来统一处理项目中Controller层所出现的异常
 * 这里的annotations = Controller.class 表明只对Controller注解的类生效
 */
@ControllerAdvice(annotations = Controller.class)
@Slf4j
public class ExceptionAdvice {
    /**
     * 这里方法名无所谓，但必须是public void 参数也可以有很多，是自定义的
     * 注解指明的是处理异常的类型，这里写的是所有异常的类型
     * @param e 异常
     * @param response
     * @param request
     */
    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response, HttpServletRequest request) throws IOException {
        log.error("服务器出错了，出错信息为：" + e.getMessage());
        //获取更加详细的异常信息，就遍历异常记录的栈,其中每一个element就代表一条异常信息
        for(StackTraceElement element : e.getStackTrace()) {
            log.error(element.toString());
        }
        //根据请求的不同来返回不同的数据,普通请求返回网页，异步请求返回json串
        String xRequestedWith = request.getHeader("x-requested-with");
        //这里如果获取的数据是XMLHttpRequest，就代表是异步请求，就返回json串
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            //这里plain指的是返回的是普通字符串，但如果返回的是json形式的字符串，在前端就可以手动将其解析为json串
            response.setContentType("application/plain;charset=utf-8");
            //注意手动给浏览器返回提示信息的方法，获取response的输出流，进行输出！
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJsonString(1,"服务器出错啦"));
        } else {
            //如果是普通请求，就重定向到错误页面！
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
