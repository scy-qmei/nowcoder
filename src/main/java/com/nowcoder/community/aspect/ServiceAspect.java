package com.nowcoder.community.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 这是针对于Service层的AOP切面，用来给service层插入各种系统需求
 * 注意切面类也要放入IOC容器
 */
@Aspect
@Component
@Slf4j
public class ServiceAspect {
    //pointcut方法就是指明切点表达式，以便复用
    //这里切点表达式的意思是 任意返回值 包名下的任意类的任意参数方法 都是切点
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut() {

    }
    //该方法就是调用我们编写的切点表达式，对响应的切点进行前置通知
    //前置/后置/返回/异常通知可以传入的切点类型为JoinPoint，环绕通知传入的是ProceedJoinPoint
    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        //日志的格式规定如下
        //用户[ip]在[时间]访问了[xx.xx.xx.xx 即访问的方法的全类名]
        //获取这些首要要获取request对象
        //通过此获取request对象
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        //如果该调用不是因为请求发起的，这里简洁处理一下，直接不打印日志
        if (requestAttributes == null) {
            return;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        //获取用户ip
        String ip = request.getRemoteHost();
        //当前时间
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        //利用切点获取方法的全类名，因为切点就是织入的方法对象，所以要获取访问的方法对象，可以利用切点获取
        String declaringTypeName = joinPoint.getSignature().getDeclaringTypeName();
        String name = joinPoint.getSignature().getName();
        String target = declaringTypeName + "." + name;

        log.info(String.format("用户[%s]在[%s]访问了[%s]", ip, now, target));

    }
}
