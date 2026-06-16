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
     * 用户当前有效 token redis key
     */
    public static final String LOGIN_USER_TOKEN_KEY = "login_user_token:";

    /**
     * 被新登录挤下线的提示 redis key
     */
    public static final String LOGIN_KICKOUT_KEY = "login_kickout:";

    /**
     * token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * token 中的登录用户标识
     */
    public static final String LOGIN_USER_KEY = "userKey";

    /**
     * 被挤下线提示在请求上下文中的属性名
     */
    public static final String LOGIN_KICKOUT_MESSAGE_ATTR = "loginKickoutMessage";

    /**
     * 数据最多查询数量
     */
    public static final Integer MAX_QUERY_SIZE = 100;

    /**
     * 查询数据权限递归次数
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
