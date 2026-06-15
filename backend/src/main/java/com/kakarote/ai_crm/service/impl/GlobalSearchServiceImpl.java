package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;
import com.kakarote.ai_crm.service.IGlobalSearchService;
import com.kakarote.ai_crm.service.PermissionService;
import com.kakarote.ai_crm.service.support.GlobalSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GlobalSearchServiceImpl implements IGlobalSearchService {

    private static final Map<String, String> PERMISSION_BY_TYPE = new LinkedHashMap<>();

    static {
        PERMISSION_BY_TYPE.put("customer", "customer:view");
        PERMISSION_BY_TYPE.put("contact", "contact:view");
        PERMISSION_BY_TYPE.put("task", "task:view");
        PERMISSION_BY_TYPE.put("schedule", "schedule:view");
        PERMISSION_BY_TYPE.put("knowledge", "knowledge:view");
    }

    private final GlobalSearchRepository globalSearchRepository;
    private final PermissionService permissionService;

    @Override
    public BasePage<GlobalSearchResultVO> search(GlobalSearchQueryBO queryBO) {
        GlobalSearchQueryBO safeQuery = queryBO == null ? new GlobalSearchQueryBO() : queryBO;
        BasePage<GlobalSearchResultVO> page = safeQuery.parse();
        String keyword = normalizeKeyword(safeQuery.getKeyword());
        if (keyword.isBlank()) {
            page.setTotal(0);
            page.setRecords(List.of());
            return page;
        }

        List<String> searchableTypes = resolveSearchableTypes(resolveRequestedType(safeQuery));
        if (searchableTypes.isEmpty()) {
            page.setTotal(0);
            page.setRecords(List.of());
            return page;
        }

        String pattern = "%" + keyword + "%";
        long total = 0;
        for (String type : searchableTypes) {
            total += globalSearchRepository.count(type, keyword, pattern);
        }
        page.setTotal(total);
        if (total == 0) {
            page.setRecords(List.of());
            return page;
        }

        long offset = Math.max(0, (page.getCurrent() - 1) * page.getSize());
        int fetchSize = Math.toIntExact(Math.min(Integer.MAX_VALUE, offset + page.getSize()));
        List<GlobalSearchResultVO> merged = new ArrayList<>();
        for (String type : searchableTypes) {
            merged.addAll(globalSearchRepository.search(type, keyword, pattern, fetchSize));
        }

        List<GlobalSearchResultVO> records = merged.stream()
                .sorted(this::compareResult)
                .skip(offset)
                .limit(page.getSize())
                .toList();
        records.forEach(this::normalizeResult);
        page.setRecords(records);
        return page;
    }

    private String resolveRequestedType(GlobalSearchQueryBO queryBO) {
        if (queryBO == null) {
            return null;
        }
        String entityType = queryBO.getEntityType();
        if (entityType != null && !entityType.isBlank()) {
            return entityType;
        }
        return queryBO.getType();
    }

    private List<String> resolveSearchableTypes(String requestedType) {
        String normalizedType = normalizeType(requestedType);
        return PERMISSION_BY_TYPE.entrySet().stream()
                .filter(entry -> "all".equals(normalizedType) || entry.getKey().equals(normalizedType))
                .filter(entry -> permissionService.hasPermission(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "all";
        }
        return PERMISSION_BY_TYPE.containsKey(normalized) ? normalized : "__none__";
    }

    private int compareResult(GlobalSearchResultVO left, GlobalSearchResultVO right) {
        int timeCompare = compareTimeDesc(resolveSortTime(left), resolveSortTime(right));
        if (timeCompare != 0) {
            return timeCompare;
        }
        String leftTitle = left.getTitle() == null ? "" : left.getTitle();
        String rightTitle = right.getTitle() == null ? "" : right.getTitle();
        return leftTitle.compareToIgnoreCase(rightTitle);
    }

    private void normalizeResult(GlobalSearchResultVO result) {
        if (result == null) {
            return;
        }
        if (result.getEntityType() == null) {
            result.setEntityType(result.getType());
        }
        if (result.getEntityId() == null) {
            result.setEntityId(result.getRecordId());
        }
        if (result.getSummary() == null) {
            result.setSummary(result.getContent());
        }
        if (result.getSortTime() == null) {
            result.setSortTime(resolveSortTime(result));
        }
        if (result.getRoutePath() == null) {
            result.setRoutePath(resolveRoutePath(result));
        }
    }

    private String resolveRoutePath(GlobalSearchResultVO result) {
        String type = result.getEntityType() == null ? result.getType() : result.getEntityType();
        Long id = result.getEntityId() == null ? result.getRecordId() : result.getEntityId();
        if (type == null || id == null) {
            return "/chat";
        }
        return switch (type) {
            case "customer" -> "/customer/" + id;
            case "contact" -> result.getCustomerId() == null
                    ? "/customer"
                    : "/customer/" + result.getCustomerId() + "?openContactId=" + id;
            case "task" -> "/task?openTaskId=" + id;
            case "schedule" -> "/calendar?openScheduleId=" + id;
            case "knowledge" -> "/knowledge?openKnowledgeId=" + id;
            case "relation" -> "/relation?openRelationId=" + id;
            case "product" -> "/product?openProductId=" + id;
            default -> "/chat";
        };
    }

    private LocalDateTime resolveSortTime(GlobalSearchResultVO result) {
        if (result == null) {
            return null;
        }
        if (result.getEventTime() != null) {
            return result.getEventTime();
        }
        if (result.getUpdateTime() != null) {
            return result.getUpdateTime();
        }
        return result.getCreateTime();
    }

    private int compareTimeDesc(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }
}
