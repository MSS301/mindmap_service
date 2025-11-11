package com.mindmap_service.mindmap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductTokenRequest {

    @JsonProperty("user_id")
    private String userId;

    private Integer tokens;

    private String description;

    @JsonProperty("reference_type")
    private String referenceType;

    @JsonProperty("reference_id")
    private String referenceId;
}
