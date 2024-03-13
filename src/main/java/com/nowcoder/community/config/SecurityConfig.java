package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 该类是spring security的配置类，负责配置认证和授权规则
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstants {
    /**
     * 该方法的主要作用就是让security不拦截对于静态资源的请求，静态资源是没必要保护的
     * @param web
     * @throws Exception
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    /**
     * 该方法主要就是用来编写security内置的或者是我们自定义的认证规则的，这里不使用，使用我们自己之前编写好的认证规则
     * @param
     * @throws Exception
     */
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        super.configure(auth);
//    }

    /**
     * 该方法可以用来配置登录/退出的请求路径，以便进行拦截，配置拦截后成功/失败对应的处理，这里因为登录/退出使用自己的逻辑，就不配置
     * 除此之外的一个重要的功能就是配置用户的授权规则！
     * 还可以自定义filter来自定义授权规则
     * 还可以配置记住我的选项，这里也用我们自己的逻辑
     * 所以这里方法的作用就是配置授权问题
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //这里设置如下的请求必须要有用户登录权限才可以访问！
        http.authorizeRequests()
                .antMatchers(
                        "/comment/**",
                        "/discuss/add",
                        "/follow",
                        "/unfollow",
                        "/like",
                        "/message/**",
                        "/user/setting",
                        "/user/password",
                        "/user/header"
                ).hasAnyAuthority(AUTHORITY_USER,AUTHORITY_ADMIN,AUTHORITY_MODERATOR)
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                ).hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers(
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**"
                ).hasAnyAuthority(AUTHORITY_ADMIN)
                //余下的其他请求是所有权限都可以访问的！！！！！
                .anyRequest().permitAll()
                //禁用security的csrf令牌
                .and().csrf().disable();


        //除了设置权限外，还要设置没有权限，或者权限不足的情况下的处理方式
        http.exceptionHandling()
                //没有登录情况的异常处理方式！
                .authenticationEntryPoint(
                        new AuthenticationEntryPoint() {
                            @Override
                            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                                //因为这里请求方式可能有普通请求或者异步请求，所以根据不同的请求方式进行不同的响应！
                                String xRequestedWith = request.getHeader("x-requested-with");
                                if ("XMLHttpRequest".equals(xRequestedWith)) {
                                    //异步请求的方式
                                    //设置响应的类型为普通字符串，但是格式我们要设置为json，方便前端解析
                                    response.setContentType("application/plain;charset=utf-8");
                                    PrintWriter writer = response.getWriter();
                                    writer.write(CommunityUtil.getJsonString(403,"你还没有登录哦"));
                                } else {
                                    //普通请求的方式，如果没有登录就跳转到登录页面即可
                                    //注意这里请求重定向的方式！
                                    response.sendRedirect(request.getContextPath() + "/login");
                                }
                            }
                        }
                )
                //权限不够时的处理方式
                .accessDeniedHandler(
                        new AccessDeniedHandler() {
                            @Override
                            public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                                String xRequestedWith = request.getHeader("x-requested-with");
                                if ("XMLHttpRequest".equals(xRequestedWith)) {
                                    //异步请求的方式
                                    //设置响应的类型为普通字符串，但是格式我们要设置为json，方便前端解析
                                    response.setContentType("application/plain;charset=utf-8");
                                    PrintWriter writer = response.getWriter();
                                    writer.write(CommunityUtil.getJsonString(403,"你没有访问的权限哦"));
                                } else {
                                    //普通请求的方式，如果没有登录就跳转到登录页面即可
                                    //注意这里请求重定向的方式！
                                    response.sendRedirect(request.getContextPath() + "/noAuthority");
                                }
                            }
                        }
                );
        //这里因为security会自动拦截logout请求，所以为了实现我们自己的退出逻辑，需要覆盖一下其默认拦截路径
        http.logout()
                .logoutUrl("/securityLogout");

    }
}

