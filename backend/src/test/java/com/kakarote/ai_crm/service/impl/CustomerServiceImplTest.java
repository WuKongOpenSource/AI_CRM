package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.BO.CustomerFieldUpdateBO;
import com.kakarote.ai_crm.entity.BO.CustomerFieldFilterBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerResolvedFieldFilterBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiReportVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiSearchParseVO;
import com.kakarote.ai_crm.entity.VO.CustomerAiSearchQueryVO;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.CustomerTagMapper;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IDynamicSchemaService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.ITaskService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceImplTest {

    @Test
    void generateAiReportReturnsFallbackReportWhenAiUnavailable() {
        CustomerServiceImpl service = new CustomerServiceImpl();
        Customer customer = new Customer();
        customer.setCustomerId(100L);
        customer.setCompanyName("悟空科技");
        customer.setIndustry("制造业");
        customer.setStage("proposal");
        customer.setLevel("A");
        customer.setQuotation(new BigDecimal("800000"));
        customer.setStatus(1);

        CustomerAiReportVO report = ReflectionTestUtils.invokeMethod(service, "buildFallbackCustomerAiReport", customer);

        assertNotNull(report);
        assertEquals(100L, report.getCustomerId());
        assertNotEquals("pending", report.getAiStatusDetection());
        assertFalse(report.getAiInsight().startsWith("No AI analysis"));
        assertNotNull(report.getAiDeepInsight());
        assertNotNull(report.getAiNextStep());
    }

    @Test
    void updateCustomerFieldUpdatesPrimaryContactPhone() {
        CustomerServiceImpl service = spy(new CustomerServiceImpl());
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        ContactMapper contactMapper = mock(ContactMapper.class);
        CustomerTagMapper customerTagMapper = mock(CustomerTagMapper.class);
        TaskMapper taskMapper = mock(TaskMapper.class);
        ICustomFieldService customFieldService = mock(ICustomFieldService.class);
        IGlobalSearchIndexService globalSearchIndexService = mock(IGlobalSearchIndexService.class);

        ReflectionTestUtils.setField(service, "baseMapper", customerMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "customerTagMapper", customerTagMapper);
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "customFieldService", customFieldService);
        ReflectionTestUtils.setField(service, "globalSearchIndexService", globalSearchIndexService);

        Long customerId = 100L;
        Long contactId = 200L;
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setCompanyName("Acme");
        customer.setPrimaryContactName("Alice");

        Contact primary = new Contact();
        primary.setContactId(contactId);
        primary.setCustomerId(customerId);
        primary.setIsPrimary(1);
        primary.setStatus(1);
        primary.setName("Alice");
        primary.setPhone("old");

        CustomerDetailVO detail = new CustomerDetailVO();
        detail.setCustomerId(customerId);
        detail.setCompanyName("Acme");

        when(customerMapper.selectById(customerId)).thenReturn(customer);
        when(customerMapper.getCustomerById(customerId)).thenReturn(detail);
        when(contactMapper.selectList(any())).thenReturn(List.of(primary));
        when(customerTagMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(customFieldService.getCustomFieldValues("customer", customerId)).thenReturn(Map.of());
        doNothing().when(service).syncContactCache(customerId);

        CustomerFieldUpdateBO updateBO = new CustomerFieldUpdateBO();
        updateBO.setCustomerId(customerId);
        updateBO.setFieldName("primaryContactPhone");
        updateBO.setFieldSource("contact");
        updateBO.setValue("13800000000");

        CustomerDetailVO result = assertDoesNotThrow(() -> service.updateCustomerField(updateBO));

        assertSame(detail, result);
        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);
        verify(contactMapper).updateById(contactCaptor.capture());
        assertEquals("13800000000", contactCaptor.getValue().getPhone());
        verify(service).syncContactCache(customerId);
        verify(globalSearchIndexService).refreshContactIndex(contactId);
    }

    @Test
    void deleteCustomerClearsSearchIndexesAndRefreshesRelatedPriorities() {
        CustomerServiceImpl service = new CustomerServiceImpl();
        CustomerMapper customerMapper = mock(CustomerMapper.class);
        ContactMapper contactMapper = mock(ContactMapper.class);
        CustomerTagMapper customerTagMapper = mock(CustomerTagMapper.class);
        IGlobalSearchIndexService globalSearchIndexService = mock(IGlobalSearchIndexService.class);
        ITaskService taskService = mock(ITaskService.class);
        CustomerLogoService customerLogoService = mock(CustomerLogoService.class);

        ReflectionTestUtils.setField(service, "baseMapper", customerMapper);
        ReflectionTestUtils.setField(service, "contactMapper", contactMapper);
        ReflectionTestUtils.setField(service, "customerTagMapper", customerTagMapper);
        ReflectionTestUtils.setField(service, "globalSearchIndexService", globalSearchIndexService);
        ReflectionTestUtils.setField(service, "taskService", taskService);
        ReflectionTestUtils.setField(service, "customerLogoService", customerLogoService);

        Long customerId = 100L;
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setLogo("customer/logo/acme.png");
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        service.deleteCustomer(customerId);

        verify(customerMapper).deleteById(customerId);
        verify(customerLogoService).deleteStoredLogoQuietly("customer/logo/acme.png");
        verify(contactMapper).delete(any());
        verify(customerTagMapper).delete(any());
        verify(globalSearchIndexService).deleteByEntity("customer", customerId);
        verify(globalSearchIndexService).deleteContactIndexesByCustomerId(customerId);
        verify(globalSearchIndexService).refreshCustomerRelatedIndexes(customerId);
        verify(taskService).refreshValuePriorityByCustomerId(customerId);
    }

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
    void heuristicSearchParsesChineseAmountComparison() {
        CustomerServiceImpl service = new CustomerServiceImpl();

        CustomerAiSearchQueryVO query = ReflectionTestUtils.invokeMethod(
                service,
                "buildHeuristicSearchQuery",
                "预计成交金额大于50万的客户"
        );

        assertNotNull(query);
        assertEquals(new BigDecimal("500000"), query.getQuotationMin());
        assertNull(query.getQuotationMax());
        assertEquals("quotation", query.getSortBy());
        assertEquals("desc", query.getSortOrder());
    }

    @Test
    void heuristicSearchParsesChineseAmountRange() {
        CustomerServiceImpl service = new CustomerServiceImpl();

        CustomerAiSearchQueryVO query = ReflectionTestUtils.invokeMethod(
                service,
                "buildHeuristicSearchQuery",
                "预计成交金额50到100万的客户"
        );

        assertNotNull(query);
        assertEquals(new BigDecimal("500000"), query.getQuotationMin());
        assertEquals(new BigDecimal("1000000"), query.getQuotationMax());
    }

    @Test
    void parseSearchDropsImplicitZeroAmountBeforeHeuristics() {
        CustomerServiceImpl service = new CustomerServiceImpl();
        String response = """
                {
                  "parsedQuery": {
                    "quotationMin": 0
                  },
                  "explanation": "高价值客户",
                  "confidence": 0.8
                }
                """;

        CustomerAiSearchParseVO result = ReflectionTestUtils.invokeMethod(
                service,
                "parseCustomerAiSearchResponse",
                response,
                "高价值客户"
        );

        assertNotNull(result);
        assertEquals(new BigDecimal("500000"), result.getParsedQuery().getQuotationMin());
        assertNull(result.getParsedQuery().getQuotationMax());
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
