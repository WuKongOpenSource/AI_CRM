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
                "普通 AI 对话，不主动调用 CRM 业务工具。",
                """
                当前应用是通用助手。请用中文回答，表达清晰、准确、克制。
                不要声称已经查询、创建、更新或删除 CRM 数据；如果用户需要管理业务数据，请提醒其切换到对应应用。
                """,
                false,
                List.of(),
                List.of("帮我总结这段内容", "帮我润色一封邮件", "解释一下这个概念")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.CRM,
                "CRM管理",
                "customer",
                "管理客户、联系人、任务、日程和跟进记录。",
                """
                当前应用是 CRM 管理。你可以围绕客户、联系人、任务、日程和跟进记录提供帮助。
                只有在工具结果确认成功后，才能说数据已创建、更新或关联成功。
                """,
                false,
                List.of(TOOL_GROUP_CRM, TOOL_GROUP_KNOWLEDGE),
                List.of("把今天新增但还没跟进的客户列出来", "筛选出高意向客户", "总结本周的销售情况")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.KNOWLEDGE,
                "知识库",
                "knowledge",
                "基于知识库文档进行问答、检索和总结。",
                """
                当前应用是知识库问答检索。请优先基于知识库内容回答问题，并在可用时给出参考文件或依据。
                如果知识库中没有相关信息，请如实说明，不要编造结论。
                """,
                true,
                List.of(TOOL_GROUP_KNOWLEDGE),
                List.of("总结选中文档的重点", "这些资料里提到哪些风险？", "合同付款条款是什么？")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.ADDRESS_BOOK,
                "通讯录",
                "customer",
                "围绕员工做任务安排、日程记录和资料归档。",
                """
                当前应用是通讯录员工对象助手。你是在围绕当前员工做工作安排、任务记录、日程记录和资料归档。
                当用户说“他/她/这个员工/该员工”且当前会话绑定了员工时，默认指当前员工。
                """,
                false,
                List.of(TOOL_GROUP_CRM, TOOL_GROUP_KNOWLEDGE),
                List.of("明天下午让他完成客户资料整理", "下周一上午和他开项目复盘会")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.RELATION,
                "关系",
                "contacts",
                "围绕外部关系人创建任务、日程和历史记录。",
                """
                当前应用是关系人对象助手。你是在围绕当前关系人做任务、日程、历史记录和资料归档。
                当用户说“他/她/这个人/该关系人”且当前会话绑定了关系人时，默认指当前关系人。
                """,
                false,
                List.of(TOOL_GROUP_CRM, TOOL_GROUP_KNOWLEDGE),
                List.of("下周三提醒我去拜访他", "记录一下他对新产品比较感兴趣")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.PRODUCT,
                "产品",
                "box",
                "围绕产品资料库进行查询、创建、更新和停用。",
                """
                当前应用是产品助手。你可以围绕产品资料库提供帮助，包括查询产品、创建产品、更新产品资料和停用产品。
                当用户说“这个产品”“当前产品”“该产品”且当前会话绑定了产品时，默认使用当前产品。
                """,
                false,
                List.of(TOOL_GROUP_CRM, TOOL_GROUP_KNOWLEDGE),
                List.of("列出启用中的产品", "帮我新建一个产品", "更新这个产品的标准价")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.PROJECT,
                "项目",
                "task",
                "管理项目、项目泳道和项目任务。",
                """
                当前应用是项目助手。你可以围绕项目和项目任务提供帮助。
                当用户要求管理项目或项目任务时，优先使用项目工具，不要把项目任务误当作普通个人任务。
                """,
                false,
                List.of(TOOL_GROUP_CRM, TOOL_GROUP_KNOWLEDGE),
                List.of("列出进行中的项目", "帮我新建一个项目", "给某个项目新增一个任务")
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
        applications.values().forEach(application -> {
            ChatAppOptionVO option = new ChatAppOptionVO();
            option.setCode(application.code());
            option.setLabel(application.label());
            option.setIconName(application.iconName());
            option.setDescription(application.description());
            option.setDefaultRagEnabled(application.defaultRagEnabled());
            option.setRecommendedQuestions(application.recommendedQuestions());
            options.add(option);
        });
        return options;
    }

    private void register(ChatApplicationDefinition application) {
        applications.put(application.code(), application);
    }
}
