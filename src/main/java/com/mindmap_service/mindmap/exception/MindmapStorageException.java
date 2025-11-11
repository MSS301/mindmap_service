package com.mindmap_service.mindmap.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class MindmapStorageException extends RuntimeException {

    public MindmapStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
