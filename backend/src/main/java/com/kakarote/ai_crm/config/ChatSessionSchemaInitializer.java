package com.kakarote.ai_crm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                ALTER TABLE crm_chat_session
                    ADD COLUMN IF NOT EXISTS project_id BIGINT,
                    ADD COLUMN IF NOT EXISTS project_task_id BIGINT,
                    ADD COLUMN IF NOT EXISTS product_id BIGINT
                """);
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_chat_session_product_id
                    ON crm_chat_session (product_id)
                """);
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_chat_session_project_id
                    ON crm_chat_session (project_id)
                """);
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_chat_session_project_task_id
                    ON crm_chat_session (project_task_id)
                """);
            log.info("Ensured crm_chat_session project context columns");
        } catch (Exception exception) {
            log.warn("Failed to ensure crm_chat_session project context columns", exception);
        }
    }
}
