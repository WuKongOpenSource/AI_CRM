package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "项目详情")
public class ProjectVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;
    private String name;
    private String description;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long customerId;
    private String customerName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerId;
    private String ownerName;
    private Date startDate;
    private Date dueDate;
    private String status;
    private Date createTime;
    private Date updateTime;
    private Integer taskCount;
    private Integer incompleteTaskCount;
    private List<ProjectLaneVO> lanes = new ArrayList<>();
    private List<ProjectTaskVO> tasks = new ArrayList<>();
    private List<ProjectAttachmentVO> attachments = new ArrayList<>();
    private List<ProjectScheduleVO> schedules = new ArrayList<>();
    private List<ProjectChatMessageVO> chatMessages = new ArrayList<>();
    private List<ProjectMemberVO> members = new ArrayList<>();
    private List<ProjectMemberLogVO> memberLogs = new ArrayList<>();
    private List<String> currentUserPermissions = new ArrayList<>();
    private String currentUserRole;
    private Boolean systemAdmin;

    @Data
    public static class ProjectLaneVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long laneId;
        private String name;
        private Integer order;
        private Boolean system;
    }

    @Data
    public static class ProjectTaskVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long taskId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long projectId;
        private String title;
        private String description;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long laneId;
        private String status;
        private Date dueDate;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long ownerId;
        private String ownerName;
        @JsonSerialize(contentUsing = ToStringSerializer.class)
        private List<Long> participantIds = new ArrayList<>();
        private List<String> participantNames = new ArrayList<>();
        private String priority;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long customerId;
        private String customerName;
        private Boolean hasAttachments;
        private Boolean hasSchedule;
        private Boolean generatedByAi;
        private String source;
        private String aiSourceText;
        private List<ProjectTaskAttachmentVO> attachments = new ArrayList<>();
        private List<ProjectTaskScheduleVO> schedules = new ArrayList<>();
        private List<ProjectTaskNoteVO> notes = new ArrayList<>();
        private List<ProjectTaskChatMessageVO> chatMessages = new ArrayList<>();
        private Date createTime;
        private Date updateTime;
    }

    @Data
    public static class ProjectTaskAttachmentVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long attachmentId;
        private String name;
        private Date createTime;
        private String createdByName;
    }

    @Data
    public static class ProjectTaskScheduleVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long scheduleId;
        private String title;
        private Date scheduleTime;
        private Date createTime;
        private String createdByName;
    }

    @Data
    public static class ProjectTaskNoteVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long noteId;
        private String content;
        private Date createTime;
        private String createdByName;
    }

    @Data
    public static class ProjectAttachmentVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long attachmentId;
        private String name;
        private String fileUrl;
        private Date createTime;
        private String createdByName;
    }

    @Data
    public static class ProjectScheduleVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long scheduleId;
        private String title;
        private Date scheduleTime;
        private Date createTime;
        private String createdByName;
    }

    @Data
    public static class ProjectChatMessageVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long messageId;
        private String role;
        private String content;
        private Date createTime;
    }

    @Data
    public static class ProjectTaskChatMessageVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long messageId;
        private String role;
        private String content;
        private Date createTime;
    }

    @Data
    public static class ProjectMemberVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long memberId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long userId;
        private String memberName;
        private String account;
        private String role;
        private String deptName;
        private Date joinedAt;
        private Date lastActionTime;
        private String status;
        private List<String> permissions = new ArrayList<>();
        private String remark;
    }

    @Data
    public static class ProjectMemberLogVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long logId;
        private String actionType;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long operatorId;
        private String operatorName;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long targetUserId;
        private String targetUserName;
        private String beforeSummary;
        private String afterSummary;
        private Date createTime;
    }

    @Data
    public static class ProjectRolePermissionConfigVO {
        private Map<String, List<String>> rolePermissions = new LinkedHashMap<>();
    }
}
