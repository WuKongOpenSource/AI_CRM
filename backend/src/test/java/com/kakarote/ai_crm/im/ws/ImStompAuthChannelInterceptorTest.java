package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.config.security.service.TokenService;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImStompAuthChannelInterceptorTest {

    @Mock TokenService tokenService;
    ImStompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ImStompAuthChannelInterceptor();
        ReflectionTestUtils.setField(interceptor, "tokenService", tokenService);
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (token != null) {
            accessor.addNativeHeader("Manager-Token", token);
        }
        return org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void validTokenSetsPrincipal() {
        ManagerUser u = new ManagerUser();
        u.setUserId(42L);
        LoginUser lu = new LoginUser();
        lu.setUser(u);
        when(tokenService.getLoginUser("good")).thenReturn(lu);

        Message<?> out = interceptor.preSend(connectMessage("good"), null);

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertThat(acc.getUser()).isNotNull();
        assertThat(acc.getUser().getName()).isEqualTo("42");
    }

    @Test
    void invalidTokenRejected() {
        when(tokenService.getLoginUser("bad")).thenReturn(null);
        assertThatThrownBy(() -> interceptor.preSend(connectMessage("bad"), null))
                .isInstanceOf(org.springframework.messaging.MessagingException.class);
    }
}
