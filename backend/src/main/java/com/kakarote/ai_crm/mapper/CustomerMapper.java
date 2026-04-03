package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 客户Mapper
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 分页查询客户列表
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    IPage<CustomerListVO> queryPageList(IPage<CustomerListVO> page, @Param("query") CustomerQueryBO query);

    /**
     * 分页查询客户列表（含动态自定义字段列，返回 Map）
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    IPage<Map<String, Object>> queryPageListWithCf(
            IPage<Map<String, Object>> page,
            @Param("query") CustomerQueryBO query,
            @Param("cfColumns") List<String> cfColumns);

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    Long queryPageListCount(@Param("query") CustomerQueryBO query);

    /**
     * 查询客户详情
     */
    CustomerDetailVO getCustomerById(@Param("customerId") Long customerId);

    /**
     * 按阶段统计客户数量
     */
    Long countByStage(@Param("stage") String stage);

    /**
     * 按等级统计客户数量
     */
    Long countByLevel(@Param("level") String level);
}
