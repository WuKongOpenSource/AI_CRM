package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 企业搜索BO
 * @author zhangzhiwei
 */
@Schema(description = "企业搜索BO")
@Getter
@Setter
public class SearchBO extends PageEntity {

    /**
     * 搜索内容
     */
    @Schema(description = "")
    private String search;

    @Schema(description = "类型")
    private String type;

    @Override
    public String toString() {
        return "CompanySearchBO{" +
                "search='" + search + '\'' +
                '}';
    }
}
