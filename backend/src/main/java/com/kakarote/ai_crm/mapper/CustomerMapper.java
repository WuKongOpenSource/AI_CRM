package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 客户Mapper
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 分页查询客户列表
     */
    IPage<CustomerListVO> queryPageList(IPage<CustomerListVO> page, @Param("query") CustomerQueryBO query);

    /**
     * 查询客户详情
     */
    CustomerListVO getCustomerById(@Param("customerId") Long customerId);

    /**
     * 按阶段统计客户数量
     */
    Long countByStage(@Param("stage") String stage);

    /**
     * 按等级统计客户数量
     */
    Long countByLevel(@Param("level") String level);
}
