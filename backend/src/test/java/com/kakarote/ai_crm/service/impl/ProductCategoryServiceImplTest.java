package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.ProductCategoryAddBO;
import com.kakarote.ai_crm.entity.PO.ProductCategory;
import com.kakarote.ai_crm.mapper.ProductCategoryMapper;
import com.kakarote.ai_crm.mapper.ProductMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceImplTest {

    @Mock
    private ProductCategoryMapper productCategoryMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductCategoryServiceImpl productCategoryService;

    @BeforeEach
    void setUp() {
        AiContextHolder.bindThreadContext(100L, 1L);
        ReflectionTestUtils.setField(productCategoryService, "baseMapper", productCategoryMapper);
    }

    @AfterEach
    void tearDown() {
        AiContextHolder.clearThreadContext();
    }

    @Test
    void ensureDefaultCategoryCreatesUncategorizedWhenMissing() {
        when(productCategoryMapper.selectOne(any(), eq(false))).thenReturn(null);

        productCategoryService.ensureDefaultCategoryId();

        ArgumentCaptor<ProductCategory> captor = ArgumentCaptor.forClass(ProductCategory.class);
        verify(productCategoryMapper).insert(captor.capture());
        ProductCategory saved = captor.getValue();
        assertThat(saved.getCategoryName()).isEqualTo("未分类");
        assertThat(saved.getParentId()).isZero();
        assertThat(saved.getLevel()).isEqualTo(1);
    }

    @Test
    void addCategoryRejectsMoreThanThreeLevels() {
        ProductCategory parent = new ProductCategory();
        parent.setCategoryId(30L);
        parent.setCategoryName("Level 3");
        parent.setLevel(3);
        parent.setDelFlag(0);
        when(productCategoryMapper.selectById(30L)).thenReturn(parent);

        ProductCategoryAddBO bo = new ProductCategoryAddBO();
        bo.setParentId(30L);
        bo.setCategoryName("Level 4");

        assertThatThrownBy(() -> productCategoryService.addCategory(bo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("三级");

        verify(productCategoryMapper, never()).insert(any(ProductCategory.class));
    }

    @Test
    void deleteCategoryRejectsBoundProducts() {
        ProductCategory category = new ProductCategory();
        category.setCategoryId(20L);
        category.setCategoryName("Software");
        category.setDelFlag(0);
        when(productCategoryMapper.selectById(20L)).thenReturn(category);
        when(productCategoryMapper.selectCount(any())).thenReturn(0L);
        when(productMapper.countByCategoryIdIgnoreDataPermission(20L)).thenReturn(1L);

        assertThatThrownBy(() -> productCategoryService.deleteCategory(20L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("产品");
    }
}
