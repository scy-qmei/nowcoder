package com.nowcoder.community.util;

public interface CommunityConstants {
    int ACTIVATE_SUCCESS = 0;
    int ACTIVATE_REPEATE = 1;
    int ACTIVATE_FAILURE = 2;

    //勾选记住用户，那么登录凭证有效期为100天
    int REMEMBER_USER = 3600 * 24 * 100;
    //不勾选记住用户，那么登录凭证的有效期只有12小时
    int NOT_REMEMBER_USER = 3600 * 12;

    //实体的类型是帖子
    int COMMENT_TYPE_POST = 1;
    //实体的类型是评论
    int COMMENT_TYPE_REPLY = 2;
    //实体的类型是用户
    int ENTITY_TYPE_USER = 3;

    //消息主题是评论
    String TOPIC_COMMENT = "comment";
    //消息的主题是点赞
    String TOPIC_LIKE = "like";
    //消息的主题是关注
    String TOPIC_FOLLOW = "follow";
    //系统用户ID
    int SYSTEM_USER_ID = 1;
    //消息的主题是涉及评论的修改，要及时同步到es
    String ES_DISCUSSPOST_UPDATE = "update";
    //消息的主题是涉及评论的删除，要及时同步到es
    String ES_DISCUSSPOST_DELETE = "delete";
    //用户权限是普通用户
    String AUTHORITY_USER = "user";
    //用户权限是管理员
    String AUTHORITY_ADMIN = "admin";
    //用户权限是版主
    String AUTHORITY_MODERATOR = "moderator";
    //消息的主题是分享
    String TOPIC_SHARE = "share";
}
