package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 知识库相关 AI Tool - 用于 Spring AI Function Calling
 */
@Component
public class KnowledgeTools {

    @Autowired
    private IKnowledgeService knowledgeService;

    @Autowired
    private WeKnoraClient weKnoraClient;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Tool(description = "查看知识库文件列表。当用户查询文档、资料、知识库、会议记录、合同等时调用。")
    public String getKnowledgeBase(
            @ToolParam(description = "搜索关键词，可搜索文件名或内容", required = false) String keyword,
            @ToolParam(description = "文件类型：meeting(会议记录)/email(邮件)/recording(录音)/document(文档)/proposal(方案)/contract(合同)", required = false) String type,
            @ToolParam(description = "关联客户ID，数字类型", required = false) String customerIdStr) {

        try {
            // 将 String 转换为 Long，处理 null 和 "null" 字符串
            Long customerId = null;
            if (customerIdStr != null && !customerIdStr.isEmpty() && !"null".equalsIgnoreCase(customerIdStr)) {
                try {
                    customerId = Long.parseLong(customerIdStr);
                } catch (NumberFormatException e) {
                    // 忽略无效的 customerId
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
            sb.append(String.format("## 知识库文件（共%d个，显示前10个）\n\n", page.getTotalRow()));

            for (KnowledgeVO knowledge : page.getList()) {
                sb.append(String.format("- **%s** [%s]",
                    knowledge.getName(),
                    getTypeLabel(knowledge.getType())));

                if (knowledge.getCustomerName() != null) {
                    sb.append(String.format("，客户: %s", knowledge.getCustomerName()));
                }
                if (knowledge.getCreateTime() != null) {
                    sb.append(String.format("，上传于: %s", dateFormat.format(knowledge.getCreateTime())));
                }
                if (knowledge.getSummary() != null) {
                    sb.append(String.format("\n  > %s", knowledge.getSummary()));
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "查询知识库失败: " + e.getMessage();
        }
    }

    @Tool(description = "获取知识库文件详情。当用户想查看某个文件的具体内容时调用。")
    public String getKnowledgeDetail(
            @ToolParam(description = "知识库文件ID，数字类型") String knowledgeIdStr) {

        try {
            // 将 String 转换为 Long，处理 null 和 "null" 字符串
            if (knowledgeIdStr == null || knowledgeIdStr.isEmpty() || "null".equalsIgnoreCase(knowledgeIdStr)) {
                return "获取文件详情失败: 缺少文件ID参数";
            }
            Long knowledgeId;
            try {
                knowledgeId = Long.parseLong(knowledgeIdStr);
            } catch (NumberFormatException e) {
                return "获取文件详情失败: 文件ID格式无效";
            }

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
                sb.append(String.format("- **上传者**: %s\n", knowledge.getUploadUserName()));
            }
            if (knowledge.getCreateTime() != null) {
                sb.append(String.format("- **上传时间**: %s\n", dateFormat.format(knowledge.getCreateTime())));
            }
            if (knowledge.getSummary() != null) {
                sb.append(String.format("\n### 摘要\n%s\n", knowledge.getSummary()));
            }

            return sb.toString();
        } catch (Exception e) {
            return "获取文件详情失败: " + e.getMessage();
        }
    }

    @Tool(description = "在知识库中语义搜索相关文档内容。当用户询问文档具体内容、查找特定信息、需要基于文档内容回答问题时调用。这是一个智能搜索工具，能理解问题语义并返回最相关的文档片段。")
    public String searchKnowledgeContent(
            @ToolParam(description = "搜索关键词或问题，例如：'合同付款条款'、'客户需求分析'、'会议讨论的技术方案'") String query) {

        if (!weKnoraClient.isEnabled()) {
            return "语义搜索功能未启用，请联系管理员配置 WeKnora 服务。";
        }

        try {
            List<WeKnoraChunk> chunks = weKnoraClient.searchKnowledge(query);

            if (chunks.isEmpty()) {
                return "未找到与 \"" + query + "\" 相关的文档内容。您可以尝试使用不同的关键词搜索，或者使用知识库列表功能查看所有文档。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("## 搜索结果：与 \"%s\" 相关的文档内容\n\n", query));

            for (int i = 0; i < chunks.size(); i++) {
                WeKnoraChunk chunk = chunks.get(i);
                sb.append(String.format("### [%d] %s", i + 1, chunk.getKnowledgeTitle()));

                if (chunk.getScore() > 0) {
                    sb.append(String.format(" （相关度: %.0f%%）", chunk.getScore() * 100));
                }
                sb.append("\n\n");

                // Add chunk content
                sb.append(chunk.getContent());
                sb.append("\n\n");

                if (i < chunks.size() - 1) {
                    sb.append("---\n\n");
                }
            }

            sb.append("\n> 以上内容来自知识库的语义检索结果，可能需要进一步确认准确性。");

            return sb.toString();
        } catch (Exception e) {
            return "语义搜索失败: " + e.getMessage();
        }
    }

    private String getTypeLabel(String type) {
        if (type == null) return "文档";
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
        if (bytes == null) return "未知";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
