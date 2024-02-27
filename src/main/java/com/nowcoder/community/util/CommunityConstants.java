package com.nowcoder.community.util;

public interface CommunityConstants {
    int ACTIVATE_SUCCESS = 0;
    int ACTIVATE_REPEATE = 1;
    int ACTIVATE_FAILURE = 2;

    //勾选记住用户，那么登录凭证有效期为100天
    int REMEMBER_USER = 3600 * 24 * 100;
    //不勾选记住用户，那么登录凭证的有效期只有12小时
    int NOT_REMEMBER_USER = 3600 * 12;
}
