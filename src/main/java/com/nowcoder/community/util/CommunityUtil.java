package com.nowcoder.community.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Map;
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

    /**
     * 该方法根据响应状态码，响应信息，以及响应数据返回给浏览器一个json字符串，利用了fastjson工具包
     * @param code
     * @param msg
     * @param data
     * @return
     */
    public static String  getJsonString(int code, String msg, Map<String, Object> data) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("msg", msg);
        if (data != null) {
            for(Map.Entry<String,Object> entry : data.entrySet()) {
                jsonObject.put(entry.getKey(),entry.getValue());
            }
        }

        return jsonObject.toJSONString();
    }
    public static String getJsonString(int code, String msg) {
        return getJsonString(code,msg,null);
    }
    public static String getJsonString(int code) {
        return getJsonString(code,null,null);
    }
}
