package com.kakarote.ai_crm.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeAnswerLocalizationUtilTest {

    @Test
    void shouldLocalizeCommonEnglishSectionTitles() {
        String content = """
            ## Summary

            Based on the retrieved information:
            - Item A

            ### References
            """;

        String localized = KnowledgeAnswerLocalizationUtil.localizeToChinese(content);

        assertThat(localized).contains("## 摘要");
        assertThat(localized).contains("根据检索信息");
        assertThat(localized).contains("### 参考文件");
        assertThat(localized).doesNotContain("Summary");
        assertThat(localized).doesNotContain("retrieved information");
        assertThat(localized).doesNotContain("References");
    }

    @Test
    void shouldLocalizePlainStandaloneTitlesAndInlineLabels() {
        String content = """
            Summary
            根据 retrieved information:
            **Summary**: test
            """;

        String localized = KnowledgeAnswerLocalizationUtil.localizeToChinese(content);

        assertThat(localized).contains("摘要");
        assertThat(localized).contains("根据检索信息：");
        assertThat(localized).contains("**摘要**： test");
        assertThat(localized).doesNotContain("Summary");
        assertThat(localized).doesNotContain("retrieved information");
    }
}
