package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ProductAddBO;
import com.kakarote.ai_crm.entity.BO.ProductQueryBO;
import com.kakarote.ai_crm.entity.BO.ProductStatusUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductUpdateBO;
import com.kakarote.ai_crm.entity.PO.Product;
import com.kakarote.ai_crm.entity.VO.ProductVO;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IProductCategoryService;
import com.kakarote.ai_crm.service.IProductService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class ProductTools {

    @Autowired
    private IProductService productService;

    @Autowired
    private IProductCategoryService productCategoryService;

    @Autowired
    private ICustomFieldService customFieldService;

    @Tool(description = "查询产品资料库。可按关键词、产品编码、状态查询当前用户有权限查看的产品。")
    @AiToolPermission(value = "product:view", action = "查询产品")
    public String queryProducts(
            @ToolParam(description = "关键词，可匹配产品名称、编码、类型、单位、描述", required = false) String keyword,
            @ToolParam(description = "产品编码，精确或关键词查询均可", required = false) String productCode,
            @ToolParam(description = "状态：enabled/disabled，留空查全部", required = false) String status) {
        try {
            ProductQueryBO queryBO = new ProductQueryBO();
            queryBO.setKeyword(firstNonBlank(productCode, keyword));
            queryBO.setStatus(normalizeStatus(status, false));
            queryBO.setPage(1);
            queryBO.setLimit(10);
            BasePage<ProductVO> page = productService.queryPageList(queryBO);
            List<ProductVO> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                return "未找到匹配的产品。";
            }
            StringBuilder builder = new StringBuilder("找到 ").append(page.getTotal()).append(" 个匹配产品，前 ")
                    .append(records.size()).append(" 个如下：\n");
            for (ProductVO product : records) {
                builder.append("- productId=").append(product.getProductId())
                        .append(", 名称=").append(StrUtil.blankToDefault(product.getProductName(), "未命名产品"));
                appendInline(builder, "编码", product.getProductCode());
                appendInline(builder, "类目", product.getCategoryPath());
                appendInline(builder, "类型", productTypeLabel(product.getProductType()));
                appendInline(builder, "单位", product.getUnit());
                appendInline(builder, "标准价", product.getStandardPrice() == null ? null : product.getStandardPrice().toPlainString());
                appendInline(builder, "状态", productStatusLabel(product.getStatus()));
                appendInline(builder, "负责人", product.getOwnerName());
                builder.append("\n");
            }
            return builder.toString();
        } catch (Exception exception) {
            log.error("[Tool调用] queryProducts failed: {}", exception.getMessage(), exception);
            return "查询产品失败: " + exception.getMessage();
        }
    }

    @Tool(description = "新增产品。默认创建为启用状态；没有提供类目时归入未分类。")
    @AiToolPermission(value = "product:create", action = "新增产品")
    public String createProduct(
            @ToolParam(description = "产品名称，必填") String productName,
            @ToolParam(description = "产品编码；是否必填由产品设置控制", required = false) String productCode,
            @ToolParam(description = "类目路径，例如 硬件/终端/平板；留空归入未分类", required = false) String categoryPath,
            @ToolParam(description = "产品类型", required = false) String productType,
            @ToolParam(description = "单位", required = false) String unit,
            @ToolParam(description = "标准价", required = false) String standardPrice,
            @ToolParam(description = "成本价", required = false) String costPrice,
            @ToolParam(description = "描述", required = false) String description) {
        try {
            if (StrUtil.isBlank(productName)) {
                return "新增产品失败: 产品名称不能为空。";
            }
            ProductAddBO bo = new ProductAddBO();
            bo.setProductName(productName.trim());
            bo.setProductCode(trimToNull(productCode));
            bo.setCategoryId(resolveCategoryId(categoryPath, false));
            bo.setProductType(trimToNull(productType));
            bo.setUnit(trimToNull(unit));
            bo.setStandardPrice(parseMoney(standardPrice, "标准价"));
            bo.setCostPrice(parseMoney(costPrice, "成本价"));
            bo.setDescription(trimToNull(description));
            bo.setOwnerId(UserUtil.getUserIdOrNull());
            Long productId = productService.addProduct(bo);
            Product product = productService.getVisibleProduct(productId);
            return "产品新增成功。\n" + formatProduct(product);
        } catch (Exception exception) {
            log.error("[Tool调用] createProduct failed: {}", exception.getMessage(), exception);
            return "新增产品失败: " + exception.getMessage();
        }
    }

    @Tool(description = "更新产品资料。productIdOrCode 可传产品ID或产品编码；留空时默认更新当前产品会话绑定的产品。空编码产品不能按编码更新。")
    @AiToolPermission(value = "product:edit", action = "更新产品")
    public String updateProduct(
            @ToolParam(description = "产品ID或产品编码；留空默认当前产品", required = false) String productIdOrCode,
            @ToolParam(description = "产品名称", required = false) String productName,
            @ToolParam(description = "产品编码；传空不修改", required = false) String productCode,
            @ToolParam(description = "类目路径，例如 硬件/终端/平板", required = false) String categoryPath,
            @ToolParam(description = "产品类型", required = false) String productType,
            @ToolParam(description = "单位", required = false) String unit,
            @ToolParam(description = "标准价", required = false) String standardPrice,
            @ToolParam(description = "成本价", required = false) String costPrice,
            @ToolParam(description = "描述", required = false) String description) {
        try {
            Product product = resolveProduct(productIdOrCode);
            if (product == null) {
                return "更新产品失败: 未找到产品或当前会话未绑定产品。";
            }
            ProductUpdateBO bo = new ProductUpdateBO();
            bo.setProductId(product.getProductId());
            bo.setProductName(trimToNull(productName));
            bo.setProductCode(trimToNull(productCode));
            if (StrUtil.isNotBlank(categoryPath)) {
                bo.setCategoryId(resolveCategoryId(categoryPath, true));
            }
            bo.setProductType(trimToNull(productType));
            bo.setUnit(trimToNull(unit));
            bo.setStandardPrice(parseMoney(standardPrice, "标准价"));
            bo.setCostPrice(parseMoney(costPrice, "成本价"));
            bo.setDescription(trimToNull(description));
            productService.updateProduct(bo);
            Product updated = productService.getVisibleProduct(product.getProductId());
            return "产品更新成功。\n" + formatProduct(updated);
        } catch (Exception exception) {
            log.error("[Tool调用] updateProduct failed: {}", exception.getMessage(), exception);
            return "更新产品失败: " + exception.getMessage();
        }
    }

    @Tool(description = "停用产品。productIdOrCode 可传产品ID或产品编码；留空时默认停用当前产品会话绑定的产品。")
    @AiToolPermission(value = "product:update_status", action = "停用产品")
    public String deactivateProduct(
            @ToolParam(description = "产品ID或产品编码；留空默认当前产品", required = false) String productIdOrCode) {
        try {
            Product product = resolveProduct(productIdOrCode);
            if (product == null) {
                return "停用产品失败: 未找到产品或当前会话未绑定产品。";
            }
            ProductStatusUpdateBO bo = new ProductStatusUpdateBO();
            bo.setProductId(product.getProductId());
            bo.setStatus("inactive");
            productService.updateStatus(bo);
            return "产品已停用。\n" + formatProduct(productService.getVisibleProduct(product.getProductId()));
        } catch (Exception exception) {
            log.error("[Tool调用] deactivateProduct failed: {}", exception.getMessage(), exception);
            return "停用产品失败: " + exception.getMessage();
        }
    }

    private Product resolveProduct(String productIdOrCode) {
        String key = trimToNull(productIdOrCode);
        if (key == null) {
            Long currentProductId = AiContextHolder.getCurrentProductId();
            return currentProductId == null ? null : productService.getVisibleProduct(currentProductId);
        }
        try {
            return productService.getVisibleProduct(Long.parseLong(key));
        } catch (NumberFormatException ignored) {
            Product product = productService.findVisibleProductByCode(key);
            return product == null ? null : productService.getVisibleProduct(product.getProductId());
        }
    }

    private Long resolveCategoryId(String categoryPath, boolean strict) {
        if (StrUtil.isBlank(categoryPath)) {
            return null;
        }
        Long categoryId = productCategoryService.findCategoryIdByPath(categoryPath.trim());
        if (categoryId == null && strict) {
            throw new IllegalArgumentException("类目不存在: " + categoryPath);
        }
        return categoryId;
    }

    private BigDecimal parseMoney(String value, String label) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + "格式不正确: " + value);
        }
    }

    private String normalizeStatus(String status, boolean defaultEnabled) {
        String text = trimToNull(status);
        if (text == null) {
            return defaultEnabled ? "active" : null;
        }
        if ("启用".equals(text) || "active".equalsIgnoreCase(text) || "enabled".equalsIgnoreCase(text)) {
            return "active";
        }
        if ("停用".equals(text) || "disabled".equalsIgnoreCase(text) || "inactive".equalsIgnoreCase(text)) {
            return "inactive";
        }
        return text;
    }

    private String formatProduct(Product product) {
        if (product == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder()
                .append("- productId: ").append(product.getProductId()).append("\n")
                .append("- 产品名称: ").append(StrUtil.blankToDefault(product.getProductName(), "未命名产品")).append("\n");
        appendLine(builder, "产品编码", product.getProductCode());
        appendLine(builder, "产品类型", productTypeLabel(product.getProductType()));
        appendLine(builder, "单位", product.getUnit());
        appendLine(builder, "标准价", product.getStandardPrice() == null ? null : product.getStandardPrice().toPlainString());
        appendLine(builder, "成本价", product.getCostPrice() == null ? null : product.getCostPrice().toPlainString());
        appendLine(builder, "状态", productStatusLabel(product.getStatus()));
        appendLine(builder, "描述", product.getDescription());
        return builder.toString();
    }

    private String productTypeLabel(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        String label = customFieldService.resolveOptionLabel("product", "productType", text);
        if (!StrUtil.equals(label, text)) {
            return label;
        }
        if ("goods".equalsIgnoreCase(text)) {
            return "商品";
        }
        if ("service".equalsIgnoreCase(text)) {
            return "服务";
        }
        if ("subscription".equalsIgnoreCase(text)) {
            return "订阅";
        }
        if ("other".equalsIgnoreCase(text)) {
            return "其他";
        }
        return text;
    }

    private String productStatusLabel(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        String label = customFieldService.resolveOptionLabel("product", "status", text);
        if (!StrUtil.equals(label, text)) {
            return label;
        }
        if ("active".equalsIgnoreCase(text)) {
            return "启用";
        }
        if ("inactive".equalsIgnoreCase(text)) {
            return "停用";
        }
        return text;
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (StrUtil.isNotBlank(value)) {
            builder.append("- ").append(label).append(": ").append(value).append("\n");
        }
    }

    private void appendInline(StringBuilder builder, String label, String value) {
        if (StrUtil.isNotBlank(value)) {
            builder.append(", ").append(label).append("=").append(value);
        }
    }

    private String firstNonBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : second;
    }

    private String trimToNull(String value) {
        if (StrUtil.isBlank(value) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }
}
