package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.BO.CustomerFieldFilterBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerResolvedFieldFilterBO;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiSearchParseVO;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerServiceImplTest {

    @Test
    void parseFollowedCustomerSearchUsesNotEmptyFilter() {
        CustomerServiceImpl service = new CustomerServiceImpl();
        String response = """
                {
                  "parsedQuery": {
                    "lastContactStart": "2026-05-23 11:09:48",
                    "lastContactEnd": "2026-05-23 11:09:48"
                  },
                  "explanation": "已跟进客户",
                  "confidence": 0.8
                }
                """;

        CustomerAiSearchParseVO result = ReflectionTestUtils.invokeMethod(
                service,
                "parseCustomerAiSearchResponse",
                response,
                "已跟进客户"
        );

        assertNotNull(result);
        assertNull(result.getParsedQuery().getLastContactStart());
        assertNull(result.getParsedQuery().getLastContactEnd());
        assertNotNull(result.getParsedQuery().getFilters());
        assertEquals(1, result.getParsedQuery().getFilters().size());
        CustomerFieldFilterBO filter = result.getParsedQuery().getFilters().getFirst();
        assertEquals("lastContactTime", filter.getFieldName());
        assertEquals("system", filter.getFieldSource());
        assertEquals("isNotEmpty", filter.getOperator());
        assertTrue(result.getDisplayChips().stream()
                .anyMatch(chip -> "filter:system:lastContactTime:isNotEmpty".equals(chip.getKey())
                        && chip.getLabel().contains("已跟进")));
    }

    @Test
    void parseDaysWithoutFollowUpKeepsOriginalRangeLogic() {
        CustomerServiceImpl service = new CustomerServiceImpl();
        String response = """
                {
                  "parsedQuery": {},
                  "explanation": "30天未跟进客户",
                  "confidence": 0.8
                }
                """;

        CustomerAiSearchParseVO result = ReflectionTestUtils.invokeMethod(
                service,
                "parseCustomerAiSearchResponse",
                response,
                "30天未跟进客户"
        );

        assertNotNull(result);
        assertNotNull(result.getParsedQuery().getLastContactEnd());
        assertEquals(Boolean.TRUE, result.getParsedQuery().getIncludeNoLastContact());
        assertTrue(result.getParsedQuery().getFilters() == null || result.getParsedQuery().getFilters().isEmpty());
    }

    @Test
    void resolveSystemFieldEmptyFilters() {
        CustomerServiceImpl service = new CustomerServiceImpl();
        IDynamicSchemaService dynamicSchemaService = mock(IDynamicSchemaService.class);
        ReflectionTestUtils.setField(service, "dynamicSchemaService", dynamicSchemaService);
        when(dynamicSchemaService.columnExists("crm_customer", "last_contact_time")).thenReturn(true);

        List<CustomerResolvedFieldFilterBO> filters = invokeResolveFilters(
                service,
                List.of(
                        filter("lastContactTime", "system", "isNotEmpty"),
                        filter("lastContactTime", "system", "isEmpty"),
                        filter("unknownField", "system", "isNotEmpty")
                )
        );

        assertEquals(2, filters.size());
        assertEquals("last_contact_time", filters.get(0).getColumnName());
        assertEquals("isNotEmpty", filters.get(0).getOperator());
        assertEquals("nullOnly", filters.get(0).getEmptyMode());
        assertEquals("isEmpty", filters.get(1).getOperator());
    }

    @Test
    void resolveCustomFieldEmptyFiltersUsesFieldMetadata() {
        CustomerServiceImpl service = new CustomerServiceImpl();
        ICustomFieldService customFieldService = mock(ICustomFieldService.class);
        IDynamicSchemaService dynamicSchemaService = mock(IDynamicSchemaService.class);
        ReflectionTestUtils.setField(service, "customFieldService", customFieldService);
        ReflectionTestUtils.setField(service, "dynamicSchemaService", dynamicSchemaService);

        CustomFieldVO textField = customField("field_abcd12", "文本字段", "text");
        CustomFieldVO multiField = customField("field_ef3456", "多选字段", "multiselect");
        when(customFieldService.getEnabledFieldsByEntity("customer")).thenReturn(List.of(textField, multiField));
        when(dynamicSchemaService.columnExists("crm_customer", "field_abcd12")).thenReturn(true);
        when(dynamicSchemaService.columnExists("crm_customer", "field_ef3456")).thenReturn(true);

        List<CustomerResolvedFieldFilterBO> filters = invokeResolveFilters(
                service,
                List.of(
                        filter("field_abcd12", "custom", "isEmpty"),
                        filter("field_ef3456", "custom", "isNotEmpty"),
                        filter("field_missing", "custom", "isNotEmpty")
                )
        );

        assertEquals(2, filters.size());
        assertEquals("field_abcd12", filters.get(0).getColumnName());
        assertEquals("blank", filters.get(0).getEmptyMode());
        assertEquals("field_ef3456", filters.get(1).getColumnName());
        assertEquals("jsonArray", filters.get(1).getEmptyMode());
    }

    private static List<CustomerResolvedFieldFilterBO> invokeResolveFilters(CustomerServiceImpl service,
                                                                            List<CustomerFieldFilterBO> filters) {
        CustomerQueryBO queryBO = new CustomerQueryBO();
        queryBO.setFilters(filters);
        ReflectionTestUtils.invokeMethod(
                service,
                "resolveCustomerFieldFilters",
                queryBO
        );
        return queryBO.getResolvedFieldFilters() == null ? List.of() : queryBO.getResolvedFieldFilters();
    }

    private static CustomerFieldFilterBO filter(String fieldName, String fieldSource, String operator) {
        CustomerFieldFilterBO filter = new CustomerFieldFilterBO();
        filter.setFieldName(fieldName);
        filter.setFieldSource(fieldSource);
        filter.setOperator(operator);
        return filter;
    }

    private static CustomFieldVO customField(String fieldName, String label, String fieldType) {
        CustomFieldVO field = new CustomFieldVO();
        field.setEntityType("customer");
        field.setFieldName(fieldName);
        field.setFieldLabel(label);
        field.setFieldSource("custom");
        field.setFieldType(fieldType);
        field.setColumnName(fieldName);
        field.setStatus(1);
        return field;
    }
}
