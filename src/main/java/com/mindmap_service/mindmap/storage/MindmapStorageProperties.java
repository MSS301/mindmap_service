package com.mindmap_service.mindmap.storage;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "mindmap.storage")
public class MindmapStorageProperties {

    private String baseDir = "data/mindmaps";

    public Path basePath() {
        return Path.of(baseDir);
    }
}
