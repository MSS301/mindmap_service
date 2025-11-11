package com.mindmap_service.mindmap.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mindmap_service.mindmap.model.MindmapMetadata;

public interface MindmapMetadataRepository extends JpaRepository<MindmapMetadata, UUID> {

    List<MindmapMetadata> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<MindmapMetadata> findTopByOrderByCreatedAtDesc();
}
