package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class RelationToolsPermissionTest {

    @Test
    void relationRemarkToolShouldNotRequireRoleMenuPermission() throws NoSuchMethodException {
        Method method = RelationTools.class.getMethod("updateCurrentRelationRemark", String.class, String.class);

        assertThat(method.isAnnotationPresent(AiToolPermission.class)).isFalse();
    }
}
