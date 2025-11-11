package com.mindmap_service.mindmap.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MindmapSummary {

    private UUID id;
    private UUID userId;
    private String name;
    private String prompt;
    private String project;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private String filename;
}
