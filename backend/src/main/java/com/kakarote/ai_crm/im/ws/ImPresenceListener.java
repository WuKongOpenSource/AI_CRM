package com.kakarote.ai_crm.im.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class ImPresenceListener {

    @Autowired
    private ImPresenceService presenceService;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String userId = principalName(event.getUser());
        if (userId != null) {
            presenceService.markOnline(userId);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        // SessionDisconnectEvent has no getUser(); unwrap the principal from the raw message.
        Principal user = StompHeaderAccessor.wrap(event.getMessage()).getUser();
        String userId = principalName(user);
        if (userId != null) {
            presenceService.markOffline(userId);
        }
    }

    private String principalName(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
