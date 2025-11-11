package com.mindmap_service.mindmap.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapPromptRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String prompt;

    private String name;

    private String project;
}
