package com.kakarote.ai_crm.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 默认分页工具类
 *
 * @author zhangzhiwei
 */
@Schema(description = "默认分页工具类")
public class PageEntity implements Serializable {

    @Schema(description = "当前页数")
    private Integer page;

    @Schema(description = "每页查询条数")
    private Integer limit;

    public PageEntity() {
        this.page = 1;
        this.limit = 15;
    }

    public PageEntity(Integer page, Integer limit) {
        this.page = page;
        this.limit = limit;
    }

    public Integer getPage() {
        return page != null ? page : 1;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        if (limit == null) {
            return 15;
        }
        if (limit > Const.MAX_QUERY_SIZE) {
            limit = Const.MAX_QUERY_SIZE;
        }
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public <T> BasePage<T> parse() {
        BasePage<T> page = new BasePage<>(getPage(), getLimit());
        return page;
    }
}
