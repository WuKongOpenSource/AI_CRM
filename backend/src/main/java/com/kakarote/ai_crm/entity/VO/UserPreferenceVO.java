package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.List;

/**
 * 当前用户 UI 偏好。
 */
@Data
public class UserPreferenceVO {

    /**
     * 左侧栏模块排序。
     */
    private List<String> sidebarModuleOrder;
}
