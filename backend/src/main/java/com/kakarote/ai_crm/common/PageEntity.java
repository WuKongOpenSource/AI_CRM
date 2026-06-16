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

    /**
     * 初始化分页Entity实例。
     */
    public PageEntity() {
        this.page = 1;
        this.limit = 15;
    }

    /**
     * 初始化分页Entity实例。
     */
    public PageEntity(Integer page, Integer limit) {
        this.page = page;
        this.limit = limit;
    }

    /**
     * 获取分页。
     */
    public Integer getPage() {
        return page != null ? page : 1;
    }

    /**
     * 设置分页。
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * 获取限制。
     */
    public Integer getLimit() {
        if (limit == null) {
            return 15;
        }
        if (limit > Const.MAX_QUERY_SIZE) {
            limit = Const.MAX_QUERY_SIZE;
        }
        return limit;
    }

    /**
     * 设置限制。
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * 解析分页Entity。
     */
    public <T> BasePage<T> parse() {
        BasePage<T> page = new BasePage<>(getPage(), getLimit());
        return page;
    }
}
