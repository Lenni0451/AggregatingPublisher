package net.lenni0451.aggregatingpublisher.webui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.services.PublisherService;
import net.lenni0451.aggregatingpublisher.web.RequestHandler;
import net.lenni0451.aggregatingpublisher.web.RequestInfo;
import net.lenni0451.aggregatingpublisher.web.ResponseInfo;
import net.lenni0451.commons.gson.GsonParser;
import net.lenni0451.commons.gson.elements.GsonObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
public class WebInterfaceHandler extends RequestHandler {

    private final String username;
    private final String password;
    private final WebFileCollector fileCollector;
    private final AggregatingPublisher publisher;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, PublishTask> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();

    @Override
    public ResponseInfo handle(RequestInfo request) throws Throwable {
        String path = request.uri().getPath();

        if (path.equals("/login")) {
            if (request.method().equalsIgnoreCase("POST")) {
                return handleLogin(request);
            } else {
                return serveResource("web/login.html", "text/html");
            }
        }

        if (path.startsWith("/static/")) {
            String resourcePath = "web/" + path.substring(8); // Remove /static/
            // Prevent directory traversal
            if (resourcePath.contains("..")) return ResponseInfo.methodNotAllowed();
            String contentType = getContentType(resourcePath);
            return serveResource(resourcePath, contentType);
        }

        // Check authentication for other endpoints
        if (!isAuthenticated(request)) {
            if (path.startsWith("/api/")) {
                return ResponseInfo.of(401, "Unauthorized");
            } else {
                Map<String, List<String>> headers = new HashMap<>();
                headers.put("Location", Collections.singletonList("/login"));
                return ResponseInfo.of(302, headers, new byte[0]);
            }
        }

        if (path.equals("/")) {
            return serveResource("web/index.html", "text/html");
        }

        if (path.equals("/api/artifacts")) {
            return handleArtifacts();
        }

        if (path.equals("/api/publishers")) {
            return handlePublishers();
        }

        if (path.equals("/api/clear") && request.method().equalsIgnoreCase("POST")) {
            this.fileCollector.clearFiles();
            return ResponseInfo.success("{\"status\":\"ok\"}");
        }

        if (path.startsWith("/api/publish/")) {
            String publisherName = path.substring(13); // Remove /api/publish/
            return handlePublish(publisherName);
        }

        if (path.startsWith("/api/progress/")) {
            String taskId = path.substring(14); // Remove /api/progress/
            return handleProgress(taskId);
        }

        if (path.equals("/api/logout")) {
             return handleLogout(request);
        }

        return ResponseInfo.notFound();
    }

    private ResponseInfo handleLogin(RequestInfo request) throws Throwable {
        String body = new String(request.body().readAllBytes(), StandardCharsets.UTF_8);
        // Expecting x-www-form-urlencoded or json? Let's assume json for simplicity as we will write the frontend.
        // Or form data from the login page. Let's do JSON.
        try {
            GsonObject json = GsonParser.parse(body).asObject();
            String u = json.getString("username");
            String p = json.getString("password");

            if (this.username.equals(u) && this.password.equals(p)) {
                String token = UUID.randomUUID().toString();
                this.sessions.put(token, u);
                Map<String, List<String>> headers = new HashMap<>();
                headers.put("Set-Cookie", Collections.singletonList("session=" + token + "; Path=/; HttpOnly; SameSite=Strict"));
                return ResponseInfo.of(200, headers, "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.debug("Login failed", e);
        }
        return ResponseInfo.of(401, "Invalid credentials");
    }

    private ResponseInfo handleLogout(RequestInfo request) {
        String token = getSessionToken(request);
        if (token != null) {
            this.sessions.remove(token);
        }
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Set-Cookie", Collections.singletonList("session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"));
         return ResponseInfo.of(200, headers, "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
    }

    private boolean isAuthenticated(RequestInfo request) {
        String token = getSessionToken(request);
        return token != null && this.sessions.containsKey(token);
    }

    private String getSessionToken(RequestInfo request) {
        List<String> cookieHeaders = request.headers().get("Cookie");
        if (cookieHeaders != null) {
            for (String header : cookieHeaders) {
                for (String cookie : header.split(";")) {
                    cookie = cookie.trim();
                    if (cookie.startsWith("session=")) {
                        return cookie.substring(8);
                    }
                }
            }
        }
        return null;
    }

    private ResponseInfo handleArtifacts() {
        Map<String, byte[]> files = this.fileCollector.getFiles();
        JsonArray array = new JsonArray();
        for (String path : files.keySet()) {
            array.add(path);
        }
        return ResponseInfo.success(gson.toJson(array));
    }

    private ResponseInfo handlePublishers() {
        JsonArray array = new JsonArray();
        for (PublisherService service : this.publisher.getPublisherServices()) {
             array.add(service.getName());
        }
        return ResponseInfo.success(gson.toJson(array));
    }

    private ResponseInfo handlePublish(String publisherName) {
        // Find publisher
        PublisherService target = null;
        for (PublisherService service : this.publisher.getPublisherServices()) {
            if (service.getName().equals(publisherName)) {
                target = service;
                break;
            }
        }

        if (target == null) {
            return ResponseInfo.notFound();
        }

        String taskId = UUID.randomUUID().toString();
        PublishTask task = new PublishTask();
        this.tasks.put(taskId, task);

        PublisherService finalTarget = target;
        this.executorService.submit(() -> {
            try {
                finalTarget.publish(this.fileCollector.getFiles(), (progress, step, totalSteps) -> {
                    task.progress = progress;
                    task.step = step;
                    task.totalSteps = totalSteps;
                });
                task.completed = true;
                task.success = true;
            } catch (Throwable t) {
                log.error("Publishing failed", t);
                task.completed = true;
                task.success = false;
                task.error = t.getMessage();
            }
        });

        JsonObject json = new JsonObject();
        json.addProperty("taskId", taskId);
        return ResponseInfo.success(gson.toJson(json));
    }

    private ResponseInfo handleProgress(String taskId) {
        PublishTask task = this.tasks.get(taskId);
        if (task == null) return ResponseInfo.notFound();

        JsonObject json = new JsonObject();
        json.addProperty("progress", task.progress);
        json.addProperty("step", task.step);
        json.addProperty("totalSteps", task.totalSteps);
        json.addProperty("completed", task.completed);
        json.addProperty("success", task.success);
        json.addProperty("error", task.error);

        return ResponseInfo.success(gson.toJson(json));
    }

    private ResponseInfo serveResource(String path, String contentType) throws Throwable {
        try (InputStream is = WebInterfaceHandler.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) return ResponseInfo.notFound();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.transferTo(baos);
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Content-Type", Collections.singletonList(contentType));
            return ResponseInfo.of(200, headers, baos.toByteArray());
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private static class PublishTask {
        volatile float progress;
        volatile int step;
        volatile int totalSteps;
        volatile boolean completed;
        volatile boolean success;
        volatile String error;
    }

}
