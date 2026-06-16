package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.entity.BO.RelationAddBO;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.BO.RelationUpdateBO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelationControllerPermissionTest {

    @Test
    void relationEndpointsShouldNotRequireRoleMenuPermissions() throws NoSuchMethodException {
        List<Method> endpoints = List.of(
                RelationController.class.getMethod("add", RelationAddBO.class),
                RelationController.class.getMethod("queryPageList", RelationQueryBO.class),
                RelationController.class.getMethod("detail", Long.class),
                RelationController.class.getMethod("update", RelationUpdateBO.class),
                RelationController.class.getMethod("delete", Long.class),
                RelationController.class.getMethod("addFromContact", Long.class)
        );

        assertThat(RelationController.class.isAnnotationPresent(RequirePermission.class)).isFalse();
        assertThat(endpoints)
                .allSatisfy(endpoint -> assertThat(endpoint.isAnnotationPresent(RequirePermission.class))
                        .as(endpoint.getName())
                        .isFalse());
    }
}
