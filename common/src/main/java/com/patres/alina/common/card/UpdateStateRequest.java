package com.patres.alina.common.card;


public record UpdateStateRequest(
        String id,
        State state
) {

    public UpdateStateRequest(String id,
                              boolean enabled) {
        this(id, enabled ? State.ENABLED : State.DISABLED);
    }

}
