package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ProductAddBO;
import com.kakarote.ai_crm.entity.BO.ProductImportBO;
import com.kakarote.ai_crm.entity.BO.ProductQueryBO;
import com.kakarote.ai_crm.entity.BO.ProductSettingsUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductStatusUpdateBO;
import com.kakarote.ai_crm.entity.BO.ProductTransferBO;
import com.kakarote.ai_crm.entity.BO.ProductUpdateBO;
import com.kakarote.ai_crm.entity.PO.Product;
import com.kakarote.ai_crm.entity.VO.ProductImportPreviewVO;
import com.kakarote.ai_crm.entity.VO.ProductImportResultVO;
import com.kakarote.ai_crm.entity.VO.ProductSettingsVO;
import com.kakarote.ai_crm.entity.VO.ProductVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IProductService extends IService<Product> {

    Long addProduct(ProductAddBO bo);

    void updateProduct(ProductUpdateBO bo);

    void deleteProduct(Long productId);

    void updateStatus(ProductStatusUpdateBO bo);

    void transferProducts(ProductTransferBO bo);

    BasePage<ProductVO> queryPageList(ProductQueryBO queryBO);

    ProductVO getProductDetail(Long productId);

    Product getVisibleProduct(Long productId);

    Product findVisibleProductByCode(String productCode);

    ProductSettingsVO getSettings();

    void updateSettings(ProductSettingsUpdateBO bo);

    void exportProducts(ProductQueryBO queryBO, HttpServletResponse response);

    void downloadImportTemplate(HttpServletResponse response);

    ProductImportPreviewVO importPreview(MultipartFile file);

    ProductImportResultVO confirmImport(List<ProductImportBO> rows);

    void refreshProductSearchIndex(Long productId);
}
