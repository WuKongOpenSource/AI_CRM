package com.kakarote.ai_crm.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeStatusEnumTest {

    @Test
    void shouldResolveConfiguredEmployeeStatuses() {
        assertThat(EmployeeStatusEnum.getName("active")).isEqualTo("在职");
        assertThat(EmployeeStatusEnum.getName("resigned")).isEqualTo("离职");
        assertThat(EmployeeStatusEnum.getName("disabled")).isEqualTo("停用");
    }

    @Test
    void shouldDefaultBlankOrUnknownEmployeeStatusToActive() {
        assertThat(EmployeeStatusEnum.normalize(null)).isEqualTo("active");
        assertThat(EmployeeStatusEnum.normalize("")).isEqualTo("active");
        assertThat(EmployeeStatusEnum.normalize("unknown")).isEqualTo("active");
    }
}
