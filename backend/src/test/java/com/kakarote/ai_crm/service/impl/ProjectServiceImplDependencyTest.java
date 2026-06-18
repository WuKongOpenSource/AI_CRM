package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Lazy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectServiceImplDependencyTest {

    @Test
    void chatClientProviderDependencyIsLazyToAvoidToolRegistrationCycle() {
        Constructor<?> constructor = ProjectServiceImpl.class.getConstructors()[0];

        Parameter chatClientProviderParameter = Arrays.stream(constructor.getParameters())
                .filter(parameter -> DynamicChatClientProvider.class.equals(parameter.getType()))
                .findFirst()
                .orElseThrow();

        assertTrue(
                chatClientProviderParameter.isAnnotationPresent(Lazy.class),
                "ProjectServiceImpl should lazily inject DynamicChatClientProvider to avoid DynamicChatClientProvider -> TaskTools -> ProjectServiceImpl startup cycle"
        );
    }
}
