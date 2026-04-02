package com.kakarote.ai_crm.common.auth;

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
public class PermissionAspect {

    @Autowired
    private PermissionService permissionService;

    @Around("@annotation(com.kakarote.ai_crm.common.auth.RequirePermission) || @within(com.kakarote.ai_crm.common.auth.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequirePermission requirePermission = resolveAnnotation(joinPoint);
        if (requirePermission == null) {
            return joinPoint.proceed();
        }
        if (!permissionService.hasPermission(requirePermission.value())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH);
        }
        return joinPoint.proceed();
    }

    private RequirePermission resolveAnnotation(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequirePermission methodAnnotation = AnnotationUtils.findAnnotation(method, RequirePermission.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RequirePermission.class);
    }
}
