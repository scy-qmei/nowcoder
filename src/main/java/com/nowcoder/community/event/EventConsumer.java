package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 这就是事件的消费者
 */

@Slf4j
@Component
public class EventConsumer implements CommunityConstants {

    @Autowired
    private MessageService messageService;
    //因为这里点赞/评论/关注触发的事件包含的信息差不多，所以这里用一个方法来消费三个事件！
    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_FOLLOW,TOPIC_LIKE})
    public void handleEvents(ConsumerRecord record) {
        //处理业务严谨一些，使用数据先判空
        if (record == null || record.value() == null) {
            log.error("消息为空");
            return;
        }
        //注意学习解析json串到对象的方法！
        Event event = (Event) JSONObject.parseObject(record.value().toString(), Event.class);
        //判断解析到的对象是否为空,如果为空，代表消费到的消息不是一个json串，解析错误
        if (event == null) {
            log.error("消息的格式不正确");
            return;
        }
        //对象不为空，继续进行
        //这里因为事件触发业务是发送一个系统通知，所以将event对象转换为message对象存入数据库！
        Message message = new Message();
        message.setCreateTime(new Date());
        message.setFromId(SYSTEM_USER_ID);
        //这里注意消息的toId就是实体的拥有者的id，即EntityUserId
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        //对于event中多余的数据，统一存储到message的content中！
        //这里因为要根据系统通知的内容进行响应的展示，即xxx评论了你的xxx，所以需要存储实体类型以及实体id
        Map<String,Object> map = new HashMap<>();
        map.put("entityType", event.getEntityType());
        map.put("entityId", event.getEntityId());
        map.put("userId", event.getUserId());

        //如果event里的map存储的数据不为空，将这些数据也存储进来
        Map<String, Object> data = event.getData();
        if (data != null) {
            for (Map.Entry<String,Object> entry : data.entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(map));
        messageService.insertMessage(message);
    }
}
