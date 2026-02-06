package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerUpdateBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import com.kakarote.ai_crm.entity.VO.DashboardStatsVO;

/**
 * 客户服务接口
 */
public interface ICustomerService extends IService<Customer> {

    /**
     * 添加客户
     */
    Long addCustomer(CustomerAddBO customerAddBO);

    /**
     * 更新客户
     */
    void updateCustomer(CustomerUpdateBO customerUpdateBO);

    /**
     * 删除客户
     */
    void deleteCustomer(Long customerId);

    /**
     * 分页查询客户列表
     */
    BasePage<CustomerListVO> queryPageList(CustomerQueryBO queryBO);

    /**
     * 获取客户详情
     */
    CustomerDetailVO getCustomerDetail(Long customerId);

    /**
     * 更新客户阶段
     */
    void updateStage(Long customerId, String stage);

    /**
     * 添加客户标签
     */
    void addTag(Long customerId, String tagName, String color);

    /**
     * 删除客户标签
     */
    void removeTag(Long customerId, Long tagId);

    /**
     * 获取客户统计信息
     */
    DashboardStatsVO getStatistics();
}
