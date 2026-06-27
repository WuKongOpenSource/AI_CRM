package com.kakarote.ai_crm.im.ws;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.config.security.service.TokenService;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/** Authenticates the STOMP CONNECT frame using the Manager-Token native header. */
@Component
public class ImStompAuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Manager-Token");
            LoginUser loginUser = StrUtil.isBlank(token) ? null : tokenService.getLoginUser(token);
            if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getUserId() == null) {
                throw new MessagingException("IM 鉴权失败：无效或过期的登录令牌");
            }
            // Build a new mutable accessor so we can set the user principal
            StompHeaderAccessor mutableAccessor = StompHeaderAccessor.wrap(message);
            mutableAccessor.setLeaveMutable(true);
            mutableAccessor.setUser(new ImPrincipal(String.valueOf(loginUser.getUser().getUserId())));
            return MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
        }
        return message;
    }
}
