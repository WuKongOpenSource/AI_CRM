package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.entity.BO.RelationUpdateBO;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.service.IRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 关系人相关 AI Tool。
 */
@Slf4j
@Component
public class RelationTools {

    @Autowired
    private IRelationService relationService;

    @Tool(description = "更新当前关系人的备注。只有当用户明确要求更新备注、写入备注、改备注时调用；普通“记录一下”应调用 createFollowUp 创建历史记录。")
    @AiToolPermission(value = "relation:edit", action = "更新关系人备注")
    public String updateCurrentRelationRemark(
            @ToolParam(description = "要写入备注的内容，必填") String remark,
            @ToolParam(description = "更新模式：append(追加)/replace(替换)，默认append", required = false) String mode) {
        Long relationId = AiContextHolder.getCurrentRelationId();
        if (relationId == null) {
            return "更新关系人备注失败: 当前对话未绑定关系人。";
        }
        if (StrUtil.isBlank(remark) || "null".equalsIgnoreCase(remark.trim())) {
            return "更新关系人备注失败: 备注内容不能为空。";
        }

        try {
            Relation relation = relationService.getOwnedRelation(relationId);
            String nextRemark = remark.trim();
            if (!"replace".equalsIgnoreCase(StrUtil.trimToEmpty(mode)) && StrUtil.isNotBlank(relation.getRemark())) {
                nextRemark = relation.getRemark().trim() + "\n" + nextRemark;
            }

            RelationUpdateBO updateBO = new RelationUpdateBO();
            updateBO.setRelationId(relation.getRelationId());
            updateBO.setName(relation.getName());
            updateBO.setAvatar(relation.getAvatar());
            updateBO.setPhone(relation.getPhone());
            updateBO.setWechat(relation.getWechat());
            updateBO.setEmail(relation.getEmail());
            updateBO.setRelationType(relation.getRelationType());
            updateBO.setCustomerId(relation.getCustomerId());
            updateBO.setRemark(nextRemark);
            relationService.updateRelation(updateBO);

            return "关系人备注更新成功。\n- relationId: " + relationId + "\n- 备注: " + nextRemark;
        } catch (Exception e) {
            log.error("【Tool调用】updateCurrentRelationRemark 失败: {}", e.getMessage(), e);
            return "更新关系人备注失败: " + e.getMessage();
        }
    }
}
