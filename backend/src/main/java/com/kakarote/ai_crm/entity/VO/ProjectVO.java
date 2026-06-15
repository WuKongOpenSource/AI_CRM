package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
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

    @Data
    public static class ProjectLaneVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long laneId;
        private String name;
        private String code;
        private Integer sortOrder;
        private Boolean system;
    }

    @Data
    public static class ProjectTaskVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long taskId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long projectId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long laneId;
        private String title;
        private String description;
        private String status;
        private Date dueDate;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long ownerId;
        private String ownerName;
        private String priority;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long customerId;
        private String customerName;
        private Boolean generatedByAi;
        private String aiSourceText;
        private Date createTime;
        private Date updateTime;
    }
}
