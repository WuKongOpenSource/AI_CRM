package com.kakarote.ai_crm.config;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class MyMetaObjectHandlerTest {

    private final MyMetaObjectHandler handler = new MyMetaObjectHandler();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        AiContextHolder.clearThreadContext();
        TenantContextHolder.clear();
    }

    @Test
    void insertFillDoesNotLogUserLookupErrorWithoutUserContext(CapturedOutput output) {
        SecurityContextHolder.clearContext();
        AiContextHolder.clearThreadContext();
        TenantContextHolder.clear();

        FillTarget target = new FillTarget();

        handler.insertFill(SystemMetaObject.forObject(target));

        assertThat(target.getCreateTime()).isNotNull();
        assertThat(target.getUpdateTime()).isNotNull();
        assertThat(target.getCreateUserId()).isNull();
        assertThat(output)
                .doesNotContain("无法获取用户ID")
                .doesNotContain("无法获取当前用户ID进行自动填充");
    }

    @Test
    void insertFillUsesAiThreadContextWhenSecurityContextIsMissing() {
        SecurityContextHolder.clearContext();
        AiContextHolder.bindThreadContext(123L, 456L);

        FillTarget target = new FillTarget();

        handler.insertFill(SystemMetaObject.forObject(target));

        assertThat(target.getCreateUserId()).isEqualTo(123L);
        assertThat(target.getTenantId()).isEqualTo(456L);
    }

    private static class FillTarget {

        private Date createTime;
        private Date updateTime;
        private Long createUserId;
        private Long tenantId;

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public Date getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(Date updateTime) {
            this.updateTime = updateTime;
        }

        public Long getCreateUserId() {
            return createUserId;
        }

        public void setCreateUserId(Long createUserId) {
            this.createUserId = createUserId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }
    }
}
