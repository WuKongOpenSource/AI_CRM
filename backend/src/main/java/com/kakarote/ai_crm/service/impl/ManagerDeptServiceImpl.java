package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.DeptAddBO;
import com.kakarote.ai_crm.entity.BO.DeptUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.DeptVO;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.service.IManagerDeptService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.utils.RecursionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ManagerDeptServiceImpl extends ServiceImpl<ManagerDeptMapper, ManagerDept> implements IManagerDeptService {

    @Autowired
    private ManageUserService manageUserService;

    @Override
    public List<DeptVO> queryDeptTree() {
        List<DeptVO> allDepts = baseMapper.queryAllDeptWithUserCount();
        return RecursionUtil.getChildListTree(allDepts, "parentId", 0L, "deptId", "children", DeptVO.class);
    }

    @Override
    public void addDept(DeptAddBO bo) {
        ManagerDept dept = BeanUtil.copyProperties(bo, ManagerDept.class);
        Long parentId = normalizeParentId(dept.getParentId());
        validateParentDept(null, parentId);
        dept.setParentId(parentId);
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        dept.setCreateTime(new Date());
        save(dept);
    }

    @Override
    public void updateDept(DeptUpdateBO bo) {
        ManagerDept dept = getById(bo.getDeptId());
        if (dept == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_SUCH_PARAMENT_ERROR);
        }
        if (bo.getDeptName() != null) {
            dept.setDeptName(bo.getDeptName());
        }
        if (bo.getParentId() != null) {
            Long parentId = normalizeParentId(bo.getParentId());
            validateParentDept(bo.getDeptId(), parentId);
            dept.setParentId(parentId);
        }
        if (bo.getSortOrder() != null) {
            dept.setSortOrder(bo.getSortOrder());
        }
        updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long deptId) {
        // 检查是否有子部门
        long childCount = lambdaQuery().eq(ManagerDept::getParentId, deptId).count();
        if (childCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "该部门下存在子部门，无法删除");
        }
        // 检查是否有关联用户
        long userCount = manageUserService.lambdaQuery().eq(ManagerUser::getDeptId, deptId).count();
        if (userCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "该部门下存在成员，无法删除");
        }
        removeById(deptId);
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private void validateParentDept(Long deptId, Long parentId) {
        if (Objects.equals(parentId, 0L)) {
            return;
        }
        if (Objects.equals(deptId, parentId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "上级部门不能选择当前部门");
        }
        if (getById(parentId) == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "上级部门不存在");
        }
        if (deptId != null && isDescendantDept(deptId, parentId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "上级部门不能选择当前部门或其下级部门");
        }
    }

    private boolean isDescendantDept(Long deptId, Long targetParentId) {
        List<ManagerDept> allDepts = lambdaQuery().list();
        Set<Long> childDeptIds = new HashSet<>();
        collectChildDeptIds(allDepts, deptId, childDeptIds, Const.AUTH_DATA_RECURSION_NUM);
        return childDeptIds.contains(targetParentId);
    }

    private void collectChildDeptIds(List<ManagerDept> allDepts, Long parentId, Set<Long> result, int depth) {
        if (parentId == null || depth <= 0) {
            return;
        }
        for (ManagerDept dept : allDepts) {
            if (Objects.equals(parentId, dept.getParentId()) && result.add(dept.getDeptId())) {
                collectChildDeptIds(allDepts, dept.getDeptId(), result, depth - 1);
            }
        }
    }
}
