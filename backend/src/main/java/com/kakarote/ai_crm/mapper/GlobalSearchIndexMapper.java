package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.PO.GlobalSearchIndex;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GlobalSearchIndexMapper extends BaseMapper<GlobalSearchIndex> {

    /**
     * 处理upsert方法逻辑。
     */
    @InterceptorIgnore(dataPermission = "true")
    void upsert(@Param("index") GlobalSearchIndex index);

    /**
     * 删除按Entity。
     */
    @InterceptorIgnore(dataPermission = "true")
    void deleteByEntity(@Param("entityType") String entityType,
                        @Param("entityId") Long entityId);

    /**
     * 删除按客户ID和Entity类型。
     */
    @InterceptorIgnore(dataPermission = "true")
    void deleteByCustomerIdAndEntityType(@Param("customerId") Long customerId,
                                         @Param("entityType") String entityType);

    /**
     * 查询分页Count。
     */
    @InterceptorIgnore(dataPermission = "true")
    Long queryPageCount(@Param("query") GlobalSearchQueryBO query);

    /**
     * 分页查询全局搜索索引列表。
     */
    @InterceptorIgnore(dataPermission = "true")
    IPage<GlobalSearchResultVO> queryPageList(IPage<GlobalSearchResultVO> page,
                                              @Param("query") GlobalSearchQueryBO query);
}
