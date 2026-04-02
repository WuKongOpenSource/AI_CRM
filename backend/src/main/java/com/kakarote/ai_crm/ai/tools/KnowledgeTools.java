package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * 知识库相关 AI Tool
 */
@Slf4j
@Component
public class KnowledgeTools {

    @Autowired
    private IKnowledgeService knowledgeService;

    @Autowired
    private WeKnoraClient weKnoraClient;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Tool(description = "查看知识库文件列表。当用户查询文档、资料、知识库、会议记录、合同等时调用。")
    @AiToolPermission(value = "knowledge:view", action = "查看知识库")
    public String getKnowledgeBase(
            @ToolParam(description = "搜索关键词，可搜索文件名或内容", required = false) String keyword,
            @ToolParam(description = "文件类型：meeting(会议记录)/email(邮件)/recording(录音)/document(文档)/proposal(方案)/contract(合同)", required = false) String type,
            @ToolParam(description = "关联客户ID，数字类型", required = false) String customerIdStr) {

        try {
            Long customerId = null;
            if (StrUtil.isNotBlank(customerIdStr) && !"null".equalsIgnoreCase(customerIdStr)) {
                try {
                    customerId = Long.parseLong(customerIdStr);
                } catch (NumberFormatException ignore) {
                    log.debug("忽略无效的 customerId: {}", customerIdStr);
                }
            }

            KnowledgeQueryBO queryBO = new KnowledgeQueryBO();
            queryBO.setKeyword(keyword);
            queryBO.setType(type);
            queryBO.setCustomerId(customerId);
            queryBO.setPage(1);
            queryBO.setLimit(10);

            BasePage<KnowledgeVO> page = knowledgeService.queryPageList(queryBO);
            if (page.getList().isEmpty()) {
                return "没有找到符合条件的知识库文件。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("## 知识库文件（共 %d 个，显示前 10 个）\n\n", page.getTotalRow()));

            for (KnowledgeVO knowledge : page.getList()) {
                sb.append(String.format("- **%s** [%s]", knowledge.getName(), getTypeLabel(knowledge.getType())));

                if (knowledge.getCustomerName() != null) {
                    sb.append(String.format("，客户：%s", knowledge.getCustomerName()));
                }
                if (knowledge.getCreateTime() != null) {
                    sb.append(String.format("，上传于：%s", dateFormat.format(knowledge.getCreateTime())));
                }
                if (StrUtil.isNotBlank(knowledge.getSummary())) {
                    sb.append(String.format("\n  > %s", knowledge.getSummary()));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "查询知识库失败: " + e.getMessage();
        }
    }

    @Tool(description = "获取知识库文件详情。当用户想查看某个文件的基本信息、摘要、上传时间时调用。")
    @AiToolPermission(value = "knowledge:view", action = "查看知识库详情")
    public String getKnowledgeDetail(
            @ToolParam(description = "知识库文件ID，数字类型") String knowledgeIdStr) {

        if (StrUtil.isBlank(knowledgeIdStr) || "null".equalsIgnoreCase(knowledgeIdStr)) {
            return "获取文件详情失败: 缺少文件ID参数";
        }

        try {
            Long knowledgeId = Long.parseLong(knowledgeIdStr);
            KnowledgeVO knowledge = knowledgeService.getKnowledgeDetail(knowledgeId);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("## 文件: %s\n\n", knowledge.getName()));
            sb.append(String.format("- **类型**: %s\n", getTypeLabel(knowledge.getType())));

            if (knowledge.getCustomerName() != null) {
                sb.append(String.format("- **关联客户**: %s\n", knowledge.getCustomerName()));
            }
            if (knowledge.getFileSize() != null) {
                sb.append(String.format("- **文件大小**: %s\n", formatFileSize(knowledge.getFileSize())));
            }
            if (knowledge.getUploadUserName() != null) {
                sb.append(String.format("- **上传人**: %s\n", knowledge.getUploadUserName()));
            }
            if (knowledge.getCreateTime() != null) {
                sb.append(String.format("- **上传时间**: %s\n", dateFormat.format(knowledge.getCreateTime())));
            }
            if (StrUtil.isNotBlank(knowledge.getSummary())) {
                sb.append(String.format("\n### 摘要\n%s\n", knowledge.getSummary()));
            }

            return sb.toString();
        } catch (NumberFormatException e) {
            return "获取文件详情失败: 文件ID格式无效";
        } catch (Exception e) {
            return "获取文件详情失败: " + e.getMessage();
        }
    }

    @Tool(description = "直接向 RAG 知识库发起问答。当用户询问知识库文件内容、合同条款、会议结论、文档总结，或者希望基于知识库直接得到答案时优先调用。可选传入知识库文件ID，将回答范围限制在指定文件内。")
    @AiToolPermission(value = "knowledge:view", action = "知识库问答")
    public String askKnowledgeQuestion(
            @ToolParam(description = "用户的问题，例如：合同付款条款是什么？") String query,
            @ToolParam(description = "可选：知识库文件ID，多个用英文逗号分隔，例如：123,456。用于限定只基于这些文件回答。", required = false) String knowledgeIdsStr) {

        if (!weKnoraClient.isEnabled()) {
            return "知识库问答功能未启用，请先配置 RAG 服务。";
        }
        if (StrUtil.isBlank(query)) {
            return "知识库问答失败：问题不能为空。";
        }

        try {
            Long conversationId = AiContextHolder.getCurrentSessionId();
            KnowledgeScope scope = resolveKnowledgeScope(knowledgeIdsStr);
            log.debug("RAG知识库问答开始: conversationId={}, query={}, scopedIds={}, invalidIds={}",
                    conversationId, abbreviateForLog(query),
                    scope.weKnoraKnowledgeIds(), scope.invalidKnowledgeIds());
            WeKnoraClient.WeKnoraChatResult result = weKnoraClient.askKnowledgeQuestion(
                    conversationId,
                    query,
                    scope.weKnoraKnowledgeIds()
            );
            log.debug("RAG知识库问答返回: conversationId={}, answerLength={}, references={}, completed={}",
                    conversationId,
                    result.getAnswer() != null ? result.getAnswer().length() : 0,
                    result.getReferences() != null ? result.getReferences().size() : 0,
                    result.isCompleted());

            if (isUnavailableRagAnswer(result.getAnswer())) {
                log.debug("RAG知识库问答结果不可用，尝试检索兜底: conversationId={}", conversationId);
                String fallback = buildRagFallbackContext(query, scope);
                if (StrUtil.isNotBlank(fallback)) {
                    log.debug("RAG知识库问答兜底成功: conversationId={}, fallbackLength={}",
                            conversationId, fallback.length());
                    return fallback;
                }
                if (!scope.invalidKnowledgeIds().isEmpty()) {
                    return "当前未能直接从知识库生成回答。以下文件ID无效或尚未完成解析: " + scope.invalidKnowledgeIds();
                }
                return "当前未能直接从知识库生成回答，请尝试换一种问法，或先查看相关文档片段。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("## RAG知识库回答\n\n");
            sb.append(result.getAnswer().trim());

            if (!scope.invalidKnowledgeIds().isEmpty()) {
                sb.append("\n\n> 以下文件ID未参与问答：");
                sb.append(scope.invalidKnowledgeIds());
            }

            appendReferenceSummary(sb, result.getReferences());
            return sb.toString();
        } catch (Exception e) {
            log.warn("RAG 知识库问答失败: query={}, error={}", query, e.getMessage(), e);
            String fallback = buildRagFallbackContext(query, resolveKnowledgeScope(knowledgeIdsStr));
            if (StrUtil.isNotBlank(fallback)) {
                return fallback;
            }
            return "知识库问答失败: " + e.getMessage();
        }
    }

    @Tool(description = "在知识库中语义检索原始文档片段。当用户明确要查看原文、核对出处、查找相关片段或列出命中文档时调用。若目标是直接回答知识库问题，优先使用 askKnowledgeQuestion。")
    @AiToolPermission(value = "knowledge:view", action = "搜索知识库内容")
    public String searchKnowledgeContent(
            @ToolParam(description = "搜索关键词或问题，例如：合同付款条款、客户需求分析、会议讨论的技术方案") String query) {

        if (!weKnoraClient.isEnabled()) {
            return "语义检索功能未启用，请联系管理员配置 RAG 服务。";
        }

        try {
            log.debug("RAG语义检索开始: query={}", abbreviateForLog(query));
            List<WeKnoraChunk> chunks = weKnoraClient.searchKnowledge(query);
            log.debug("RAG语义检索完成: query={}, chunkCount={}", abbreviateForLog(query), chunks.size());

            if (chunks.isEmpty()) {
                return "未找到与 \"" + query + "\" 相关的文档内容。您可以尝试更换关键词，或先查看知识库文件列表。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("## 检索结果：与 \"%s\" 相关的文档片段\n\n", query));

            for (int i = 0; i < chunks.size(); i++) {
                WeKnoraChunk chunk = chunks.get(i);
                sb.append(String.format("### [%d] %s", i + 1, chunk.getKnowledgeTitle()));

                if (chunk.getScore() != null && chunk.getScore() > 0) {
                    sb.append(String.format("（相关度: %.0f%%）", chunk.getScore() * 100));
                }
                sb.append("\n\n");
                sb.append(StrUtil.blankToDefault(chunk.getContent(), "暂无片段内容"));
                sb.append("\n\n");

                if (i < chunks.size() - 1) {
                    sb.append("---\n\n");
                }
            }

            sb.append("\n> 以上内容来自 RAG 语义检索结果，适合用于核对原文或出处。");
            return sb.toString();
        } catch (Exception e) {
            return "语义检索失败: " + e.getMessage();
        }
    }

    private String buildRagFallbackContext(String query, KnowledgeScope scope) {
        try {
            log.debug("构建RAG检索兜底内容: query={}, scopedKnowledgeIds={}",
                    abbreviateForLog(query),
                    scope != null ? scope.weKnoraKnowledgeIds() : Collections.emptyList());
            List<WeKnoraChunk> chunks = weKnoraClient.searchKnowledge(query);
            if (CollUtil.isEmpty(chunks)) {
                log.debug("RAG检索兜底无结果: query={}", abbreviateForLog(query));
                return "";
            }

            if (scope != null && CollUtil.isNotEmpty(scope.weKnoraKnowledgeIds())) {
                Set<String> allowedIds = Set.copyOf(scope.weKnoraKnowledgeIds());
                chunks = chunks.stream()
                        .filter(chunk -> allowedIds.contains(chunk.getKnowledgeId()))
                        .toList();
            }

            if (CollUtil.isEmpty(chunks)) {
                log.debug("RAG检索兜底在范围过滤后无结果: query={}", abbreviateForLog(query));
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("## RAG检索结果\n\n");
            sb.append("当前未能直接生成最终答案，下面是与问题最相关的文档片段，可据此继续回答用户：\n\n");

            for (int i = 0; i < chunks.size(); i++) {
                WeKnoraChunk chunk = chunks.get(i);
                sb.append(String.format("### [%d] %s", i + 1,
                        StrUtil.blankToDefault(chunk.getKnowledgeTitle(), "未命名文档")));
                if (chunk.getScore() != null && chunk.getScore() > 0) {
                    sb.append(String.format("（相关度: %.0f%%）", chunk.getScore() * 100));
                }
                sb.append("\n\n");
                sb.append(StrUtil.blankToDefault(chunk.getContent(), "暂无片段内容"));
                sb.append("\n\n");
            }

            if (scope != null && !scope.invalidKnowledgeIds().isEmpty()) {
                sb.append("> 以下文件ID未参与检索：").append(scope.invalidKnowledgeIds()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建 RAG 检索兜底内容失败: query={}, error={}", query, e.getMessage());
            return "";
        }
    }

    private boolean isUnavailableRagAnswer(String answer) {
        if (StrUtil.isBlank(answer)) {
            return true;
        }
        String normalized = answer.trim();
        return normalized.contains("NO_MATCH")
                || "对不起，我无法回答这个问题".equals(normalized)
                || "抱歉，我无法回答这个问题。".equals(normalized)
                || "抱歉，我无法回答这个问题".equals(normalized);
    }

    private KnowledgeScope resolveKnowledgeScope(String knowledgeIdsStr) {
        if (StrUtil.isBlank(knowledgeIdsStr)) {
            return new KnowledgeScope(Collections.emptyList(), Collections.emptyList());
        }

        List<String> weKnoraKnowledgeIds = new ArrayList<>();
        List<String> invalidKnowledgeIds = new ArrayList<>();

        for (String part : knowledgeIdsStr.split(",")) {
            String trimmed = StrUtil.trim(part);
            if (StrUtil.isBlank(trimmed)) {
                continue;
            }

            try {
                Long knowledgeId = Long.parseLong(trimmed);
                Knowledge knowledge = knowledgeService.getById(knowledgeId);
                if (knowledge == null
                        || StrUtil.isBlank(knowledge.getWeKnoraKnowledgeId())
                        || !"completed".equalsIgnoreCase(knowledge.getWeKnoraParseStatus())) {
                    invalidKnowledgeIds.add(trimmed);
                    continue;
                }
                weKnoraKnowledgeIds.add(knowledge.getWeKnoraKnowledgeId());
            } catch (NumberFormatException e) {
                invalidKnowledgeIds.add(trimmed);
            }
        }

        log.debug("解析知识库范围完成: input={}, validWeKnoraIds={}, invalidIds={}",
                knowledgeIdsStr, weKnoraKnowledgeIds, invalidKnowledgeIds);
        return new KnowledgeScope(weKnoraKnowledgeIds, invalidKnowledgeIds);
    }

    private void appendReferenceSummary(StringBuilder sb, List<WeKnoraChunk> references) {
        if (CollUtil.isEmpty(references)) {
            return;
        }

        LinkedHashMap<String, WeKnoraChunk> uniqueReferences = new LinkedHashMap<>();
        for (WeKnoraChunk reference : references) {
            if (reference == null) {
                continue;
            }
            String fileName = StrUtil.blankToDefault(
                    reference.getKnowledgeTitle(),
                    StrUtil.blankToDefault(reference.getKnowledgeFilename(), "未命名文件")
            );
            uniqueReferences.putIfAbsent(fileName, reference);
        }

        if (uniqueReferences.isEmpty()) {
            return;
        }

        sb.append("\n\n### 参考文件\n");
        for (WeKnoraChunk reference : uniqueReferences.values()) {
            String fileName = StrUtil.blankToDefault(
                    reference.getKnowledgeTitle(),
                    StrUtil.blankToDefault(reference.getKnowledgeFilename(), "未命名文件")
            );
            sb.append("- ").append(fileName);
            if (reference.getScore() != null && reference.getScore() > 0) {
                sb.append(String.format("（相关度: %.0f%%）", reference.getScore() * 100));
            }
            if (StrUtil.isNotBlank(reference.getContent())) {
                sb.append("：");
                sb.append(ellipsize(reference.getContent(), 120));
            }
            sb.append("\n");
        }
    }

    private String ellipsize(String text, int maxLength) {
        if (StrUtil.isBlank(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String abbreviateForLog(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private String getTypeLabel(String type) {
        if (type == null) {
            return "文档";
        }
        return switch (type.toLowerCase()) {
            case "meeting" -> "会议记录";
            case "email" -> "邮件";
            case "recording" -> "录音";
            case "document" -> "文档";
            case "proposal" -> "方案";
            case "contract" -> "合同";
            default -> type;
        };
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) {
            return "未知";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private record KnowledgeScope(List<String> weKnoraKnowledgeIds, List<String> invalidKnowledgeIds) {
    }
}
