package com.kakarote.ai_crm.utils;


import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * @author z
 * 一些通用方法
 */
public class BaseUtil {

    private static final Snowflake SNOWFLAKE;

    static {
        /*
            TODO 目前使用自动生成的工作节点ID和数据中心ID,可使用自定义的数据中心ID
         */

        SNOWFLAKE = IdUtil.getSnowflake();
    }

    /**
     * 获取long类型的id，雪花算法
     * @return id
     */
    public static Long getNextId(){
        return SNOWFLAKE.nextId();
    }


}
