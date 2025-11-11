package com.mindmap_service.mindmap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MindmapResponse {

    private MindmapSummary metadata;
    private JsonNode mindmap;
}
