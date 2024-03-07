package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息的生产者，即事件生产者,发送事件到消息队列
 */
@Component
public class EventProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;
    //触发事件常用的方法的名字！
    public void fireEvent(Event event) {
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
