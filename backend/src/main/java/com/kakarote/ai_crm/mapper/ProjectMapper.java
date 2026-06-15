package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.PO.Project;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    BasePage<ProjectVO> queryPageList(IPage<ProjectVO> page, @Param("query") ProjectBO.Query query);

    ProjectVO getProjectById(@Param("projectId") Long projectId);
}
