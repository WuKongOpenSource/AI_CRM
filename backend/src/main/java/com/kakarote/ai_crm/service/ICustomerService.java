package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerAiParseBO;
import com.kakarote.ai_crm.entity.BO.CustomerAiSearchParseBO;
import com.kakarote.ai_crm.entity.BO.CustomerExportBO;
import com.kakarote.ai_crm.entity.BO.CustomerFieldUpdateBO;
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
import com.kakarote.ai_crm.entity.VO.CustomerLogoUploadVO;
import com.kakarote.ai_crm.entity.VO.DashboardStatsVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 客户服务接口
 */
public interface ICustomerService extends IService<Customer> {

    /**
     * 新增客户。
     */
    Long addCustomer(CustomerAddBO customerAddBO);

    /**
     * 更新客户。
     */
    void updateCustomer(CustomerUpdateBO customerUpdateBO);

    /**
     * 更新客户字段。
     */
    CustomerDetailVO updateCustomerField(CustomerFieldUpdateBO fieldUpdateBO);

    /**
     * 删除客户。
     */
    void deleteCustomer(Long customerId);

    /**
     * 分页查询客户列表。
     */
    BasePage<CustomerListVO> queryPageList(CustomerQueryBO queryBO);

    /**
     * 查找客户按精确公司名称。
     */
    List<Customer> findCustomersByExactCompanyName(String companyName);

    /**
     * 查找客户按精确公司名称忽略数据权限。
     */
    List<Customer> findCustomersByExactCompanyNameIgnoreDataPermission(String companyName);

    /**
     * 查找客户按公司名称模糊忽略数据权限。
     */
    List<Customer> findCustomersByCompanyNameLikeIgnoreDataPermission(String keyword, int limit);

    /**
     * 按ID查找客户，忽略数据权限。
     */
    Customer findCustomerByIdIgnoreDataPermission(Long customerId);

    /**
     * 获取客户详情。
     */
    CustomerDetailVO getCustomerDetail(Long customerId);

    /**
     * 更新阶段。
     */
    void updateStage(Long customerId, String stage);

    /**
     * 新增标签。
     */
    void addTag(Long customerId, String tagName, String color);

    /**
     * 移除标签。
     */
    void removeTag(Long customerId, Long tagId);

    /**
     * 转移客户。
     */
    void transferCustomer(CustomerTransferBO transferBO);

    /**
     * 获取统计信息。
     */
    DashboardStatsVO getStatistics();

    /**
     * 导出客户。
     */
    void exportCustomers(CustomerExportBO exportBO, HttpServletResponse response);

    /**
     * 下载导入模板客户。
     */
    void downloadImportTemplate(HttpServletResponse response);

    /**
     * 预览导入客户。
     */
    CustomerImportPreviewVO importPreview(MultipartFile file);

    /**
     * 确认导入客户。
     */
    CustomerImportResultVO confirmImport(List<CustomerImportBO> rows);

    CustomerLogoUploadVO uploadCustomerLogo(MultipartFile file);

    /**
     * 使用 AI 解析客户。
     */
    CustomerAiParseVO aiParseCustomer(CustomerAiParseBO parseBO);

    /**
     * 生成AI报告。
     */
    CustomerAiReportVO generateAiReport(Long customerId);

    /**
     * Refresh customer activity metadata after related CRM records change.
     */
    void refreshCustomerActivity(Long customerId);

    /**
     * 使用 AI 解析搜索。
     */
    CustomerAiSearchParseVO aiParseSearch(CustomerAiSearchParseBO parseBO);
}
