package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * 该控制器处理的是模拟分享业务的请求
 * 控制器接收到请求后，对接收到的url进行转换为image存储到服务器，然后相应给浏览器一个访问图片的地址
 * 浏览器接收到后访问图片的地址，再次被该controller拦截，从服务器中获取图片响应给浏览器
 *
 */
@Controller
@Slf4j
public class ShareController implements CommunityConstants {

    @Value("${wk.image.storage}")
    private String wkImageStorage;
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(value = "share",method = RequestMethod.GET)
    @ResponseBody
    public String shareImage(String htmlUrl) {
        if (StringUtils.isBlank(htmlUrl)) {
            throw new IllegalArgumentException("转换的html路径为空！");
        }

        //这里因为生成的图片是很耗时的，如果等待图片生成完毕再响应给浏览器，会让客户端等待时间太长
        //因此这里将生成图片作为一个异步的请求，控制器将事件放入队列后就往下执行响应给浏览器，增强服务器的相应速度！
        //执行生成图片命令锁需要的参数有命令所在的路径， 存储路径，存储的文件名以及后缀，这里前两者都已经在配置文件声明，所以后几个加入事件传递给消费组
        String fileName = CommunityUtil.generateRandomStr();
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl",htmlUrl)
                .setData("fileName",fileName)
                .setData("suffix",".png");
        //触发事件
        eventProducer.fireEvent(event);
        //拼接一个提供给浏览器的访问分享图片的请求链接
        String shareImageUrl = domain  + contextPath + "/share/image/" + fileName;
        Map<String, Object> map = new HashMap<>();
        map.put("shareImageUrl", shareImageUrl);
        return CommunityUtil.getJsonString(0, null, map);
    }

    @RequestMapping(value = "share/image/{fileName}", method = RequestMethod.GET)
    public void responseShareImage(@PathVariable String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名为空！");
        }
        //因为图片的固定后缀设置为png，所以直接拼接
        String imagePath = wkImageStorage + "/" + fileName + ".png";

        //读取图片，通过response给浏览器响应图片

        FileInputStream inputStream = null;

        try {
            ServletOutputStream outputStream = response.getOutputStream();
            inputStream = new FileInputStream(imagePath);
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, b);
            }
        } catch (IOException e) {
            log.error("读取图片失败，错误信息为:" + e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("输入流关闭失败，错误信息为:" + e.getMessage());
            }
        }


    }
}
