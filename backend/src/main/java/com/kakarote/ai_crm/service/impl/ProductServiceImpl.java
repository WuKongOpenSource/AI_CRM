package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ProductAddBO;
import com.kakarote.ai_crm.entity.BO.ProductImportBO;
import com.kakarote.ai_crm.entity.BO.ProductQueryBO;
import com.kakarote.ai_crm.entity.BO.ProductSettingsUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductStatusUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductTransferBO;
import com.kakarote.ai_crm.entity.BO.ProductUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Product;
import com.kakarote.ai_crm.entity.PO.ProductCategory;
import com.kakarote.ai_crm.entity.VO.ProductImportPreviewVO;
import com.kakarote.ai_crm.entity.VO.ProductImportResultVO;
import com.kakarote.ai_crm.entity.VO.ProductSettingsVO;
import com.kakarote.ai_crm.entity.VO.ProductVO;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ProductMapper;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.IProductCategoryService;
import com.kakarote.ai_crm.service.IProductService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import com.kakarote.ai_crm.utils.UserUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    private static final String ENTITY_PRODUCT = "product";
    private static final String CONFIG_PRODUCT_CODE_REQUIRED = "product.code.required";
    private static final String CONFIG_TYPE_PRODUCT = "product";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_INACTIVE = "inactive";
    private static final String DEFAULT_PRODUCT_TYPE = "goods";
    private static final List<String> IMPORT_HEADERS = List.of(
            "产品名称", "产品编码", "类目路径", "产品类型", "单位", "标准价", "成本价", "负责人", "状态", "描述"
    );

    @Autowired
    private IProductCategoryService productCategoryService;

    @Autowired
    private ISystemConfigService systemConfigService;

    @Autowired
    private ICustomFieldService customFieldService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private IGlobalSearchIndexService globalSearchIndexService;

    @Autowired
    private ManageUserMapper manageUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addProduct(ProductAddBO bo) {
        Product product = new Product();
        product.setProductId(IdWorker.getId());
        product.setProductName(requireProductName(bo.getProductName()));
        product.setProductCode(normalizeCode(bo.getProductCode()));
        product.setMainImage(normalizeOptional(bo.getMainImage()));
        validateCodeForSave(product.getProductCode(), null);
        product.setCategoryId(resolveCategoryId(bo.getCategoryId()));
        product.setProductType(StrUtil.blankToDefault(normalizeOptional(bo.getProductType()), DEFAULT_PRODUCT_TYPE));
        product.setUnit(normalizeOptional(bo.getUnit()));
        product.setStandardPrice(bo.getStandardPrice());
        product.setCostPrice(bo.getCostPrice());
        product.setOwnerId(resolveOwnerId(bo.getOwnerId()));
        product.setStatus(STATUS_ACTIVE);
        product.setDescription(normalizeOptional(bo.getDescription()));
        product.setDelFlag(0);
        customFieldService.validateUniqueCustomFieldValues(ENTITY_PRODUCT, null,
                buildUniqueFieldValues(product, bo.getCustomFields()));
        save(product);
        updateCustomFields(product.getProductId(), bo.getCustomFields());
        refreshProductSearchIndex(product.getProductId());
        return product.getProductId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductUpdateBO bo) {
        Product product = getVisibleProduct(bo.getProductId());
        if (StrUtil.isNotBlank(bo.getProductName())) {
            product.setProductName(requireProductName(bo.getProductName()));
        }
        if (bo.getProductCode() != null) {
            product.setProductCode(normalizeCode(bo.getProductCode()));
        }
        if (bo.getMainImage() != null) {
            product.setMainImage(normalizeOptional(bo.getMainImage()));
        }
        validateCodeForSave(product.getProductCode(), product.getProductId());
        if (bo.getCategoryId() != null) {
            product.setCategoryId(resolveCategoryId(bo.getCategoryId()));
        }
        if (bo.getProductType() != null) {
            product.setProductType(StrUtil.blankToDefault(normalizeOptional(bo.getProductType()), DEFAULT_PRODUCT_TYPE));
        }
        if (bo.getUnit() != null) {
            product.setUnit(normalizeOptional(bo.getUnit()));
        }
        if (bo.getStandardPrice() != null) {
            product.setStandardPrice(bo.getStandardPrice());
        }
        if (bo.getCostPrice() != null) {
            product.setCostPrice(bo.getCostPrice());
        }
        if (bo.getOwnerId() != null) {
            product.setOwnerId(resolveOwnerId(bo.getOwnerId()));
        }
        if (bo.getDescription() != null) {
            product.setDescription(normalizeOptional(bo.getDescription()));
        }
        product.setUpdateUserId(UserUtil.getUserIdOrNull());
        customFieldService.validateUniqueCustomFieldValues(ENTITY_PRODUCT, product.getProductId(),
                buildUniqueFieldValues(product, bo.getCustomFields()));
        updateById(product);
        updateCustomFields(product.getProductId(), bo.getCustomFields());
        refreshProductSearchIndex(product.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        Product product = getVisibleProduct(productId);
        product.setDelFlag(1);
        product.setUpdateUserId(UserUtil.getUserIdOrNull());
        updateById(product);
        globalSearchIndexService.deleteByEntity(ENTITY_PRODUCT, productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(ProductStatusUpdateBO bo) {
        Product product = getVisibleProduct(bo.getProductId());
        String status = normalizeStatus(bo.getStatus());
        product.setStatus(status);
        product.setUpdateUserId(UserUtil.getUserIdOrNull());
        updateById(product);
        refreshProductSearchIndex(product.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferProducts(ProductTransferBO bo) {
        if (bo.getProductIds() == null || bo.getProductIds().isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "请选择要转移的产品");
        }
        Long ownerId = resolveOwnerId(bo.getOwnerId());
        for (Long productId : bo.getProductIds()) {
            Product product = getVisibleProduct(productId);
            product.setOwnerId(ownerId);
            product.setUpdateUserId(UserUtil.getUserIdOrNull());
            updateById(product);
            refreshProductSearchIndex(productId);
        }
    }

    @Override
    public BasePage<ProductVO> queryPageList(ProductQueryBO queryBO) {
        if (queryBO == null) {
            queryBO = new ProductQueryBO();
        }
        BasePage<ProductVO> page = baseMapper.queryPageList(queryBO.parse(), queryBO);
        enrichProductVOs(page.getRecords());
        return page;
    }

    @Override
    public ProductVO getProductDetail(Long productId) {
        ProductVO vo = baseMapper.getProductById(productId);
        if (vo == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "产品不存在");
        }
        vo.setCustomFields(customFieldService.getCustomFieldValues(ENTITY_PRODUCT, productId));
        resolveProductImageUrl(vo);
        return vo;
    }

    @Override
    public Product getVisibleProduct(Long productId) {
        Product product = getById(productId);
        if (product == null || Integer.valueOf(1).equals(product.getDelFlag())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "产品不存在");
        }
        return product;
    }

    @Override
    public Product findVisibleProductByCode(String productCode) {
        String code = normalizeCode(productCode);
        if (code == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getProductCode, code)
                .eq(Product::getDelFlag, 0)
                .last("LIMIT 1"), false);
    }

    @Override
    public ProductSettingsVO getSettings() {
        ProductSettingsVO vo = new ProductSettingsVO();
        vo.setCodeRequired(isCodeRequired());
        return vo;
    }

    @Override
    public void updateSettings(ProductSettingsUpdateBO bo) {
        boolean required = bo == null || bo.getCodeRequired() == null || Boolean.TRUE.equals(bo.getCodeRequired());
        systemConfigService.updateConfigsWithType(
                Map.of(CONFIG_PRODUCT_CODE_REQUIRED, String.valueOf(required)),
                CONFIG_TYPE_PRODUCT
        );
    }

    @Override
    public void exportProducts(ProductQueryBO queryBO, HttpServletResponse response) {
        if (queryBO == null) {
            queryBO = new ProductQueryBO();
        }
        queryBO.setPage(1);
        queryBO.setLimit(10000);
        List<ProductVO> products = queryPageList(queryBO).getRecords();
        if (products.isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "没有符合条件的产品数据");
        }

        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.writeRow(List.of("产品名称", "产品编码", "类目", "产品类型", "单位", "标准价", "成本价", "负责人", "状态", "描述", "更新时间"));
        for (ProductVO product : products) {
            writer.writeRow(List.of(
                    safe(product.getProductName()),
                    safe(product.getProductCode()),
                    safe(product.getCategoryPath()),
                    safe(product.getProductType()),
                    safe(product.getUnit()),
                    product.getStandardPrice() == null ? "" : product.getStandardPrice(),
                    product.getCostPrice() == null ? "" : product.getCostPrice(),
                    safe(product.getOwnerName()),
                    statusLabel(product.getStatus()),
                    safe(product.getDescription()),
                    product.getUpdateTime() == null ? "" : product.getUpdateTime()
            ));
        }
        writeExcel(response, writer, "products.xlsx");
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.writeRow(IMPORT_HEADERS);
        writer.writeRow(List.of("示例产品", "SKU-001", "软件/订阅", "service", "套", "99.00", "30.00", "管理员", "active", "示例描述"));
        writeExcel(response, writer, "product_import_template.xlsx");
    }

    @Override
    public ProductImportPreviewVO importPreview(MultipartFile file) {
        ProductImportPreviewVO preview = new ProductImportPreviewVO();
        if (file == null || file.isEmpty()) {
            preview.getErrors().add("导入文件不能为空");
            return preview;
        }

        List<ProductImportBO> rows = new ArrayList<>();
        try (ExcelReader reader = ExcelUtil.getReader(file.getInputStream())) {
            List<List<Object>> rawRows = reader.read();
            for (int i = 1; i < rawRows.size(); i++) {
                List<Object> raw = rawRows.get(i);
                if (isEmptyRow(raw)) {
                    continue;
                }
                ProductImportBO row = parseImportRow(i + 1, raw);
                validateImportRow(row);
                rows.add(row);
            }
        } catch (Exception exception) {
            preview.getErrors().add("读取导入文件失败: " + exception.getMessage());
        }

        int errorRows = (int) rows.stream().filter(row -> row.getErrors() != null && !row.getErrors().isEmpty()).count();
        int duplicateRows = (int) rows.stream().filter(ProductImportBO::isDuplicate).count();
        preview.setRows(rows);
        preview.setTotalRows(rows.size());
        preview.setErrorRows(errorRows);
        preview.setDuplicateRows(duplicateRows);
        preview.setValidRows(rows.size() - errorRows);
        return preview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductImportResultVO confirmImport(List<ProductImportBO> rows) {
        ProductImportResultVO result = new ProductImportResultVO();
        if (rows == null || rows.isEmpty()) {
            return result;
        }

        for (ProductImportBO row : rows) {
            try {
                if (row.getErrors() != null && !row.getErrors().isEmpty()) {
                    result.setSkipped(result.getSkipped() + 1);
                    continue;
                }
                if (row.isDuplicate() && row.getExistingProductId() != null) {
                    if ("skip".equalsIgnoreCase(row.getHandleMode())) {
                        result.setSkipped(result.getSkipped() + 1);
                        continue;
                    }
                    ProductUpdateBO updateBO = new ProductUpdateBO();
                    updateBO.setProductId(row.getExistingProductId());
                    updateBO.setProductName(row.getProductName());
                    updateBO.setProductCode(row.getProductCode());
                    updateBO.setCategoryId(row.getCategoryId());
                    updateBO.setProductType(row.getProductType());
                    updateBO.setUnit(row.getUnit());
                    updateBO.setStandardPrice(row.getStandardPrice());
                    updateBO.setCostPrice(row.getCostPrice());
                    updateBO.setOwnerId(row.getOwnerId());
                    updateBO.setDescription(row.getDescription());
                    updateProduct(updateBO);
                    result.setUpdated(result.getUpdated() + 1);
                    continue;
                }

                ProductAddBO addBO = new ProductAddBO();
                addBO.setProductName(row.getProductName());
                addBO.setProductCode(row.getProductCode());
                addBO.setCategoryId(row.getCategoryId());
                addBO.setProductType(row.getProductType());
                addBO.setUnit(row.getUnit());
                addBO.setStandardPrice(row.getStandardPrice());
                addBO.setCostPrice(row.getCostPrice());
                addBO.setOwnerId(row.getOwnerId());
                addBO.setDescription(row.getDescription());
                Long productId = addProduct(addBO);
                if (STATUS_INACTIVE.equals(row.getStatus())) {
                    ProductStatusUpdateBO statusBO = new ProductStatusUpdateBO();
                    statusBO.setProductId(productId);
                    statusBO.setStatus(STATUS_INACTIVE);
                    updateStatus(statusBO);
                }
                result.setImported(result.getImported() + 1);
            } catch (Exception exception) {
                result.getErrors().add("第" + row.getRowNum() + "行导入失败: " + exception.getMessage());
            }
        }
        return result;
    }

    @Override
    public void refreshProductSearchIndex(Long productId) {
        try {
            globalSearchIndexService.refreshProductIndex(productId);
        } catch (Exception exception) {
            log.warn("刷新产品搜索索引失败: productId={}, error={}", productId, exception.getMessage());
        }
    }

    private void enrichProductVOs(List<ProductVO> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> productIds = products.stream()
                .map(ProductVO::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Map<String, Object>> customFieldValues = customFieldService.getBatchCustomFieldValues(ENTITY_PRODUCT, productIds);
        for (ProductVO product : products) {
            product.setCustomFields(customFieldValues.get(product.getProductId()));
            resolveProductImageUrl(product);
        }
    }

    private void resolveProductImageUrl(ProductVO product) {
        if (product == null || StrUtil.isBlank(product.getMainImage())) {
            return;
        }
        product.setMainImageUrl(fileStorageService.getUrl(product.getMainImage()));
    }

    private void updateCustomFields(Long productId, Map<String, Object> customFields) {
        if (customFields != null && !customFields.isEmpty()) {
            customFieldService.updateCustomFieldValues(ENTITY_PRODUCT, productId, customFields);
        }
    }

    private String requireProductName(String productName) {
        if (StrUtil.isBlank(productName)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "产品名称不能为空");
        }
        return productName.trim();
    }

    private String normalizeCode(String code) {
        return normalizeOptional(code);
    }

    private String normalizeOptional(String value) {
        if (StrUtil.isBlank(value) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private void validateCodeForSave(String productCode, Long excludeProductId) {
        if (isCodeRequired() && StrUtil.isBlank(productCode)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "产品编码不能为空");
        }
        if (StrUtil.isBlank(productCode)) {
            return;
        }
        Long count = baseMapper.countByCodeIgnoreDataPermission(productCode, excludeProductId);
        if (count != null && count > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "产品编码已存在");
        }
    }

    private boolean isCodeRequired() {
        return Boolean.parseBoolean(systemConfigService.getConfigValue(CONFIG_PRODUCT_CODE_REQUIRED, "true"));
    }

    private Long resolveCategoryId(Long categoryId) {
        Long resolved = categoryId == null ? productCategoryService.ensureDefaultCategoryId() : categoryId;
        ProductCategory category = productCategoryService.getById(resolved);
        if (category == null || Integer.valueOf(1).equals(category.getDelFlag())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "产品类目不存在");
        }
        if (category.getLevel() != null && category.getLevel() > 3) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "产品类目最多支持三级");
        }
        return resolved;
    }

    private Long resolveOwnerId(Long ownerId) {
        Long resolved = ownerId == null ? UserUtil.getUserIdOrNull() : ownerId;
        if (resolved == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "负责人不能为空");
        }
        ManagerUser owner = manageUserMapper.getUserId(resolved);
        if (owner == null || Integer.valueOf(0).equals(owner.getStatus())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "负责人不存在或已禁用");
        }
        return resolved;
    }

    private String normalizeStatus(String status) {
        String normalized = normalizeOptional(status);
        if (STATUS_ACTIVE.equalsIgnoreCase(normalized)) {
            return STATUS_ACTIVE;
        }
        if (STATUS_INACTIVE.equalsIgnoreCase(normalized)) {
            return STATUS_INACTIVE;
        }
        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "产品状态无效");
    }

    private Map<String, Object> buildUniqueFieldValues(Product product, Map<String, Object> customFields) {
        Map<String, Object> values = new HashMap<>();
        values.put("productName", product.getProductName());
        values.put("productCode", product.getProductCode());
        values.put("productType", product.getProductType());
        values.put("unit", product.getUnit());
        values.put("standardPrice", product.getStandardPrice());
        values.put("costPrice", product.getCostPrice());
        values.put("ownerId", product.getOwnerId());
        if (customFields != null) {
            values.putAll(customFields);
        }
        return values;
    }

    private ProductImportBO parseImportRow(int rowNum, List<Object> raw) {
        ProductImportBO row = new ProductImportBO();
        row.setRowNum(rowNum);
        row.setProductName(cell(raw, 0));
        row.setProductCode(cell(raw, 1));
        row.setCategoryPath(cell(raw, 2));
        row.setProductType(cell(raw, 3));
        row.setUnit(cell(raw, 4));
        row.setStandardPrice(parseDecimal(cell(raw, 5), row, "标准价"));
        row.setCostPrice(parseDecimal(cell(raw, 6), row, "成本价"));
        row.setOwnerName(cell(raw, 7));
        row.setStatus(StrUtil.blankToDefault(cell(raw, 8), STATUS_ACTIVE));
        row.setDescription(cell(raw, 9));
        return row;
    }

    private void validateImportRow(ProductImportBO row) {
        if (StrUtil.isBlank(row.getProductName())) {
            row.getErrors().add("产品名称不能为空");
        }
        row.setProductCode(normalizeCode(row.getProductCode()));
        if (isCodeRequired() && StrUtil.isBlank(row.getProductCode())) {
            row.getErrors().add("产品编码不能为空");
        }
        if (StrUtil.isBlank(row.getOwnerName())) {
            row.getErrors().add("负责人不能为空");
        } else {
            ManagerUser owner = findUserByName(row.getOwnerName());
            if (owner == null) {
                row.getErrors().add("负责人不存在: " + row.getOwnerName());
            } else {
                row.setOwnerId(owner.getUserId());
            }
        }
        if (StrUtil.isBlank(row.getCategoryPath())) {
            row.setCategoryId(productCategoryService.ensureDefaultCategoryId());
        } else {
            Long categoryId = productCategoryService.findCategoryIdByPath(row.getCategoryPath());
            if (categoryId == null) {
                row.getErrors().add("类目路径不存在: " + row.getCategoryPath());
            } else {
                row.setCategoryId(categoryId);
            }
        }
        row.setProductType(StrUtil.blankToDefault(normalizeOptional(row.getProductType()), DEFAULT_PRODUCT_TYPE));
        row.setStatus(normalizeImportStatus(row.getStatus(), row));
        if (StrUtil.isNotBlank(row.getProductCode())) {
            Product existing = baseMapper.selectByCodeIgnoreDataPermission(row.getProductCode());
            if (existing != null) {
                row.setDuplicate(true);
                row.setExistingProductId(existing.getProductId());
                if (StrUtil.isBlank(row.getHandleMode())) {
                    row.setHandleMode("update");
                }
            }
        }
    }

    private ManagerUser findUserByName(String name) {
        String keyword = name.trim();
        return manageUserMapper.selectOne(new LambdaQueryWrapper<ManagerUser>()
                .and(wrapper -> wrapper.eq(ManagerUser::getRealname, keyword)
                        .or()
                        .eq(ManagerUser::getUsername, keyword))
                .last("LIMIT 1"));
    }

    private String normalizeImportStatus(String status, ProductImportBO row) {
        if (StrUtil.isBlank(status)) {
            return STATUS_ACTIVE;
        }
        String normalized = status.trim();
        if ("启用".equals(normalized) || STATUS_ACTIVE.equalsIgnoreCase(normalized)) {
            return STATUS_ACTIVE;
        }
        if ("停用".equals(normalized) || STATUS_INACTIVE.equalsIgnoreCase(normalized)) {
            return STATUS_INACTIVE;
        }
        row.getErrors().add("产品状态无效: " + status);
        return STATUS_ACTIVE;
    }

    private BigDecimal parseDecimal(String value, ProductImportBO row, String label) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception exception) {
            row.getErrors().add(label + "格式无效: " + value);
            return null;
        }
    }

    private boolean isEmptyRow(List<Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return true;
        }
        return raw.stream().allMatch(cell -> cell == null || StrUtil.isBlank(String.valueOf(cell)));
    }

    private String cell(List<Object> row, int index) {
        if (row == null || row.size() <= index || row.get(index) == null) {
            return null;
        }
        return String.valueOf(row.get(index)).trim();
    }

    private String statusLabel(String status) {
        return STATUS_INACTIVE.equals(status) ? "停用" : "启用";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void writeExcel(HttpServletResponse response, ExcelWriter writer, String fileName) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName);
            try (ServletOutputStream out = response.getOutputStream()) {
                writer.flush(out, true);
            }
        } catch (IOException exception) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "导出 Excel 失败");
        } finally {
            writer.close();
        }
    }
}
