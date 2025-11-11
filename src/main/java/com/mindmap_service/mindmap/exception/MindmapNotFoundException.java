package com.mindmap_service.mindmap.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MindmapNotFoundException extends RuntimeException {

    public MindmapNotFoundException(String message) {
        super(message);
    }
}
