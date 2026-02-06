package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.PO.FollowUp;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 跟进记录Mapper
 */
@Mapper
public interface FollowUpMapper extends BaseMapper<FollowUp> {

    /**
     * 分页查询跟进记录
     */
    IPage<FollowUpVO> queryPageList(IPage<FollowUpVO> page, @Param("query") FollowUpQueryBO query);

    /**
     * 查询客户最近跟进记录
     */
    List<FollowUpVO> getRecentByCustomerId(@Param("customerId") Long customerId, @Param("limit") int limit);

    /**
     * 统计本月跟进次数
     */
    Long countThisMonth(@Param("userId") Long userId);
}
