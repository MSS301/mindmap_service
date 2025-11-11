package com.mindmap_service.mindmap.notification;

import com.mindmap_service.mindmap.model.MindmapMetadata;

public interface MindmapNotificationPublisher {

    void notifyMindmapReady(MindmapMetadata metadata);
}
