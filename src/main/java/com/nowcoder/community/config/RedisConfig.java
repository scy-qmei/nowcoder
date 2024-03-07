package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.Serializable;

/**
 * redistemplate配置类，该类用来自定义redistemplate，将其key的类型设置为String而不是默认实现的Object
 */
@Configuration
public class RedisConfig {
    //注意方法的参数，如果在IOC容器中，那么容器会自动的进行依赖注入！
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //给redisTemplate设置连接工厂，这样才能建立java和redis的连接！
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        //设置redis的序列化方式，因为这里是java程序，将数据存储到redis数据库上需要进行数据格式转换！
        //普通的key，因为我们规定了使用string，所以设置字符串的序列化方式即可
        redisTemplate.setKeySerializer(RedisSerializer.string());
        //普通的value，因为value可能是各种数据类型，所以这里将其转为json格式，为了提高可读性
        redisTemplate.setValueSerializer(RedisSerializer.json());
        //哈希表的key和value 与上面同理
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());

        //使得上述的配置生效
        redisTemplate.afterPropertiesSet();
        //这里因为实例化的redistemplate的泛型的key是String，所以就设置生效了！
        return redisTemplate;
    }
}
