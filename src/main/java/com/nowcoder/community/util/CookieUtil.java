package com.nowcoder.community.util;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtil {
    /**
     * 该方法是从请求头中获取指定key（name）的cookie的value
     * @param request
     * @param name cookie的key
     * @return
     */
    public static String getValue(HttpServletRequest request, String name) {
        //首先对参数判空
        if (request == null || name == null) {
            throw new IllegalArgumentException("参数为空");
        }
        Cookie[] cookies = request.getCookies();
        //对获取的cookie数组判空
        if (cookies != null) {
            for(Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) return cookie.getValue();
            }
        }
        return null;

    }
}
