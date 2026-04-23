package com.kakarote.ai_crm.common;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhangzhiwei
 * 默认分页
 */
public class BasePage<T> implements IPage<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 8545996863226528798L;

    /**
     * 查询数据列表
     */
    @Getter
    private List<T> list = new ArrayList<>();
    /**
     * 总数
     */
    @Getter
    private long totalRow = 0;

    /**
     * 每页显示条数，默认 15
     */
    @Getter
    private long pageSize = 15;

    /**
     * 当前页
     */
    private long pageNumber = 1;

    /**
     * 排序字段信息
     */
    private final List<OrderItem> orders = new ArrayList<>();

    private boolean searchCount = true;

    private boolean optimizeCountSql = true;

    private boolean optimizeJoinOfCountSql = true;

    private String countId;

    /**
     * 额外数据
     */
    @Getter
    @Setter
    private Object extraData;


    public BasePage() {

    }

    /**
     * 分页构造函数
     *
     * @param current 当前页
     * @param size    每页显示条数
     */
    public BasePage(long current, long size) {
        this(current, size, 0);
    }

    public BasePage(long current, long size, long total) {
        if (current > 1) {
            this.pageNumber = current;
        }
        this.pageSize = size;
        this.totalRow = total;
    }


    @Override
    @JsonIgnore
    public List<T> getRecords() {
        return this.list;
    }

    public long getTotalPage() {
        if (getSize() == 0) {
            return 0L;
        }
        long pages = getTotal() / getSize();
        if (getTotal() % getSize() != 0) {
            pages++;
        }
        return pages;
    }


    public boolean isFirstPage() {
        return this.pageNumber == 1L;
    }

    public boolean isLastPage() {
        return getTotal() == 0 || this.pageNumber == getTotalPage();
    }

    public void setPageNumber(long pageNumber) {
        this.pageNumber = pageNumber;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    @Override
    public BasePage<T> setRecords(List<T> records) {
        this.list = records;
        return this;
    }

    @Override
    @JsonIgnore
    public long getTotal() {
        return this.totalRow;
    }

    @Override
    public BasePage<T> setTotal(long total) {
        this.totalRow = total;
        return this;
    }

    @Override
    @JsonIgnore
    public long getSize() {
        return this.pageSize;
    }

    @Override
    public BasePage<T> setSize(long size) {
        this.pageSize = size;
        return this;
    }

    @Override
    @JsonIgnore
    public long getCurrent() {
        return this.pageNumber;
    }

    @Override
    public BasePage<T> setCurrent(long current) {
        this.pageNumber = current;
        return this;
    }


    /**
     * 添加新的排序条件
     *
     * @param items 条件
     * @return 返回分页参数本身
     */
    public BasePage<T> addOrder(OrderItem... items) {
        orders.addAll(Arrays.asList(items));
        return this;
    }

    @Override
    public List<OrderItem> orders() {
        return orders;
    }

    @Override
    @JsonIgnore
    public boolean searchCount() {
        return searchCount;
    }

    public BasePage<T> setSearchCount(boolean searchCount) {
        this.searchCount = searchCount;
        return this;
    }

    @Override
    @JsonIgnore
    public boolean optimizeCountSql() {
        return optimizeCountSql;
    }

    public BasePage<T> setOptimizeCountSql(boolean optimizeCountSql) {
        this.optimizeCountSql = optimizeCountSql;
        return this;
    }

    @Override
    @JsonIgnore
    public boolean optimizeJoinOfCountSql() {
        return optimizeJoinOfCountSql;
    }

    public BasePage<T> setOptimizeJoinOfCountSql(boolean optimizeJoinOfCountSql) {
        this.optimizeJoinOfCountSql = optimizeJoinOfCountSql;
        return this;
    }

    @Override
    @JsonIgnore
    public String countId() {
        return countId;
    }

    public BasePage<T> setCountId(String countId) {
        this.countId = countId;
        return this;
    }


    /**
     * 类型转换 通过beanCopy
     *
     * @param clazz 转换后的类
     * @param <R>   R
     * @return BasePage
     */
    public <R> BasePage<R> copy(Class<R> clazz) {
        return copy(clazz, obj -> BeanUtil.copyProperties(obj, clazz));
    }

    /**
     * 类型转换 通过beanCopy
     *
     * @param clazz 转换后的类
     * @param <R>   R
     * @return BasePage
     */
    public <R> BasePage<R> copy(Class<R> clazz, Function<? super T, ? extends R> mapper) {
        BasePage<R> basePage = new BasePage<>(getCurrent(), getSize(), getTotal());
        basePage.setRecords(getRecords().stream().map(mapper).collect(Collectors.toList()));
        return basePage;
    }

    @Override
    @JsonIgnore
    public long getPages() {
        return getTotalPage();
    }
}
