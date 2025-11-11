# Mindmap AI Service

The mindmap service receives natural language prompts, asks Google Gemini to build a structured mindmap JSON, and persists the payload scoped by user. It ships with OpenAPI documentation, file-based storage, and an optional hook to notify another notification service once a mindmap is ready.

## Features

- `POST /api/mindmaps` — accept a prompt, call Gemini, persist the resulting mindmap JSON, and return metadata plus the JSON payload.
- Consistent JSON validation (title/nodes) before persisting so every mindmap can be rendered reliably.
- File-based storage under `mindmap.storage.base-dir`, keeping one `.json` per mindmap plus a metadata index.
- `GET/PUT/DELETE /api/mindmaps/users/{userId}/{mindmapId}` for retrieval, regeneration, and removal.
- `GET /api/mindmaps/users/{userId}` to list metadata for a user.
- `POST /api/mindmaps/users/{userId}/{mindmapId}/notify` to manually push a notification (auto-invoked on generation).
- Optional notification bridge configurable via `mindmap.notification.*` to call another service when the mindmap is ready.
- Swagger UI and OpenAPI docs under `/v3/api-docs` and `/swagger-ui.html`.

## API Overview

- `POST /api/mindmaps` – body: `{ "userId": "...", "prompt": "...", "name": "...", "project": "..." }`
- `GET /api/mindmaps/users/{userId}` – list all mindmap summaries for the user.
- `GET /api/mindmaps/users/{userId}/{mindmapId}` – fetch a single mindmap payload.
- `PUT /api/mindmaps/users/{userId}/{mindmapId}` – update prompt/name/project; regenerates JSON when a prompt is provided.
- `DELETE /api/mindmaps/users/{userId}/{mindmapId}` – delete metadata and stored payload.
- `POST /api/mindmaps/users/{userId}/{mindmapId}/notify` – trigger the optional notification bridge.

## Storage layout

By default mindmaps are stored under `./data/mindmaps`. Each user receives a directory named after their UUID; every file is named `{mindmapId}.json`. A shared `mindmaps-index.json` file mirrors metadata so the service can rebuild its index on restart.

## Configuration

Key configuration lives in `src/main/resources/application.properties`:

```properties
spring.application.name=mindmap-service
server.port=8086
server.servlet.context-path=/mindmap-service

mindmap.storage.base-dir=./data/mindmaps
mindmap.ai.gemini.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5:generateText
mindmap.ai.gemini.temperature=0.15
mindmap.ai.gemini.max-output-tokens=1500
mindmap.ai.gemini.api-key=${GEMINI_API_KEY:}

# Optional notification bridge
mindmap.notification.url=
mindmap.notification.api-key=
mindmap.notification.template=A new mindmap "{name}" created from: {prompt}
mindmap.notification.title=Mindmap ready for you
```

The Gemini API key can be injected via environment variables such as `GEMINI_API_KEY`. Populate `mindmap.notification.url` (and optionally `api-key`) to call your notification service once a mindmap is ready.

## Building and running

```bash
./mvnw clean package
./mvnw spring-boot:run
```

Swagger UI: `http://localhost:8086/mindmap-service/swagger-ui.html`

## Next steps

1. Wire an actual notification endpoint by filling `mindmap.notification.url`.
2. Add security or rate limiting if the service is exposed publicly.
3. Replace the Gemini client with a streaming implementation if payloads need to be streamed directly to a front-end viewer.
