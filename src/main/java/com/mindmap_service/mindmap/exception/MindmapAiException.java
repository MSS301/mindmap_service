package com.mindmap_service.mindmap.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class MindmapAiException extends RuntimeException {

    public MindmapAiException(String message) {
        super(message);
    }

    public MindmapAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
