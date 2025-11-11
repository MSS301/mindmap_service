package com.mindmap_service.mindmap.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mindmap_service.mindmap.dto.MindmapPromptRequest;
import com.mindmap_service.mindmap.dto.MindmapResponse;
import com.mindmap_service.mindmap.dto.MindmapSummary;
import com.mindmap_service.mindmap.dto.MindmapUpdateRequest;
import com.mindmap_service.mindmap.service.MindmapService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mindmaps")
@RequiredArgsConstructor
@Tag(name = "Mindmap AI", description = "Generate and retrieve mindmap JSON payloads from Gemini")
public class MindmapController {

    private final MindmapService mindmapService;

    @Operation(summary = "Generate a mindmap JSON from a user prompt")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MindmapResponse createMindmap(@Valid @RequestBody MindmapPromptRequest request) {
        return mindmapService.createMindmap(request);
    }

    @Operation(summary = "List all mindmaps (admin only)")
    @GetMapping("/admin")
    public List<MindmapSummary> listAllMindmaps() {
        return mindmapService.listAllMindmaps();
    }

    @Operation(summary = "Fetch a mindmap by id without a user filter")
    @GetMapping("/admin/{mindmapId}")
    public MindmapResponse getMindmapAdmin(@PathVariable UUID mindmapId) {
        return mindmapService.getMindmapById(mindmapId);
    }

    @Operation(summary = "Remove a mindmap by id (admin)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/admin/{mindmapId}")
    public void deleteMindmapAdmin(@PathVariable UUID mindmapId) {
        mindmapService.deleteMindmapById(mindmapId);
    }

    @Operation(summary = "List all generated mindmaps for a user")
    @GetMapping("/users/{userId}")
    public List<MindmapSummary> listMindmaps(@PathVariable UUID userId) {
        return mindmapService.listMindmaps(userId);
    }

    @Operation(summary = "Retrieve a generated mindmap payload by its id")
    @GetMapping("/{mindmapId}")
    public MindmapResponse getMindmap(@PathVariable UUID mindmapId) {
        return mindmapService.getMindmapById(mindmapId);
    }

    @Operation(summary = "Retrieve a generated mindmap payload")
    @GetMapping("/users/{userId}/{mindmapId}")
    public MindmapResponse getMindmap(@PathVariable UUID userId, @PathVariable UUID mindmapId) {
        return mindmapService.getMindmap(userId, mindmapId);
    }

    @Operation(summary = "Update a mindmap prompt or metadata")
    @PutMapping("/users/{userId}/{mindmapId}")
    public MindmapResponse updateMindmap(
            @PathVariable UUID userId, @PathVariable UUID mindmapId, @Valid @RequestBody MindmapUpdateRequest request) {
        return mindmapService.updateMindmap(userId, mindmapId, request);
    }

    @Operation(summary = "Delete a mindmap record")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/users/{userId}/{mindmapId}")
    public void deleteMindmap(@PathVariable UUID userId, @PathVariable UUID mindmapId) {
        mindmapService.deleteMindmap(userId, mindmapId);
    }

    @Operation(summary = "Trigger a notification for a mindmap")
    @PostMapping("/users/{userId}/{mindmapId}/notify")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void notifyUser(@PathVariable UUID userId, @PathVariable UUID mindmapId) {
        mindmapService.notifyManually(userId, mindmapId);
    }

    @Operation(summary = "Download a mindmap as JSON file")
    @GetMapping("/users/{userId}/{mindmapId}/download")
    public ResponseEntity<byte[]> downloadMindmap(@PathVariable UUID userId, @PathVariable UUID mindmapId) {
        byte[] content = mindmapService.downloadMindmap(userId, mindmapId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mindmap-" + mindmapId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(content);
    }

    @Operation(summary = "Download a mindmap by id (admin)")
    @GetMapping("/admin/{mindmapId}/download")
    public ResponseEntity<byte[]> downloadMindmapAdmin(@PathVariable UUID mindmapId) {
        byte[] content = mindmapService.downloadMindmapById(mindmapId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mindmap-" + mindmapId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(content);
    }
}
