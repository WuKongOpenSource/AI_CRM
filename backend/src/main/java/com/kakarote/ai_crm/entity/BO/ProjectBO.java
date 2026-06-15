package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

public class ProjectBO {

    @Data
    public static class Query extends PageEntity {
        private String keyword;
        private String status;
    }

    @Data
    public static class Create {
        @NotBlank(message = "Project name is required")
        private String name;
        private String description;
        private Long customerId;
        private String customerName;
        private Long ownerId;
        private Date startDate;
        private Date dueDate;
        private String status;
    }

    @Data
    public static class Update extends Create {
        @NotNull(message = "Project ID is required")
        private Long projectId;
    }

    @Data
    public static class LaneSave {
        private Long laneId;

        @NotBlank(message = "Lane name is required")
        private String name;
    }

    @Data
    public static class TaskSave {
        private Long taskId;

        @NotBlank(message = "Task title is required")
        private String title;

        private String description;
        private Long laneId;
        private Date dueDate;
        private Long ownerId;
        private String ownerName;
        private String priority;
        private Long customerId;
        private String customerName;
        private Boolean generatedByAi;
        private String aiSourceText;
    }

    @Data
    public static class TaskMove {
        @NotNull(message = "Task ID is required")
        private Long taskId;

        @NotNull(message = "Lane ID is required")
        private Long laneId;
    }
}
