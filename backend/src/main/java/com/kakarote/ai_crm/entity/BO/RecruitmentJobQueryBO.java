package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RecruitmentJobQueryBO", description = "招聘岗位查询")
public class RecruitmentJobQueryBO extends PageEntity {

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "负责人ID")
    private Long ownerId;
}
