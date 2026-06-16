package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.entity.BO.FieldSortUpdateBO;
import com.kakarote.ai_crm.entity.PO.CustomFieldSort;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;
import com.kakarote.ai_crm.mapper.CustomFieldSortMapper;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.ICustomFieldSortService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户自定义字段排序服务实现
 */
@Slf4j
@Service
public class CustomFieldSortServiceImpl extends ServiceImpl<CustomFieldSortMapper, CustomFieldSort>
        implements ICustomFieldSortService {

    @Lazy
    @Autowired
    private ICustomFieldService customFieldService;

    /**
     * 获取用户字段配置。
     */
    @Override
    public List<CustomFieldVO> getUserFieldConfig(String entityType) {
        List<CustomFieldVO> allFields = buildMergedConfig(entityType);
        // 过滤掉隐藏字段
        return allFields.stream()
                .filter(f -> !Boolean.TRUE.equals(f.getHidden()))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户全部字段配置。
     */
    @Override
    public List<CustomFieldVO> getUserAllFieldConfig(String entityType) {
        return buildMergedConfig(entityType);
    }

    /**
     * 保存用户字段配置。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserFieldConfig(FieldSortUpdateBO bo) {
        Long userId = UserUtil.getUserId();
        String entityType = bo.getEntityType();

        // 删除当前用户该 entityType 的所有排序记录
        remove(new LambdaQueryWrapper<CustomFieldSort>()
                .eq(CustomFieldSort::getUserId, userId)
                .eq(CustomFieldSort::getEntityType, entityType));

        // 批量插入新记录
        if (bo.getItems() != null && !bo.getItems().isEmpty()) {
            List<CustomFieldSort> sortList = new ArrayList<>();
            for (FieldSortUpdateBO.FieldSortItem item : bo.getItems()) {
                CustomFieldSort sort = new CustomFieldSort();
                sort.setUserId(userId);
                sort.setEntityType(entityType);
                sort.setFieldId(item.getFieldId());
                sort.setSortOrder(item.getSortOrder());
                sort.setIsHidden(Boolean.TRUE.equals(item.getHidden()) ? 1 : 0);
                sortList.add(sort);
            }
            saveBatch(sortList);
        }
    }

    /**
     * 移除按字段ID。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByFieldId(Long fieldId) {
        remove(new LambdaQueryWrapper<CustomFieldSort>()
                .eq(CustomFieldSort::getFieldId, fieldId));
    }

    /**
     * 合并字段定义和用户排序配置
     */
    private List<CustomFieldVO> buildMergedConfig(String entityType) {
        Long userId = UserUtil.getUserId();

        // 1. 获取当前实体所有启用字段
        List<CustomFieldVO> enabledFields = customFieldService.getListFieldsByEntity(entityType);
        if (enabledFields.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取当前用户的排序记录
        List<CustomFieldSort> userSorts = list(new LambdaQueryWrapper<CustomFieldSort>()
                .eq(CustomFieldSort::getUserId, userId)
                .eq(CustomFieldSort::getEntityType, entityType));

        Map<Long, CustomFieldSort> sortMap = userSorts.stream()
                .collect(Collectors.toMap(CustomFieldSort::getFieldId, Function.identity(), (a, b) -> a));

        // 3. 合并：有用户配置的用用户的排序和显隐，没有的用默认值
        for (CustomFieldVO field : enabledFields) {
            CustomFieldSort userSort = sortMap.get(field.getFieldId());
            if (userSort != null) {
                field.setUserSortOrder(userSort.getSortOrder());
                field.setHidden(userSort.getIsHidden() != null && userSort.getIsHidden() == 1);
            } else {
                // 无用户配置：用字段定义的排序，默认显示
                field.setUserSortOrder(field.getSortOrder());
                field.setHidden(false);
            }
        }

        // 4. 按用户排序排列
        enabledFields.sort(Comparator
                .comparingInt((CustomFieldVO f) -> f.getUserSortOrder() != null ? f.getUserSortOrder() : Integer.MAX_VALUE)
                .thenComparing(f -> f.getCreateTime() != null ? f.getCreateTime() : new Date(0)));

        return enabledFields;
    }
}
