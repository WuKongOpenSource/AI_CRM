package com.kakarote.ai_crm.ai.tools.support;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class AiToolPermissionAspect {

    @Autowired
    private PermissionService permissionService;

    @Around("@annotation(com.kakarote.ai_crm.ai.tools.support.AiToolPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        AiToolPermission permission = AnnotationUtils.findAnnotation(method, AiToolPermission.class);
        if (permission == null) {
            return joinPoint.proceed();
        }

        if (permissionService.hasPermission(permission.value())) {
            return joinPoint.proceed();
        }

        if (String.class.equals(method.getReturnType())) {
            return "您没有「" + permission.action() + "」所需的权限，AI 助手无法继续执行。";
        }

        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH);
    }
}
