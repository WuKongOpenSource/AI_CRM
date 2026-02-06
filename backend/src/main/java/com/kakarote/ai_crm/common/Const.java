package com.kakarote.ai_crm.common;

/**
 * 通用常量信息
 *
 * @author zhangzhiwei
 */
public class Const {

    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 令牌前缀
     */
    public static final String LOGIN_USER_KEY = "userKey";


    /**
     * 数据最多查询数量
     */
    public static final Integer MAX_QUERY_SIZE = 100;

    /**
     * 查询数据权限递归次数,可以通过继承这个类修改
     */
    public static final int AUTH_DATA_RECURSION_NUM = 20;

    /**
     * 批量保存数量
     */
    public static final Integer BATCH_SAVE_SIZE = 1000;

    /**
     * 权限缓存
     */
    public static final String USER_AUTH_CACHE_KET = "MANAGER:USER:AUTH:";
}
