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
import org.apache.ibatis.annotations.Select;

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

    /**
     * 分页查询Count。
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    Long queryPageListCount(@Param("query") CustomerQueryBO query);

    /**
     * 查询按ID忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT c.*, u.realname AS owner_name
            FROM crm_customer c
            LEFT JOIN manager_user u ON c.owner_id = u.user_id
            WHERE c.customer_id = #{customerId}
            """)
    Customer selectByIdIgnoreDataPermission(@Param("customerId") Long customerId);

    /**
     * 查询按精确公司名称忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT c.*, u.realname AS owner_name
            FROM crm_customer c
            LEFT JOIN manager_user u ON c.owner_id = u.user_id
            WHERE c.company_name = #{companyName}
              AND c.status = 1
            ORDER BY c.create_time DESC
            """)
    List<Customer> selectByExactCompanyNameIgnoreDataPermission(@Param("companyName") String companyName);

    /**
     * 查询按公司名称模糊忽略数据权限。
     */
    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT c.*, u.realname AS owner_name
            FROM crm_customer c
            LEFT JOIN manager_user u ON c.owner_id = u.user_id
            WHERE c.company_name LIKE CONCAT('%', #{keyword}, '%')
              AND c.status = 1
            ORDER BY c.create_time DESC
            LIMIT #{limit}
            """)
    List<Customer> selectByCompanyNameLikeIgnoreDataPermission(@Param("keyword") String keyword,
                                                               @Param("limit") Integer limit);

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
