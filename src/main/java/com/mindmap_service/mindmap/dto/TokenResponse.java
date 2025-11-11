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
public class TokenResponse {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("token_before")
    private Integer tokenBefore;

    @JsonProperty("token_after")
    private Integer tokenAfter;

    @JsonProperty("tokens_deducted")
    private Integer tokensDeducted;

    @JsonProperty("transaction_id")
    private Long transactionId;

    private String status;

    private String message;
}
