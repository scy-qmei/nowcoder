package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 这是一个threadlocal工具类，负责在多线程并发场景下完成对User的存取
 */
@Component
public class HostHolder {
    private ThreadLocal<User> threadLocal = new ThreadLocal<>();

    public void setUser(User user) {
        threadLocal.set(user);
    }
    public User getUser() {
        return threadLocal.get();
    }
    public void clear() {
        threadLocal.remove();
    }
}
