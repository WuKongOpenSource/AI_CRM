package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.im.event.ImMessageSentEvent;
import com.kakarote.ai_crm.service.ImMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImRealtimePushListenerTest {

    @Mock ImMessageService messageService;
    @Mock ImPushService pushService;
    ImRealtimePushListener listener;

    @BeforeEach
    void setUp() {
        listener = new ImRealtimePushListener();
        ReflectionTestUtils.setField(listener, "messageService", messageService);
        ReflectionTestUtils.setField(listener, "pushService", pushService);
    }

    @Test
    void onMessageSentPushesVoToMembers() {
        ImMessageVO vo = new ImMessageVO();
        vo.setId("100");
        when(messageService.getMessageVO(100L)).thenReturn(vo);

        listener.onMessageSent(new ImMessageSentEvent(10L, 100L, List.of(1L, 2L)));

        verify(pushService).pushMessage(eq(10L), eq(vo), eq(List.of(1L, 2L)));
    }
}
