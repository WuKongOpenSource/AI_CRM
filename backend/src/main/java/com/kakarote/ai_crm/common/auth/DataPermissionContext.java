package com.kakarote.ai_crm.common.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataPermissionContext {

    private boolean allData;

    private List<Long> userIds = new ArrayList<>();

    /**
     * 处理all方法逻辑。
     */
    public static DataPermissionContext all() {
        return new DataPermissionContext(true, new ArrayList<>());
    }

    /**
     * 处理none方法逻辑。
     */
    public static DataPermissionContext none() {
        return new DataPermissionContext(false, new ArrayList<>());
    }

    /**
     * 处理users方法逻辑。
     */
    public static DataPermissionContext users(Collection<Long> userIds) {
        return new DataPermissionContext(false, new ArrayList<>(userIds));
    }

    /**
     * 判断是否空值。
     */
    public boolean isEmpty() {
        return !allData && (userIds == null || userIds.isEmpty());
    }
}
