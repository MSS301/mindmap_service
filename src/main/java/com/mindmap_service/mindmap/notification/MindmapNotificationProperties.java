package com.mindmap_service.mindmap.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "mindmap.notification")
public class MindmapNotificationProperties {

    private String url;
    private String apiKey;
    private String apiKeyHeader = "X-API-KEY";
    private String title = "Your mindmap is ready";
    private String template = "A new mindmap \"{name}\" was created from prompt: {prompt}";
}
