package com.kakarote.ai_crm.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeServiceImplTest {

    private final KnowledgeServiceImpl knowledgeService = new KnowledgeServiceImpl();

    @Test
    void shouldRemoveNullBytesAndControlCharactersBeforePersistingSearchableContent() {
        String normalized = ReflectionTestUtils.invokeMethod(
            knowledgeService,
            "normalizeSearchableContent",
            "Alpha\u0000Beta\u0007 \n Gamma\t"
        );

        assertThat(normalized).isEqualTo("AlphaBeta Gamma");
    }

    @Test
    void shouldReturnNullWhenSearchableContentOnlyContainsUnsupportedCharacters() {
        String normalized = ReflectionTestUtils.invokeMethod(
            knowledgeService,
            "normalizeSearchableContent",
            "\u0000\u0007\t  \n"
        );

        assertThat(normalized).isNull();
    }
}
