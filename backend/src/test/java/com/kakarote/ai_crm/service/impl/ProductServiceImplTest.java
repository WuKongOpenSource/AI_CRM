package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.ProductAddBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Product;
import com.kakarote.ai_crm.entity.PO.ProductCategory;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ProductMapper;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.IProductCategoryService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private IProductCategoryService productCategoryService;

    @Mock
    private ISystemConfigService systemConfigService;

    @Mock
    private ICustomFieldService customFieldService;

    @Mock
    private IGlobalSearchIndexService globalSearchIndexService;

    @Mock
    private ManageUserMapper manageUserMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        AiContextHolder.bindThreadContext(100L, 1L);
        ReflectionTestUtils.setField(productService, "baseMapper", productMapper);
    }

    @AfterEach
    void tearDown() {
        AiContextHolder.clearThreadContext();
    }

    @Test
    void addProductRejectsMissingCodeWhenCodeIsRequired() {
        when(systemConfigService.getConfigValue("product.code.required", "true")).thenReturn("true");

        ProductAddBO bo = new ProductAddBO();
        bo.setProductName("Starter Plan");
        bo.setOwnerId(100L);

        assertThatThrownBy(() -> productService.addProduct(bo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("产品编码");

        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void addProductAllowsMissingCodeWhenCodeIsOptionalAndUsesDefaultCategory() {
        when(systemConfigService.getConfigValue("product.code.required", "true")).thenReturn("false");
        when(productCategoryService.ensureDefaultCategoryId()).thenReturn(10L);
        ProductCategory category = new ProductCategory();
        category.setCategoryId(10L);
        category.setLevel(1);
        category.setDelFlag(0);
        when(productCategoryService.getById(10L)).thenReturn(category);
        ManagerUser owner = new ManagerUser();
        owner.setUserId(100L);
        owner.setRealname("Owner");
        when(manageUserMapper.getUserId(100L)).thenReturn(owner);

        ProductAddBO bo = new ProductAddBO();
        bo.setProductName("Starter Plan");
        bo.setOwnerId(100L);
        bo.setStandardPrice(new BigDecimal("99.00"));

        productService.addProduct(bo);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductCode()).isNull();
        assertThat(saved.getCategoryId()).isEqualTo(10L);
        assertThat(saved.getStatus()).isEqualTo("active");
        assertThat(saved.getOwnerId()).isEqualTo(100L);
    }

    @Test
    void addProductRejectsDuplicateCodeWithinTenant() {
        when(systemConfigService.getConfigValue("product.code.required", "true")).thenReturn("true");
        when(productMapper.countByCodeIgnoreDataPermission("SKU-001", null)).thenReturn(1L);

        ProductAddBO bo = new ProductAddBO();
        bo.setProductName("Starter Plan");
        bo.setProductCode("SKU-001");
        bo.setOwnerId(100L);

        assertThatThrownBy(() -> productService.addProduct(bo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("产品编码");

        verify(productMapper, never()).insert(any(Product.class));
    }
}
