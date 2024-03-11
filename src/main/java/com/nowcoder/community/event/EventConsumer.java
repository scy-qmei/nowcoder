package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticSearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 这就是事件的消费者
 */

@Slf4j
@Component
public class EventConsumer implements CommunityConstants {
    @Value("${wk.image.command}")
    private String wkImageCommand;
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ElasticSearchService elasticSearchService;
    @Autowired
    private DiscussPostService discussPostService;
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
            //如果事件对象中有帖子拥有者的id，表明该事件是回复某用户的回复触发的，那么除了给回复的用户一个通知外，还要给帖子的拥有者一个通知
            Integer discussPostOwnerId = (Integer) data.get("discussPostOwner");
            if (discussPostOwnerId != null) {
                Message message1 = new Message();
                message1.setFromId(SYSTEM_USER_ID);
                message1.setToId(discussPostOwnerId);
                message1.setCreateTime(new Date());
                message1.setConversationId(event.getTopic());
                Map<String, Object> postMap = new HashMap<>();
                postMap.put("userId", event.getUserId());
                postMap.put("entityType", COMMENT_TYPE_POST);
                postMap.put("entitiyId",data.get("postId"));
                for(Map.Entry<String,Object> entry : data.entrySet()) {
                    postMap.put(entry.getKey(), entry.getValue());
                }
                message1.setContent(JSONObject.toJSONString(postMap));
                messageService.insertMessage(message1);
            }
        }
        message.setContent(JSONObject.toJSONString(map));
        messageService.insertMessage(message);
    }

    /**
     * 该方法是消费因添加帖子/添加评论导致的需要向es中更新帖子数据而产生的事件
     * @param record
     */
    @KafkaListener(topics = ES_DISCUSSPOST_UPDATE)
    public void handlePostEvents(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息的格式不正确");
            return;
        }
        //如果对象正确的转换，就获取事件中存储的帖子id属性，根据此id查找最新的帖子数据进行更新
        //因为在封装该事件对象时，实体就是帖子，当然实体id就是对应的帖子了！
        Integer postId = event.getEntityId();
        DiscussPost discussPost = discussPostService.selectDiscussPostById(postId);
        elasticSearchService.insertDiscussPost(discussPost);
    }

    /**
     * 该方法是消费由于删除帖子触发的事件，同步到es
     * @param record
     */
    @KafkaListener(topics = ES_DISCUSSPOST_DELETE)
    public void handlerDetelePost(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息不能为空");
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息的格式不正确");
        }
        elasticSearchService.deleteDiscussPost(event.getEntityId());
    }

    /**
     * 消费分享图片触发的事件
     * @param record
     */
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessages(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            throw new IllegalArgumentException("消息为空");
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            throw new IllegalArgumentException("消息的格式不正确");
        }
        String htmlUrl = (String)event.getData().get("htmlUrl");
        String fileName = (String)event.getData().get("fileName");
        String suffix = (String)event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " "
                 + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            log.info("html成功转换为img，执行命令为：" + cmd);
        } catch (IOException e) {
            log.error("html转换为img失败,错误原因为:" + e.getMessage());
        }
    }

}
