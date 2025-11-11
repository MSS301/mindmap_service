package com.mindmap_service.mindmap.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mindmap_service.mindmap.config.FeignConfig;
import com.mindmap_service.mindmap.dto.DeductTokenRequest;
import com.mindmap_service.mindmap.dto.TokenResponse;
import com.mindmap_service.mindmap.dto.WalletApiResponse;

@FeignClient(name = "wallet-service", path = "/wallet/internal/wallets", configuration = FeignConfig.class)
public interface WalletFeignClient {

    @PostMapping("/deduct-token")
    WalletApiResponse<TokenResponse> deductToken(@RequestBody DeductTokenRequest request);
}
