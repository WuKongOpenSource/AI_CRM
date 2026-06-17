package com.kakarote.ai_crm.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SingleInstallSchemaTest {

    @Test
    void baselineSchemaContainsCustomFieldTables() throws IOException {
        String schema = resource("sql/crm_init_postgres.sql");

        assertThat(schema).contains("CREATE TABLE crm_custom_field");
        assertThat(schema).contains("CREATE TABLE crm_custom_field_pool");
        assertThat(schema).contains("CREATE TABLE crm_custom_field_sort");
    }

    @Test
    void latestSingleInstallMigrationSeedsDefaultFieldsAndAddressBookDetailPermission() throws IOException {
        String migration = resource("db/migration/V37__single_default_fields_and_permissions.sql");

        assertThat(migration).contains("addressBook:detail");
        assertThat(migration).contains("'customer', 'companyName'");
        assertThat(migration).contains("'customer', 'stage'");
        assertThat(migration).contains("'contact', 'name'");
        assertThat(migration).contains("'relation', 'relationType'");
        assertThat(migration).contains("'product', 'productName'");
    }

    @Test
    void latestWeKnoraMigrationStopsDisabledSeedFromOverridingRuntimeConfig() throws IOException {
        String migration = resource("db/migration/V38__single_weknora_runtime_defaults.sql");

        assertThat(migration).contains("weknora_enabled");
        assertThat(migration).contains("config_value = ''");
        assertThat(migration).contains("WEKNORA");
    }

    private static String resource(String path) throws IOException {
        try (var input = SingleInstallSchemaTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
