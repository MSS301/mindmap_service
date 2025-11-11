package com.mindmap_service.mindmap.notification;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.mindmap_service.mindmap.model.MindmapMetadata;

import lombok.Builder;
import lombok.Data;

@Component
@ConditionalOnProperty(prefix = "mindmap.notification", name = "url")
public class HttpMindmapNotificationPublisher implements MindmapNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(HttpMindmapNotificationPublisher.class);

    private final MindmapNotificationProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public HttpMindmapNotificationPublisher(MindmapNotificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void notifyMindmapReady(MindmapMetadata metadata) {
        if (!StringUtils.hasText(properties.getUrl())) {
            log.warn("Notification URL is not configured; skipping alerts for mindmap {}", metadata.getId());
            return;
        }

        MindmapNotificationPayload payload = buildPayload(metadata);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(properties.getApiKey())) {
            headers.set(properties.getApiKeyHeader(), properties.getApiKey());
        }

        HttpEntity<MindmapNotificationPayload> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.postForEntity(properties.getUrl(), request, Void.class);
            if (log.isDebugEnabled()) {
                log.debug("Notified external notification service for mindmap {}", metadata.getId());
            }
        } catch (RestClientException ex) {
            log.warn(
                    "Failed to notify external notification service for mindmap {}: {}",
                    metadata.getId(),
                    ex.getMessage());
        }
    }

    private MindmapNotificationPayload buildPayload(MindmapMetadata metadata) {
        String message = properties
                .getTemplate()
                .replace("{name}", metadata.getName() != null ? metadata.getName() : "Untitled mindmap")
                .replace("{prompt}", metadata.getPrompt());
        return MindmapNotificationPayload.builder()
                .userId(metadata.getUserId())
                .mindmapId(metadata.getId())
                .title(properties.getTitle())
                .message(message)
                .build();
    }

    @Data
    @Builder
    private static class MindmapNotificationPayload {

        private UUID userId;
        private UUID mindmapId;
        private String title;
        private String message;
    }
}
