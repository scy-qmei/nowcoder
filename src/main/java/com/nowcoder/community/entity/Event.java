package com.nowcoder.community.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 该类是一个实体类，用来当做生产者消费者模型中的消息
 * 封装的就是发送/消费消息所需要的数据
 * 发送消息就等同于触发事件
 * 牢记这里的事件就相当于消息！！！！！！！！
 */
public class Event {
    //事件的主题,即消息的主题
    private String topic;
    //触发事件的用户的id
    private int userId;
    //触发事件涉及的实体的类型
    private int entityType;
    //触发事件涉及的实体的id
    private int entityId;
    //如果实体是评论/帖子，需要其发布者的id
    private int entityUserId;
    //暂时考虑到上述那么多数据，如果之后复杂的业务，为了便于扩展，这里直接定义一个map存储额外的数据
    private Map<String,Object> data = new HashMap<>();
    public int getUserId() {
        return userId;
    }
    //这里设置set方法的返回值是event是为了方便链式调用
    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object o) {
        this.data.put(key,o);
        return this;
    }

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }
}
