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
public class ProjectTaskAttachmentSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                ALTER TABLE crm_project_task_attachment
                    ADD COLUMN IF NOT EXISTS file_path TEXT
                """);
            jdbcTemplate.execute("""
                ALTER TABLE crm_project_task_attachment
                    ADD COLUMN IF NOT EXISTS file_size BIGINT
                """);
            jdbcTemplate.execute("""
                ALTER TABLE crm_project_task_attachment
                    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(255)
                """);
            log.info("Ensured crm_project_task_attachment file metadata columns");
        } catch (Exception exception) {
            log.warn("Failed to ensure crm_project_task_attachment file metadata columns", exception);
        }
    }
}
