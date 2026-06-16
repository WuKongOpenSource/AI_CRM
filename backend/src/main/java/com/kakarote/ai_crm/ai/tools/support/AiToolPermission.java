package com.kakarote.ai_crm.ai.tools.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

    /**
     * 处理value方法逻辑。
     */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AiToolPermission {

    String value();

    /**
     * 处理action方法逻辑。
     */
    String action();
}
