package com.nowcoder.community.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * 这种涉及初始化的操作很适合写在配置类里，因为项目启动会先加载配置类进行实例化，这里我们在该配置类实例化之后进行服务器存储路径的创建！
 */
@Configuration
@Slf4j
public class WKConfig {
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    //设置初始化方法，在配置类被实例化完成后就判断图片存储路径是否存在
    @PostConstruct
    public void init() {
        File file = new File(wkImageStorage);
        if (!file.exists()) {
            boolean mkdir = file.mkdir();
            if(mkdir) {
                log.info("分享功能的存储路径已经创建完毕，路径为:"+ wkImageStorage);
            }else {
                log.error("分享功能的存储路径已经创建失败");
            }
        }
    }
}
