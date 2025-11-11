package com.mindmap_service.mindmap.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "mindmap.ai.gemini")
public class GeminiProperties {

    private String url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";
    private String apiKey;
    private String promptTemplate =
            """
			Generate a mindmap payload in JSON with the following structure:
			{
				"title": "<summary>",
				"description": "<short description>",
				"nodes": [
					{"id": "<uuid>", "label": "<topic>", "parentId": "<parent-id>", "notes": "<extra>"}
				]
			}
			Always respond with valid JSON without markdown fences.
			Project: {project}
			UserId: {userId}
			Prompt: {prompt}
			""";
}
