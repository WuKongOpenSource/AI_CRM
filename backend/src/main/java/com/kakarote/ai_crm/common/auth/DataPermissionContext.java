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

    public static DataPermissionContext all() {
        return new DataPermissionContext(true, new ArrayList<>());
    }

    public static DataPermissionContext none() {
        return new DataPermissionContext(false, new ArrayList<>());
    }

    public static DataPermissionContext users(Collection<Long> userIds) {
        return new DataPermissionContext(false, new ArrayList<>(userIds));
    }

    public boolean isEmpty() {
        return !allData && (userIds == null || userIds.isEmpty());
    }
}
