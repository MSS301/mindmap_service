package com.mindmap_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import com.mindmap_service.mindmap.client.GeminiProperties;
import com.mindmap_service.mindmap.notification.MindmapNotificationProperties;
import com.mindmap_service.mindmap.storage.MindmapStorageProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties({
    MindmapStorageProperties.class,
    GeminiProperties.class,
    MindmapNotificationProperties.class
})
public class MindmapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindmapServiceApplication.class, args);
    }
}
