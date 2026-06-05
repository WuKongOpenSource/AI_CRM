package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ProjectBO {

    @Data
    public static class Query extends PageEntity {
        @Schema(description = "模糊搜索关键词")
        private String keyword;

        @Schema(description = "项目状态")
        private String status;
    }

    @Data
    public static class Create {
        @NotBlank(message = "项目名称不能为空")
        @Schema(description = "项目名称")
        private String name;

        @Schema(description = "项目描述")
        private String description;

        @Schema(description = "关联客户ID")
        private Long customerId;

        @Schema(description = "关联客户名称")
        private String customerName;

        @Schema(description = "项目负责人ID")
        private Long ownerId;

        @Schema(description = "项目负责人姓名")
        private String ownerName;

        @Schema(description = "项目负责人账号")
        private String ownerAccount;

        @Schema(description = "项目负责人部门")
        private String ownerDeptName;

        @Schema(description = "项目开始时间")
        private Date startDate;

        @Schema(description = "项目截止时间")
        private Date dueDate;

        @Schema(description = "项目状态")
        private String status;
    }

    @Data
    public static class Update extends Create {
        @NotNull(message = "项目ID不能为空")
        private Long projectId;
    }

    @Data
    public static class LaneSave {
        private Long laneId;

        @NotBlank(message = "泳道名称不能为空")
        private String name;
    }

    @Data
    public static class TaskSave {
        private Long taskId;

        @NotBlank(message = "任务名称不能为空")
        private String title;

        private String description;
        private Long laneId;
        private Date dueDate;
        private Long ownerId;
        private String ownerName;
        private List<Long> participantIds;
        private List<String> participantNames;
        private String priority;
        private Long customerId;
        private String customerName;
        private Boolean hasAttachments;
        private Boolean hasSchedule;
        private Boolean generatedByAi;
        private String aiSourceText;
    }

    @Data
    public static class TaskMove {
        @NotNull(message = "任务ID不能为空")
        private Long taskId;

        @NotNull(message = "泳道ID不能为空")
        private Long laneId;
    }

    @Data
    public static class MemberSave {
        @NotNull(message = "成员用户不能为空")
        private Long userId;

        @NotBlank(message = "成员姓名不能为空")
        private String memberName;

        @NotBlank(message = "成员账号不能为空")
        private String account;

        @NotBlank(message = "项目角色不能为空")
        private String role;

        private String deptName;
        private List<String> permissions;
        private String remark;
        private String status;
    }

    @Data
    public static class MemberRole {
        @NotNull(message = "成员用户不能为空")
        private Long userId;

        @NotBlank(message = "项目角色不能为空")
        private String role;
    }

    @Data
    public static class MemberPermissions {
        @NotNull(message = "成员用户不能为空")
        private Long userId;

        private List<String> permissions;
    }

    @Data
    public static class MemberStatus {
        @NotNull(message = "成员用户不能为空")
        private Long userId;

        @NotBlank(message = "成员状态不能为空")
        private String status;
    }

    @Data
    public static class AiCommand {
        @NotBlank(message = "对话内容不能为空")
        private String content;
    }

    @Data
    public static class RolePermissionConfig {
        private Map<String, List<String>> rolePermissions;
    }
}
