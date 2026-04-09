package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerAiParseBO;
import com.kakarote.ai_crm.entity.BO.CustomerAiSearchParseBO;
import com.kakarote.ai_crm.entity.BO.CustomerExportBO;
import com.kakarote.ai_crm.entity.BO.CustomerImportBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerTransferBO;
import com.kakarote.ai_crm.entity.BO.CustomerUpdateBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.CustomerAiParseVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiReportVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiSearchParseVO;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.CustomerImportPreviewVO;
import com.kakarote.ai_crm.entity.VO.CustomerImportResultVO;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import com.kakarote.ai_crm.entity.VO.DashboardStatsVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 客户服务接口
 */
public interface ICustomerService extends IService<Customer> {

    Long addCustomer(CustomerAddBO customerAddBO);

    void updateCustomer(CustomerUpdateBO customerUpdateBO);

    void deleteCustomer(Long customerId);

    BasePage<CustomerListVO> queryPageList(CustomerQueryBO queryBO);

    List<Customer> findCustomersByExactCompanyName(String companyName);

    CustomerDetailVO getCustomerDetail(Long customerId);

    void updateStage(Long customerId, String stage);

    void addTag(Long customerId, String tagName, String color);

    void removeTag(Long customerId, Long tagId);

    void transferCustomer(CustomerTransferBO transferBO);

    DashboardStatsVO getStatistics();

    void exportCustomers(CustomerExportBO exportBO, HttpServletResponse response);

    void downloadImportTemplate(HttpServletResponse response);

    CustomerImportPreviewVO importPreview(MultipartFile file);

    CustomerImportResultVO confirmImport(List<CustomerImportBO> rows);

    CustomerAiParseVO aiParseCustomer(CustomerAiParseBO parseBO);

    CustomerAiReportVO generateAiReport(Long customerId);

    CustomerAiSearchParseVO aiParseSearch(CustomerAiSearchParseBO parseBO);
}
