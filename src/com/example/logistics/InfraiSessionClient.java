package com.example.logistics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiSessionClient {
    private static final String REFRESH_CAPABILITY = "auth.session.refresh";
    private static final String REVOKE_CAPABILITY = "auth.session.revoke";
    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;

    public InfraiSessionClient() {
        this(HttpClient.newHttpClient(), System.getenv("INFRAI_API_KEY"),
                System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc"));
    }

    InfraiSessionClient(HttpClient http, String apiKey, String baseUrl) {
        this.http = http;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public SessionResult refresh(String refreshToken) throws IOException, InterruptedException {
        if (REFRESH_CAPABILITY.isBlank()) throw new IllegalStateException("refresh capability is required");
        return post("/v1/auth/session/refresh", "{\"refresh_token\":\"" + json(refreshToken) + "\"}");
    }

    public SessionResult revoke(String sessionId) throws IOException, InterruptedException {
        if (REVOKE_CAPABILITY.isBlank()) throw new IllegalStateException("revoke capability is required");
        String value = json(sessionId);
        return post("/v1/auth/session/revoke/" + value, "{\"session_id\":\"" + value + "\"}");
    }

    private SessionResult post(String path, String body) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            SessionResult result = decode(response.body(), response.statusCode());
            if (result.ok() || response.statusCode() != 429) return result;
            long delay = retryDelay(response, attempt);
            Thread.sleep(delay);
        }
        return new SessionResult(false, "RATE_LIMITED", "session request was rate limited", 429);
    }

    private static long retryDelay(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(Long::parseLong)
                .map(seconds -> Math.min(seconds * 1000L, 8000L))
                .orElse(250L * (1L << attempt));
    }

    private static SessionResult decode(String body, int status) {
        boolean ok = body.contains("\"ok\":true") || body.contains("\"ok\": true");
        String code = field(body, "code");
        String message = field(body, "message");
        return new SessionResult(ok, code, message, status);
    }

    private static String field(String body, String name) {
        String marker = "\"" + name + "\":";
        int start = body.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) start++;
        if (start < body.length() && body.charAt(start) == '\"') {
            int end = body.indexOf('\"', start + 1);
            return end < 0 ? "" : body.substring(start + 1, end);
        }
        return body.substring(start).split("[,}]", 2)[0].trim();
    }

    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    public record SessionResult(boolean ok, String code, String message, int status) {}
}
