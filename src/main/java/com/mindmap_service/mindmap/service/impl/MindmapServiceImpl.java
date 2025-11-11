package com.mindmap_service.mindmap.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap_service.mindmap.client.GeminiClient;
import com.mindmap_service.mindmap.client.WalletFeignClient;
import com.mindmap_service.mindmap.dto.DeductTokenRequest;
import com.mindmap_service.mindmap.dto.MindmapPromptRequest;
import com.mindmap_service.mindmap.dto.MindmapResponse;
import com.mindmap_service.mindmap.dto.MindmapSummary;
import com.mindmap_service.mindmap.dto.MindmapUpdateRequest;
import com.mindmap_service.mindmap.dto.TokenResponse;
import com.mindmap_service.mindmap.dto.WalletApiResponse;
import com.mindmap_service.mindmap.exception.MindmapBadRequestException;
import com.mindmap_service.mindmap.exception.MindmapNotFoundException;
import com.mindmap_service.mindmap.model.MindmapMetadata;
import com.mindmap_service.mindmap.notification.MindmapNotificationPublisher;
import com.mindmap_service.mindmap.service.MindmapService;
import com.mindmap_service.mindmap.storage.MindmapStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MindmapServiceImpl implements MindmapService {

    private final GeminiClient geminiClient;
    private final MindmapStorage storage;
    private final MindmapNotificationPublisher notificationPublisher;
    private final ObjectMapper objectMapper;
    private final WalletFeignClient walletFeignClient;

    public MindmapResponse createMindmap(MindmapPromptRequest request) {
        // Deduct 1 token from wallet before generating mindmap
        UUID mindmapId = UUID.randomUUID();

        DeductTokenRequest deductRequest = DeductTokenRequest.builder()
                .userId(request.getUserId().toString())
                .tokens(1)
                .description("Generate mindmap: " + (request.getName() != null ? request.getName() : "New mindmap"))
                .referenceType("MINDMAP_GENERATION")
                .referenceId(mindmapId.toString())
                .build();

        try {
            WalletApiResponse<TokenResponse> response = walletFeignClient.deductToken(deductRequest);
            log.info(
                    "Token deducted for user {}. Remaining tokens: {}",
                    request.getUserId(),
                    response.getResult().getTokenAfter());
        } catch (Exception e) {
            log.error("Failed to deduct token for user {}: {}", request.getUserId(), e.getMessage());
            throw new MindmapBadRequestException("Cannot create mindmap: " + e.getMessage());
        }

        // Generate mindmap after token deduction succeeds
        JsonNode mindmap = geminiClient.generateMindmap(request.getUserId(), request.getPrompt(), request.getProject());
        MindmapMetadata metadata = storage.saveMindmap(request, mindmap);
        notificationPublisher.notifyMindmapReady(metadata);
        return toResponse(metadata, mindmap);
    }

    public List<MindmapSummary> listMindmaps(UUID userId) {
        return storage.listByUser(userId).stream().map(this::toSummary).collect(Collectors.toList());
    }

    public List<MindmapSummary> listAllMindmaps() {
        return storage.listAll().stream().map(this::toSummary).collect(Collectors.toList());
    }

    public MindmapResponse getMindmap(UUID userId, UUID mindmapId) {
        MindmapMetadata metadata =
                storage.find(userId, mindmapId).orElseThrow(() -> new MindmapNotFoundException("Mindmap not found"));
        JsonNode mindmap = storage.readMindmap(metadata);
        return toResponse(metadata, mindmap);
    }

    public MindmapResponse getMindmapById(UUID mindmapId) {
        MindmapMetadata metadata =
                storage.findById(mindmapId).orElseThrow(() -> new MindmapNotFoundException("Mindmap not found"));
        JsonNode mindmap = storage.readMindmap(metadata);
        return toResponse(metadata, mindmap);
    }

    public MindmapResponse updateMindmap(UUID userId, UUID mindmapId, MindmapUpdateRequest request) {
        if (!request.hasChanges()) {
            throw new MindmapBadRequestException("Update must include prompt, name, or project");
        }
        MindmapMetadata existing =
                storage.find(userId, mindmapId).orElseThrow(() -> new MindmapNotFoundException("Mindmap not found"));
        JsonNode mindmap = storage.readMindmap(existing);
        boolean regenerated = false;
        if (request.getPrompt() != null) {
            mindmap = geminiClient.generateMindmap(userId, request.getPrompt(), request.getProject());
            regenerated = true;
        }
        MindmapMetadata updated = storage.updateMindmap(existing, request, mindmap);
        if (regenerated) {
            notificationPublisher.notifyMindmapReady(updated);
        }
        return toResponse(updated, mindmap);
    }

    public void deleteMindmap(UUID userId, UUID mindmapId) {
        storage.deleteMindmap(userId, mindmapId);
    }

    public void deleteMindmapById(UUID mindmapId) {
        storage.deleteMindmap(mindmapId);
    }

    public void notifyManually(UUID userId, UUID mindmapId) {
        MindmapMetadata metadata =
                storage.find(userId, mindmapId).orElseThrow(() -> new MindmapNotFoundException("Mindmap not found"));
        notificationPublisher.notifyMindmapReady(metadata);
    }

    public byte[] downloadMindmap(UUID userId, UUID mindmapId) {
        MindmapMetadata metadata =
                storage.find(userId, mindmapId).orElseThrow(() -> new MindmapNotFoundException("Mindmap not found"));
        JsonNode mindmap = storage.readMindmap(metadata);
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mindmap);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize mindmap for download", e);
        }
    }

    public byte[] downloadMindmapById(UUID mindmapId) {
        MindmapMetadata metadata =
                storage.findById(mindmapId).orElseThrow(() -> new MindmapNotFoundException("Mindmap not found"));
        JsonNode mindmap = storage.readMindmap(metadata);
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mindmap);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize mindmap for download", e);
        }
    }

    private MindmapResponse toResponse(MindmapMetadata metadata, JsonNode mindmap) {
        return MindmapResponse.builder()
                .metadata(toSummary(metadata))
                .mindmap(mindmap)
                .build();
    }

    private MindmapSummary toSummary(MindmapMetadata metadata) {
        return MindmapSummary.builder()
                .id(metadata.getId())
                .userId(metadata.getUserId())
                .name(metadata.getName())
                .prompt(metadata.getPrompt())
                .project(metadata.getProject())
                .status(metadata.getStatus())
                .createdAt(metadata.getCreatedAt())
                .updatedAt(metadata.getUpdatedAt())
                .filename(metadata.getFilename())
                .build();
    }
}
