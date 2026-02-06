package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 枚举视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EnumVO", description = "枚举视图对象")
public class EnumVO {

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "颜色")
    private String color;

    /**
     * 两参数构造函数
     */
    public EnumVO(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
