package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.entity.VO.RoleVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */

public interface ManagerRoleMapper extends BaseMapper<ManagerRole> {

    /**
     * 查询角色列表包含用户Count。
     */
    List<RoleVO> queryRoleListWithUserCount(@Param("search") String search);
}
