package net.dzikoysk.openapi;

import java.io.Serializable;

public final class EntityDto implements Serializable {

    private final int status;
    private final String message;

    public EntityDto(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

}
