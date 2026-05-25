package com.kakarote.ai_crm.ai.app;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.entity.VO.ChatAppOptionVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatApplicationRegistry {

    public static final String TOOL_GROUP_CRM = "crm";
    public static final String TOOL_GROUP_KNOWLEDGE = "knowledge";

    private final Map<String, ChatApplicationDefinition> applications = new LinkedHashMap<>();

    public ChatApplicationRegistry() {
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.GENERAL,
                "通用助手",
                "sparkles",
                "普通 AI 对话，不调用 CRM 或知识库业务工具。",
                """
                你是通用 AI 助手。请用中文回答，表达清晰、准确、克制。
                当前没有进入任何业务应用，不要声称已经查询或修改 CRM、知识库等系统数据。
                如果用户需要查看或管理客户信息，提示《请先在下方输入框左下角点击+号，勾选“悟空技能”中的“CRM管理”》；如果用户需要基于文档或知识库回答，引导其选择知识库应用。
                """,
                false,
                List.of(),
                List.of("帮我总结这段内容", "帮我润色一封邮件", "解释一下这个概念")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.CRM,
                "CRM管理",
                "customer",
                "管理客户、联系人、任务、日程、跟进记录，并可参考客户相关知识库内容。",
                """
                当前应用是 CRM 管理。你可以围绕客户、联系人、任务、日程、跟进记录和客户相关知识库内容提供帮助。
                只有在工具结果确认成功后，才能说数据已创建、更新或关联成功。
                """,
                false,
                List.of(TOOL_GROUP_CRM, TOOL_GROUP_KNOWLEDGE),
                List.of("把今天新增但还没跟进的客户列出来", "找出快丢单的客户", "筛选出高意向客户", "总结本周的销售情况")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.KNOWLEDGE,
                "知识库",
                "knowledge-1",
                "基于知识库文档进行问答、检索、总结和引用来源。",
                """
                当前应用是知识库问答检索。请优先基于知识库内容回答问题，并在可用时给出参考文件或依据。
                如果知识库中没有相关信息，请如实说明，不要编造结论。
                不要创建、修改或删除 CRM 数据；如果用户提出 CRM 数据操作，请提示其切换到 CRM 管理应用。
                """,
                true,
                List.of(TOOL_GROUP_KNOWLEDGE),
                List.of("总结选中文档的重点", "合同付款条款是什么？", "找出会议里的待办事项", "这些资料里提到哪些风险？")
        ));
    }

    public ChatApplicationDefinition resolve(String appCode) {
        return applications.getOrDefault(normalize(appCode), applications.get(ChatApplicationCodes.GENERAL));
    }

    public String normalize(String appCode) {
        String normalized = StrUtil.blankToDefault(appCode, ChatApplicationCodes.GENERAL).trim().toLowerCase();
        return applications.containsKey(normalized) ? normalized : ChatApplicationCodes.GENERAL;
    }

    public List<ChatAppOptionVO> listOptions() {
        List<ChatAppOptionVO> options = new ArrayList<>();
        applications.values().forEach(app -> {
            ChatAppOptionVO vo = new ChatAppOptionVO();
            vo.setCode(app.code());
            vo.setLabel(app.label());
            vo.setIconName(app.iconName());
            vo.setDescription(app.description());
            vo.setDefaultRagEnabled(app.defaultRagEnabled());
            vo.setRecommendedQuestions(app.recommendedQuestions());
            options.add(vo);
        });
        return options;
    }

    public boolean hasToolGroup(String appCode, String toolGroup) {
        return resolve(appCode).toolGroups().contains(toolGroup);
    }

    public boolean isKnowledgeApp(String appCode) {
        return ChatApplicationCodes.KNOWLEDGE.equals(resolve(appCode).code());
    }

    private void register(ChatApplicationDefinition application) {
        applications.put(application.code(), application);
    }
}
