package com.kakarote.ai_crm.im.ws;

import java.security.Principal;

/** WebSocket session principal whose name is the user id (as String). */
public record ImPrincipal(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
