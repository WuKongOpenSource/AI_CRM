package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ProjectTask;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectTaskMapper extends BaseMapper<ProjectTask> {

    List<ProjectVO.ProjectTaskVO> selectProjectTasks(@Param("projectId") Long projectId,
                                                     @Param("keyword") String keyword);
}
