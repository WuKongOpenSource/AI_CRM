package com.kakarote.ai_crm.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 统一将知识库回答中常见的英文标题和标签本地化为中文，避免直接展示给终端用户。
 */
public final class KnowledgeAnswerLocalizationUtil {

    /**
     * 初始化知识答案Localization实例。
     */
    private KnowledgeAnswerLocalizationUtil() {
    }

    /**
     * 处理localizeToChinese方法逻辑。
     */
    public static String localizeToChinese(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }

        String normalized = content.replace("\r\n", "\n").trim();

        normalized = replaceMarkdownHeading(normalized, "summary", "摘要");
        normalized = replaceMarkdownHeading(normalized, "executive\\s+summary", "摘要");
        normalized = replaceMarkdownHeading(normalized, "retrieved\\s+information", "检索信息");
        normalized = replaceMarkdownHeading(normalized, "key\\s+points?", "关键信息");
        normalized = replaceMarkdownHeading(normalized, "key\\s+findings?", "关键信息");
        normalized = replaceMarkdownHeading(normalized, "references?", "参考文件");
        normalized = replaceMarkdownHeading(normalized, "reference\\s+documents?", "参考文件");
        normalized = replaceMarkdownHeading(normalized, "reference\\s+files?", "参考文件");
        normalized = replaceMarkdownHeading(normalized, "sources?", "参考文件");

        normalized = replaceStandaloneLine(normalized, "summary", "摘要");
        normalized = replaceStandaloneLine(normalized, "executive\\s+summary", "摘要");
        normalized = replaceStandaloneLine(normalized, "retrieved\\s+information", "检索信息");
        normalized = replaceStandaloneLine(normalized, "key\\s+points?", "关键信息");
        normalized = replaceStandaloneLine(normalized, "key\\s+findings?", "关键信息");
        normalized = replaceStandaloneLine(normalized, "references?", "参考文件");
        normalized = replaceStandaloneLine(normalized, "reference\\s+documents?", "参考文件");
        normalized = replaceStandaloneLine(normalized, "reference\\s+files?", "参考文件");
        normalized = replaceStandaloneLine(normalized, "sources?", "参考文件");

        normalized = replaceBoldLabel(normalized, "summary", "摘要");
        normalized = replaceBoldLabel(normalized, "retrieved\\s+information", "检索信息");
        normalized = replaceBoldLabel(normalized, "references?", "参考文件");

        normalized = normalized
                .replaceAll("(?i)根据\\s*retrieved\\s+information\\s*[:：]?", "根据检索信息：")
                .replaceAll("(?i)according\\s+to\\s+the\\s+retrieved\\s+information\\s*[:：]?", "根据检索信息")
                .replaceAll("(?i)based\\s+on\\s+the\\s+retrieved\\s+information\\s*[:：]?", "根据检索信息")
                .replaceAll("(?i)according\\s+to\\s+retrieved\\s+information\\s*[:：]?", "根据检索信息")
                .replaceAll("(?i)based\\s+on\\s+retrieved\\s+information\\s*[:：]?", "根据检索信息")
                .replaceAll("(?im)\\bretrieved\\s+information\\s*[:：]", "检索信息：")
                .replaceAll("(?im)(^|[\\s（(])summary\\s*[:：]", "$1摘要：")
                .replaceAll("(?im)(^|[\\s（(])references?\\s*[:：]", "$1参考文件：");

        return normalized.trim();
    }

    /**
     * 处理replaceMarkdownHeading方法逻辑。
     */
    private static String replaceMarkdownHeading(String content, String englishPattern, String chineseHeading) {
        return content.replaceAll("(?im)^(#{1,6}\\s*)" + englishPattern + "\\s*$", "$1" + chineseHeading);
    }

    /**
     * 处理replaceStandaloneLine方法逻辑。
     */
    private static String replaceStandaloneLine(String content, String englishPattern, String chineseHeading) {
        return content.replaceAll("(?im)^\\s*" + englishPattern + "\\s*$", chineseHeading);
    }

    /**
     * 处理replaceBoldLabel方法逻辑。
     */
    private static String replaceBoldLabel(String content, String englishPattern, String chineseHeading) {
        return content.replaceAll("(?im)\\*\\*\\s*" + englishPattern + "\\s*\\*\\*\\s*[:：]?", "**" + chineseHeading + "**：");
    }
}
