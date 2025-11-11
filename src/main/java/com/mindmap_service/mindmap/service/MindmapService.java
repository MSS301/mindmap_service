package com.mindmap_service.mindmap.service;

import java.util.List;
import java.util.UUID;

import com.mindmap_service.mindmap.dto.MindmapPromptRequest;
import com.mindmap_service.mindmap.dto.MindmapResponse;
import com.mindmap_service.mindmap.dto.MindmapSummary;
import com.mindmap_service.mindmap.dto.MindmapUpdateRequest;

public interface MindmapService {

    MindmapResponse createMindmap(MindmapPromptRequest request);

    List<MindmapSummary> listMindmaps(UUID userId);

    List<MindmapSummary> listAllMindmaps();

    MindmapResponse getMindmap(UUID userId, UUID mindmapId);

    MindmapResponse getMindmapById(UUID mindmapId);

    MindmapResponse updateMindmap(UUID userId, UUID mindmapId, MindmapUpdateRequest request);

    void deleteMindmap(UUID userId, UUID mindmapId);

    void deleteMindmapById(UUID mindmapId);

    void notifyManually(UUID userId, UUID mindmapId);
}
