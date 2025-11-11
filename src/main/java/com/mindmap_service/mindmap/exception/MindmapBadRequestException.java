package com.mindmap_service.mindmap.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MindmapBadRequestException extends RuntimeException {

    public MindmapBadRequestException(String message) {
        super(message);
    }
}
