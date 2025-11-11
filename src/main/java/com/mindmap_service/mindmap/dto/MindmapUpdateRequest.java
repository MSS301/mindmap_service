package com.mindmap_service.mindmap.dto;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MindmapUpdateRequest {

    private String prompt;

    private String name;

    private String project;

    public boolean hasChanges() {
        return StringUtils.hasText(prompt) || StringUtils.hasText(name) || StringUtils.hasText(project);
    }
}
