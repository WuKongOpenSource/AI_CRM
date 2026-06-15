package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Project;
import com.kakarote.ai_crm.entity.PO.ProjectLane;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ProjectLaneMapper;
import com.kakarote.ai_crm.mapper.ProjectMapper;
import com.kakarote.ai_crm.mapper.ProjectTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectLaneMapper projectLaneMapper;

    @Mock
    private ProjectTaskMapper projectTaskMapper;

    @Mock
    private ManageUserMapper manageUserMapper;

    @Mock
    private CustomerMapper customerMapper;

    private ProjectServiceImpl projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl(projectMapper, projectLaneMapper, projectTaskMapper, manageUserMapper, customerMapper);
        setCurrentUser(1001L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProjectDefaultsOwnerStatusAndSystemLanes() {
        when(manageUserMapper.getUserId(1001L)).thenReturn(activeUser(1001L));

        ProjectBO.Create createBO = new ProjectBO.Create();
        createBO.setName("Implementation");
        createBO.setDescription("Rollout work");

        ProjectVO result = projectService.createProject(createBO);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(projectCaptor.capture());
        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getName()).isEqualTo("Implementation");
        assertThat(savedProject.getStatus()).isEqualTo("NOT_STARTED");
        assertThat(savedProject.getOwnerId()).isEqualTo(1001L);
        assertThat(savedProject.getCreateUserId()).isEqualTo(1001L);
        assertThat(savedProject.getUpdateUserId()).isEqualTo(1001L);
        assertThat(result.getName()).isEqualTo("Implementation");

        ArgumentCaptor<ProjectLane> laneCaptor = ArgumentCaptor.forClass(ProjectLane.class);
        verify(projectLaneMapper, times(3)).insert(laneCaptor.capture());
        List<String> laneNames = laneCaptor.getAllValues().stream().map(ProjectLane::getName).toList();
        assertThat(laneNames).containsExactly("未开始", "进行中", "已完成");
        assertThat(laneCaptor.getAllValues()).allSatisfy(lane -> {
            assertThat(lane.getProjectId()).isEqualTo(savedProject.getProjectId());
            assertThat(lane.getSystemFlag()).isTrue();
        });
    }

    private ManagerUser activeUser(Long userId) {
        ManagerUser user = new ManagerUser();
        user.setUserId(userId);
        user.setUsername("user" + userId);
        user.setRealname("User " + userId);
        user.setStatus(1);
        return user;
    }

    private void setCurrentUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(activeUser(userId));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
    }
}
