package com.kakarote.ai_crm.ai.tools.support;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Captures every Spring AI tool result, including permission-denial strings
 * returned by inner aspects, so ChatService can surface failures deterministically.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiToolExecutionRecordingAspect {

    @Autowired
    private AiToolExecutionRecorder recorder;

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object recordToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String toolName = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        try {
            Object result = joinPoint.proceed();
            recorder.record(toolName, methodName, result, null);
            return result;
        } catch (Throwable throwable) {
            recorder.record(toolName, methodName, null, throwable);
            throw throwable;
        }
    }
}
