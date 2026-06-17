package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.RelationAddBO;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.BO.RelationUpdateBO;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.RelationVO;
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

    private static final int DEFAULT_QUERY_LIMIT = 10;

    @Autowired
    private IRelationService relationService;

    @Tool(description = "查询关系人列表。当用户在“关系”技能下要查找、搜索、查看关系人时调用。可按姓名、手机号、微信、邮箱或备注关键词搜索。")
    public String queryRelations(
            @ToolParam(description = "搜索关键词，可匹配姓名、手机号、微信、邮箱或备注", required = false) String keyword,
            @ToolParam(description = "关系类型，例如 decision_maker/influencer/partner/customer_contact/other", required = false) String relationType,
            @ToolParam(description = "返回数量，默认 10，最多 20", required = false) String limitStr) {
        try {
            RelationQueryBO queryBO = new RelationQueryBO();
            queryBO.setKeyword(trimToNull(keyword));
            queryBO.setRelationType(trimToNull(relationType));
            queryBO.setPage(1);
            queryBO.setLimit(resolveLimit(limitStr));
            BasePage<RelationVO> page = relationService.queryPageList(queryBO);
            if (page.getRecords().isEmpty()) {
                return "未找到匹配的关系人。";
            }
            StringBuilder sb = new StringBuilder("找到 ").append(page.getTotal()).append(" 个匹配关系人：\n");
            for (RelationVO relation : page.getRecords()) {
                sb.append("- relationId=").append(relation.getRelationId())
                        .append(", 姓名=").append(StrUtil.blankToDefault(relation.getName(), "未命名关系人"));
                appendInline(sb, "类型", StrUtil.blankToDefault(relation.getRelationTypeName(), relation.getRelationType()));
                appendInline(sb, "手机号", relation.getPhone());
                appendInline(sb, "微信", relation.getWechat());
                appendInline(sb, "邮箱", relation.getEmail());
                appendInline(sb, "关联客户", relation.getCustomerName());
                sb.append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("【Tool调用】queryRelations 失败: {}", e.getMessage(), e);
            return "查询关系人失败: " + e.getMessage();
        }
    }

    @Tool(description = "创建关系人。当用户在“关系”技能下明确要求新增、创建、记录一个关系人时调用。姓名必填；手机号、微信、邮箱、关系类型、关联客户和备注可选。")
    public String createRelation(
            @ToolParam(description = "关系人姓名，必填") String name,
            @ToolParam(description = "手机号", required = false) String phone,
            @ToolParam(description = "微信号", required = false) String wechat,
            @ToolParam(description = "邮箱", required = false) String email,
            @ToolParam(description = "关系类型：decision_maker/influencer/partner/customer_contact/other，默认 other", required = false) String relationType,
            @ToolParam(description = "关联客户ID，数字类型；留空时如果当前对话绑定客户则自动使用当前客户", required = false) String customerIdStr,
            @ToolParam(description = "备注", required = false) String remark) {
        if (StrUtil.isBlank(name) || "null".equalsIgnoreCase(name.trim())) {
            return "创建关系人失败: 姓名不能为空。";
        }
        try {
            RelationAddBO addBO = new RelationAddBO();
            addBO.setName(name.trim());
            addBO.setPhone(trimToNull(phone));
            addBO.setWechat(trimToNull(wechat));
            addBO.setEmail(trimToNull(email));
            addBO.setRelationType(StrUtil.blankToDefault(trimToNull(relationType), "other"));
            addBO.setCustomerId(resolveCustomerId(customerIdStr));
            addBO.setRemark(trimToNull(remark));
            Long relationId = relationService.addRelation(addBO);
            RelationVO relation = relationService.getOwnedRelationVO(relationId);
            return "关系人创建成功。\n"
                    + "- relationId: " + relationId + "\n"
                    + "- 姓名: " + StrUtil.blankToDefault(relation.getName(), addBO.getName()) + "\n"
                    + "- 类型: " + StrUtil.blankToDefault(relation.getRelationTypeName(), relation.getRelationType());
        } catch (Exception e) {
            log.error("【Tool调用】createRelation 失败: {}", e.getMessage(), e);
            return "创建关系人失败: " + e.getMessage();
        }
    }

    @Tool(description = "更新当前关系人的备注。只有当用户明确要求更新备注、写入备注、改备注时调用；普通“记录一下”应调用 createFollowUp 创建历史记录。")
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

    private Long resolveCustomerId(String customerIdStr) {
        if (StrUtil.isNotBlank(customerIdStr) && !"null".equalsIgnoreCase(customerIdStr.trim())) {
            return Long.parseLong(customerIdStr.trim());
        }
        return AiContextHolder.getCurrentCustomerId();
    }

    private int resolveLimit(String limitStr) {
        if (StrUtil.isBlank(limitStr)) {
            return DEFAULT_QUERY_LIMIT;
        }
        try {
            return Math.max(1, Math.min(20, Integer.parseInt(limitStr.trim())));
        } catch (NumberFormatException ignored) {
            return DEFAULT_QUERY_LIMIT;
        }
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) || "null".equalsIgnoreCase(value.trim()) ? null : value.trim();
    }

    private void appendInline(StringBuilder builder, String label, String value) {
        if (StrUtil.isNotBlank(value)) {
            builder.append(", ").append(label).append("=").append(value);
        }
    }
}
