package com.kakarote.ai_crm.service.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferenceSupportTest {

    @Test
    void normalizeSidebarModuleOrderReturnsDefaultForMissingInput() {
        assertThat(UserPreferenceSupport.normalizeSidebarModuleOrder(null))
                .containsExactly("recent", "customer", "project", "relation", "addressBook");
        assertThat(UserPreferenceSupport.normalizeSidebarModuleOrder(List.of()))
                .containsExactly("recent", "customer", "project", "relation", "addressBook");
    }

    @Test
    void normalizeSidebarModuleOrderKeepsKnownUniqueKeysAndAppendsMissingDefaults() {
        List<String> normalized = UserPreferenceSupport.normalizeSidebarModuleOrder(
                List.of("customer", "recent", "customer", "unknown", "project"));

        assertThat(normalized)
                .containsExactly("customer", "recent", "project", "relation", "addressBook");
    }

    @Test
    void serializeAndParsePreferencesRoundTripsNormalizedSidebarOrder() {
        String json = UserPreferenceSupport.serializePreferences(
                List.of("relation", "addressBook", "recent", "customer", "project"));

        assertThat(UserPreferenceSupport.parsePreferences(json).getSidebarModuleOrder())
                .containsExactly("relation", "addressBook", "recent", "customer", "project");
    }

    @Test
    void parsePreferencesNormalizesInvalidStoredJson() {
        String json = """
                {"sidebarModuleOrder":["addressBook","bad","addressBook","customer"]}
                """;

        assertThat(UserPreferenceSupport.parsePreferences(json).getSidebarModuleOrder())
                .containsExactly("addressBook", "customer", "recent", "project", "relation");
    }
}
