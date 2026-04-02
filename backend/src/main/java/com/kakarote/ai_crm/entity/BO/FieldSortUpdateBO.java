package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户字段排序更新请求
 */
@Data
@Schema(name = "FieldSortUpdateBO", description = "用户字段排序更新请求")
public class FieldSortUpdateBO {

    @NotBlank(message = "实体类型不能为空")
    @Schema(description = "实体类型: customer / contact")
    private String entityType;

    @NotNull(message = "排序项不能为空")
    @Schema(description = "排序项列表")
    private List<FieldSortItem> items;

    @Data
    @Schema(description = "单个字段的排序配置")
    public static class FieldSortItem {

        @NotNull(message = "字段ID不能为空")
        @Schema(description = "字段ID")
        private Long fieldId;

        @NotNull(message = "排序序号不能为空")
        @Schema(description = "排序序号")
        private Integer sortOrder;

        @Schema(description = "是否隐藏，默认false")
        private Boolean hidden;
    }
}
