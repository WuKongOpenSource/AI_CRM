package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.service.ImConversationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImConversationServiceImplTest {

    @Test
    void directMemberKeyIsOrderIndependent() {
        assertThat(ImConversationService.directMemberKey(7L, 3L)).isEqualTo("3_7");
        assertThat(ImConversationService.directMemberKey(3L, 7L)).isEqualTo("3_7");
    }
}
