package com.nowcoder.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Random;
import java.util.UUID;

/**
 * 该类是自己开发的该项目的工具类
 */
public class CommunityUtil {
    /**
     * 该方法就是用来生成一个随机字符串
     * @return
     */
    public static String generateRandomStr() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 该方法就是对输入的字符串进行md5加密，通过commons lang提供的工具类对输入的字符串判断非空 非空格 非null 才可以进行加密
     * @param text
     * @return
     */
    public static String md5(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(text.getBytes());
    }
}
