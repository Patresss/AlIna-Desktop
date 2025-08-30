package com.patres.alina.uidesktop.messagecontext;

import java.nio.file.Path;

public class MessageContextException extends RuntimeException {

    public MessageContextException(Path path, Exception cause) {
        super("Message context exception from path: " + path.toString(), cause);
    }

    public MessageContextException(Exception cause) {
        super("Message context exception", cause);
    }
}
