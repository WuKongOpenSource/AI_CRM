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
    public static final String TOOL_GROUP_CUSTOMER = "customer";
    public static final String TOOL_GROUP_CONTACT = "contact";
    public static final String TOOL_GROUP_FOLLOW_UP = "followup";
    public static final String TOOL_GROUP_TASK_SCHEDULE = "task_schedule";
    public static final String TOOL_GROUP_MAIL = "mail";
    public static final String TOOL_GROUP_PRODUCT = "product";
    public static final String TOOL_GROUP_PROJECT = "project";
    public static final String TOOL_GROUP_RELATION = "relation";
    public static final String TOOL_GROUP_KNOWLEDGE = "knowledge";
    public static final String TOOL_GROUP_CRM_NOOP = "crm_noop";
    public static final String TOOL_GROUP_EMPLOYEE = "employee";
    public static final String TOOL_GROUP_EMPLOYEE_LOOKUP = "employee_lookup";

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
                如果用户需要查看或管理客户、联系人、普通任务、日程或跟进记录，回复《请先在下方输入框左下角点击+号，勾选“悟空技能”中的“CRM管理”》。
                如果用户需要查看、创建、更新、停用或管理产品、产品资料库、产品类目，或提到“创建某某产品/新增产品/修改产品/停用产品”，回复《目前无法直接创建或修改产品，因为尚未启用产品技能。请在下方输入框左下角点击+号，勾选“悟空技能”中的“产品”，然后再发送需求。》。
                如果用户需要查看、创建或管理项目、项目泳道、项目任务，或要求“在某个项目下创建任务”，回复《目前无法直接创建或修改项目/项目任务，因为尚未启用项目技能。请在下方输入框左下角点击+号，勾选“悟空技能”中的“项目”，然后再发送需求。》。
                如果用户需要基于文档或知识库回答，引导其选择知识库应用。
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
                List.of(TOOL_GROUP_CUSTOMER, TOOL_GROUP_CONTACT, TOOL_GROUP_FOLLOW_UP,
                        TOOL_GROUP_TASK_SCHEDULE, TOOL_GROUP_MAIL, TOOL_GROUP_KNOWLEDGE,
                        TOOL_GROUP_CRM_NOOP),
                List.of("把今天新增但还没跟进的客户列出来", "找出快丢单的客户", "筛选出高意向客户", "总结本周的销售情况")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.PRODUCT,
                "产品",
                "box",
                "围绕产品资料库进行查询、创建、更新、停用，并可参考知识库内容。",
                """
                当前应用是产品助手。你可以围绕产品资料库提供帮助，包括查询产品、创建产品、更新产品资料、停用产品，以及基于知识库内容回答产品相关问题。
                当用户说“这个产品”“当前产品”“该产品”且当前会话绑定了产品时，默认使用当前 productId。
                新增或修改产品必须调用 ProductTools，并且只有工具结果确认成功后才能回复创建、更新或停用成功。
                """,
                false,
                List.of(TOOL_GROUP_PRODUCT, TOOL_GROUP_KNOWLEDGE),
                List.of("列出启用中的产品", "帮我新建一个产品", "更新这个产品的标准价", "停用当前产品")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.PROJECT,
                "项目",
                "task",
                "管理项目、项目泳道和项目任务，并可参考项目关联客户与知识库内容。",
                """
                当前应用是项目助手。你可以围绕项目和项目任务提供帮助，包括查询项目、创建项目、更新项目、删除项目，以及创建、更新、移动、删除项目内任务。
                当用户要求管理项目或项目任务时，优先调用 ProjectTools 中的项目工具，不要把项目任务误当作普通个人任务。
                创建或修改项目、项目任务后，只有在工具结果确认成功后，才能说数据已创建、更新、移动或删除成功。
                如果用户只说“任务”但上下文明确是在项目技能中，请先判断是否指项目任务；缺少项目ID或任务ID且无法从上下文确认时，请先询问。
                当需要按人名给项目或任务指派负责人时，先调用 searchEmployees 在通讯录中按姓名模糊查找，拿到对应的用户ID再作为 ownerIdStr 传入；不要凭空编造用户ID。若一个名字匹配到多个人或无法确定，请把候选列表给用户确认后再指派。
                """,
                false,
                List.of(TOOL_GROUP_PROJECT, TOOL_GROUP_KNOWLEDGE, TOOL_GROUP_EMPLOYEE_LOOKUP),
                List.of("列出进行中的项目", "帮我新建一个项目", "查看这个项目的任务", "给某个项目新增一个任务")
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
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.ADDRESS_BOOK,
                "通讯录",
                "customer",
                "围绕企业员工做任务安排、日程记录、附件归档和知识库检索，并支持上传名单批量导入员工。",
                """
                当前应用是通讯录员工对象助手。你是在围绕当前员工做工作安排、任务记录、日程记录和附件归档，不是在给员工发送即时消息。
                当用户说“他/她/这个员工/该员工”且当前会话绑定了员工时，默认指当前员工。
                没有具体执行时间点、只有截止或待办语义时创建任务；出现具体执行时间点时创建日程。
                创建任务时默认负责人是当前员工；创建日程时默认把当前员工加入参与人。
                当用户想新增或批量添加员工到通讯录时（无论是上传文件、在对话中粘贴名单文本，还是直接说出一个或多个员工的信息）：
                你必须调用 previewEmployeeImport 工具来生成预览——把识别到的员工整理成 JSON 数组作为该工具的参数传入。
                禁止你自己整理或直接以表格、列表等形式输出员工名单当作预览；预览内容只能来自 previewEmployeeImport 工具的返回结果，你把工具返回的内容原样转述给用户即可。
                随后询问用户是否确认导入；用户确认后你必须调用 confirmEmployeeImport 才算真正建号，用户放弃则调用 cancelEmployeeImport。
                只有当内容中确实没有任何可识别的员工信息时，才说明无法识别并且不调用导入工具。未填密码的员工将使用默认密码 123456。
                只有在工具结果确认成功后，才能说数据已创建、更新或关联成功。
                """,
                false,
                List.of(TOOL_GROUP_TASK_SCHEDULE, TOOL_GROUP_KNOWLEDGE, TOOL_GROUP_EMPLOYEE, TOOL_GROUP_EMPLOYEE_LOOKUP),
                List.of("上传名单帮我批量添加员工", "明天下午让他完成客户资料整理", "下周一上午和他开项目复盘会", "总结这个员工最近的任务和附件")
        ));
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.RELATION,
                "关系",
                "contacts",
                "围绕外部关系人创建任务、日程、历史记录、附件归档和知识库检索。",
                """
                当前应用是关系人对象助手。你是在围绕当前关系人做任务、日程、历史记录和附件归档。
                当用户说“他/她/这个人/这个关系人/该关系人”且当前会话绑定了关系人时，默认指当前关系人。
                没有具体执行时间点、只有截止或待办语义时创建任务；出现具体执行时间点时创建日程。
                当用户说“记录一下”“帮我记一下”或描述已经发生的偏好、兴趣、沟通结论时，默认调用 createFollowUp 创建历史记录，不要直接修改备注；只有用户明确要求“更新备注/改备注/写到备注”时才更新备注。
                关系人附件默认归档到该关系人的知识库资料。
                只有在工具结果确认成功后，才能说数据已创建、更新或关联成功。
                """,
                false,
                List.of(TOOL_GROUP_RELATION, TOOL_GROUP_FOLLOW_UP,
                        TOOL_GROUP_TASK_SCHEDULE, TOOL_GROUP_KNOWLEDGE),
                List.of("下周三提醒我去拜访他", "记录一下，他对我们的新产品比较感兴趣", "总结这个关系人的任务和附件")
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
