package com.nowcoder.community.util;

public class  RedisKeyUtil {
    private static final String SPLIT = ":";
    private static final String PREFIEX_ENTITY_LIKE = "like:entity";
    private  static final String PREFIX_USER_LIKE = "like:user";
    //关注功能所需要的key flowee:用户ID:关注的实体类型
    private static final String PREFIX_FOLLOWEE = "followee";
    //查看粉丝功能所需要的key flower:实体类型:实体ID
    private static final String PREFIX_FOLLOWER = "follower";
    //验证码存储所需的key
    private static final String PREFIX_KAPTCHA = "kaptcha";
    //存储登录凭证的数据类型的key
    private static final String PREFIX_TICKET = "ticket";
    private static final String PREFIX_USER = "user";
    //统计网站UV的key
    private static final String PREFIX_UV = "uv";
    //统计网站dau的key
    private static final String PREFIX_DAU = "dau";
    //分数发生修改，需要重新计算的帖子的id的key
    private static final String PREFIX_POST = "post";

    //生成实体的赞的redis的key
    public static String generateKey(int entityType, int entityId) {
        return PREFIEX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }
    //生成用户受到的赞的数量的redis的key
    public static String generateUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }
    //生成关注功能需要的key
    public static String generateFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }
    //生成查看关注者功能所需要的key
    public static String generateFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    /**
     * 这里验证码是需要有一个归属者的，以生成独特的key存储在redis中
     * 但是第一次进入登录页面，用户没有登录，无法获取用户的id，所以这里采用的方式是进入登录页面，服务器响应给浏览器一个cookie随机字符串，浏览器存储下来，每次请求携带
     * 于是就根据这个分配的随机字符串来区分用户，并生成最终的验证码！
     * @param owner
     * @return
     */
    public static String getKaptcha(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }
    //这里通过登录凭证的凭证码来生成在redis中存储的唯一key！
    public static String getTicket(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }
    public static String getUser(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    /**
     * 生成单日UV的key
     * @param date 字符串形式的日期，格式为yyyyMMdd
     * @return 返回的就是key
     */
    public static String getUV(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    /**
     * 生成一段时间的UV的key
     * @param from 起始日期
     * @param to 终止日期
     * @return
     */
    public static String getUV(String from, String to) {
        return PREFIX_UV + SPLIT + from + SPLIT + to;
    }

    /**
     * 生成单日DAU的key
     * @param date 当前日期的字符串形式
     * @return
     */
    public static String getDAU(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    /**
     * 生成一段时间的DAU的key
     * @param from 起始日期
     * @param to 终止日期
     * @return
     */
    public static String getDAU(String from, String to) {
        return PREFIX_DAU + SPLIT + from + SPLIT + to;
    }

    /**
     * 获取存储需要修改分数的帖子的id的key
     * @return
     */
    public static String getPostScorekey() {
        return PREFIX_POST + SPLIT + "score";
    }
}
