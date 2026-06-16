package com.kakarote.ai_crm.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Lets the model explicitly choose "no CRM business tool needed" when CRM tool
 * calling is required for a turn.
 */
@Component
public class CrmNoopTools {

    @Tool(description = "当用户只是咨询、解释、闲聊、讨论操作方法，或问题不需要读取/创建/更新/删除 CRM 业务数据时调用。用户要求创建、新增、添加、记录、安排、更新、修改、删除客户、联系人、任务、日程或跟进记录时，禁止调用此工具，必须选择对应 CRM 业务工具。")
    public String answerDirectly(
            @ToolParam(description = "为什么本轮不需要读取或写入 CRM 业务数据", required = false) String reason) {
        if (reason == null || reason.isBlank()) {
            return "本轮不需要调用 CRM 业务数据工具，请直接回答用户。";
        }
        return "本轮不需要调用 CRM 业务数据工具，请直接回答用户。原因：" + reason;
    }
}
