package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

import java.util.List;

/**
 * 当前用户偏好更新参数。
 */
@Data
public class UserPreferenceUpdateBO {

    /**
     * 左侧栏模块排序。
     */
    private List<String> sidebarModuleOrder;
}
