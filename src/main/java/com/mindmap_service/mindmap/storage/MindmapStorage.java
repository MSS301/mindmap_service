package com.mindmap_service.mindmap.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap_service.mindmap.dto.MindmapPromptRequest;
import com.mindmap_service.mindmap.dto.MindmapUpdateRequest;
import com.mindmap_service.mindmap.exception.MindmapStorageException;
import com.mindmap_service.mindmap.model.MindmapMetadata;
import com.mindmap_service.mindmap.repository.MindmapMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MindmapStorage {

    private final MindmapStorageProperties properties;
    private final ObjectMapper objectMapper;
    private final MindmapMetadataRepository metadataRepository;

    private Path basePath;

    @PostConstruct
    void setup() {
        try {
            basePath = determineBasePath();
            Files.createDirectories(basePath);
        } catch (IOException ex) {
            throw new MindmapStorageException("Failed to initialize mindmap storage", ex);
        }
    }

    public synchronized MindmapMetadata saveMindmap(MindmapPromptRequest request, JsonNode content) {
        try {
            UUID id = UUID.randomUUID();
            UUID userId = request.getUserId();
            Instant now = Instant.now();
            String filename = id + ".json";
            Path file = userDirectory(userId).resolve(filename);
            Files.createDirectories(file.getParent());
            writeMindmapToFile(content, file);

            MindmapMetadata metadata = MindmapMetadata.builder()
                    .id(id)
                    .userId(userId)
                    .name(request.getName())
                    .prompt(request.getPrompt())
                    .project(request.getProject())
                    .status("READY")
                    .createdAt(now)
                    .updatedAt(now)
                    .filename(filename)
                    .build();
            return metadataRepository.save(metadata);
        } catch (IOException ex) {
            throw new MindmapStorageException("Unable to persist mindmap file", ex);
        }
    }

    public Optional<MindmapMetadata> find(UUID userId, UUID mindmapId) {
        return metadataRepository.findById(mindmapId).filter(metadata -> metadata.getUserId()
                .equals(userId));
    }

    public Optional<MindmapMetadata> findById(UUID mindmapId) {
        return metadataRepository.findById(mindmapId);
    }

    public List<MindmapMetadata> listByUser(UUID userId) {
        return metadataRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<MindmapMetadata> listAll() {
        return metadataRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public JsonNode readMindmap(MindmapMetadata metadata) {
        Path file = userDirectory(metadata.getUserId()).resolve(metadata.getFilename());
        System.out.println(metadata.getFilename());
        System.out.println(file.toString());
        try {
            return objectMapper.readTree(file.toFile());
        } catch (IOException ex) {
            throw new MindmapStorageException("Failed to read mindmap content", ex);
        }
    }

    public synchronized MindmapMetadata updateMindmap(
            MindmapMetadata existing, MindmapUpdateRequest updates, JsonNode content) {
        try {
            MindmapMetadata updated = existing.toBuilder()
                    .name(updates.getName() != null ? updates.getName() : existing.getName())
                    .project(updates.getProject() != null ? updates.getProject() : existing.getProject())
                    .prompt(updates.getPrompt() != null ? updates.getPrompt() : existing.getPrompt())
                    .status("READY")
                    .updatedAt(Instant.now())
                    .build();

            if (content != null) {
                Path file = userDirectory(updated.getUserId()).resolve(updated.getFilename());
                writeMindmapToFile(content, file);
            }
            return metadataRepository.save(updated);
        } catch (IOException ex) {
            throw new MindmapStorageException("Unable to update mindmap file", ex);
        }
    }

    public synchronized void deleteMindmap(UUID userId, UUID mindmapId) {
        Optional<MindmapMetadata> metadataOpt = metadataRepository.findById(mindmapId);
        if (metadataOpt.isEmpty()) {
            return;
        }
        MindmapMetadata metadata = metadataOpt.get();
        if (!metadata.getUserId().equals(userId)) {
            return;
        }
        try {
            Path file = userDirectory(userId).resolve(metadata.getFilename());
            Files.deleteIfExists(file);
            metadataRepository.delete(metadata);
        } catch (IOException ex) {
            throw new MindmapStorageException("Unable to delete mindmap file", ex);
        }
    }

    public synchronized void deleteMindmap(UUID mindmapId) {
        Optional<MindmapMetadata> metadataOpt = metadataRepository.findById(mindmapId);
        if (metadataOpt.isEmpty()) {
            return;
        }
        MindmapMetadata metadata = metadataOpt.get();
        try {
            Path file = userDirectory(metadata.getUserId()).resolve(metadata.getFilename());
            Files.deleteIfExists(file);
            metadataRepository.delete(metadata);
        } catch (IOException ex) {
            throw new MindmapStorageException("Unable to delete mindmap file", ex);
        }
    }

    private void writeMindmapToFile(JsonNode content, Path file) throws IOException {
        String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content);
        Files.writeString(file, payload, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private Path userDirectory(UUID userId) {
        return basePath.resolve(userId.toString());
    }

    private Path determineBasePath() {
        Path rawConfigured = properties.basePath();
        Path configured = rawConfigured.toAbsolutePath().normalize();
        Optional<MindmapMetadata> reference = metadataRepository.findTopByOrderByCreatedAtDesc();
        if (reference.isEmpty()) {
            return configured;
        }

        MindmapMetadata sample = reference.get();
        if (Files.exists(fileLocation(configured, sample))) {
            return configured;
        }

        if (!rawConfigured.isAbsolute()) {
            Optional<Path> fallback = locateExistingStorageRoot(rawConfigured.normalize(), sample);
            if (fallback.isPresent()) {
                Path resolved = fallback.get();
                log.info(
                        "Detected existing mindmap payloads under {}. Using it instead of configured directory {}.",
                        resolved,
                        configured);
                return resolved;
            }
        }
        return configured;
    }

    private Optional<Path> locateExistingStorageRoot(Path relativeBaseDir, MindmapMetadata sample) {
        Path current = Path.of("").toAbsolutePath().getParent();
        while (current != null) {
            Path candidate = current.resolve(relativeBaseDir).normalize();
            if (Files.exists(fileLocation(candidate, sample))) {
                return Optional.of(candidate);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private Path fileLocation(Path base, MindmapMetadata metadata) {
        return base.resolve(metadata.getUserId().toString()).resolve(metadata.getFilename());
    }
}
