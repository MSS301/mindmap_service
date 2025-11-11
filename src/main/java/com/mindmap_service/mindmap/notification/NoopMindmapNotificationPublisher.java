package com.mindmap_service.mindmap.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.mindmap_service.mindmap.model.MindmapMetadata;

@Component
@ConditionalOnMissingBean(MindmapNotificationPublisher.class)
public class NoopMindmapNotificationPublisher implements MindmapNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopMindmapNotificationPublisher.class);

    @Override
    public void notifyMindmapReady(MindmapMetadata metadata) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Mindmap {} for user {} would be notified but no publisher is configured",
                    metadata.getId(),
                    metadata.getUserId());
        }
    }
}
