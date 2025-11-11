package com.mindmap_service.mindmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Response wrapper from Wallet Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletApiResponse<T> {
    private int code;
    private String message;
    private T result;
}
