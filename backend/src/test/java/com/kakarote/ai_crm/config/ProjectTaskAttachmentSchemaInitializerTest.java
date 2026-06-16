package com.kakarote.ai_crm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectTaskAttachmentSchemaInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensuresProjectTaskAttachmentMetadataColumns() {
        ProjectTaskAttachmentSchemaInitializer initializer = new ProjectTaskAttachmentSchemaInitializer(jdbcTemplate);

        initializer.run(null);

        verify(jdbcTemplate).execute(contains("ADD COLUMN IF NOT EXISTS file_path"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN IF NOT EXISTS file_size"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN IF NOT EXISTS mime_type"));
    }
}
