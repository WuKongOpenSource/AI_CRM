package com.kakarote.ai_crm.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TencentMeetingSignerTest {

    @Test
    void signShouldFollowTencentMeetingAkSkAlgorithm() {
        String signature = TencentMeetingSigner.sign(
                "test-secret-id",
                "test-secret-key",
                "GET",
                "123456",
                "1700000000",
                "/v1/meetings?userid=user1&instanceid=1",
                ""
        );

        assertThat(signature)
                .isEqualTo("YmUzZjMxZTE1ZGNjNmMxODdkYjUzNTFhZDA0MWMzZmRmM2QwNTliMWY0NWQ1MWU2MTM2NzY0ODVkZTUxYjQ3Ng==");
    }
}
