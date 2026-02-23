package net.lenni0451.aggregatingpublisher.webui;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.services.PublisherService;
import net.lenni0451.aggregatingpublisher.web.RequestHandler;
import net.lenni0451.aggregatingpublisher.web.RequestInfo;
import net.lenni0451.aggregatingpublisher.web.ResponseInfo;
import net.lenni0451.commons.gson.GsonCollectors;
import net.lenni0451.commons.gson.GsonParser;
import net.lenni0451.commons.gson.elements.GsonArray;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.gson.elements.GsonPrimitive;

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

    @Override
    public ResponseInfo handle(RequestInfo request) throws Throwable {
        String path = request.uri().getPath();

        if (path.equals("/login")) {
            if (request.method().equalsIgnoreCase("POST")) {
                return this.handleLogin(request);
            } else if (request.method().equalsIgnoreCase("GET")) {
                return this.serveResource("web/login.html", "text/html");
            } else {
                return ResponseInfo.methodNotAllowed();
            }
        } else if (path.startsWith("/static/")) {
            if (request.method().equalsIgnoreCase("GET")) {
                String resourcePath = "web/" + path.substring(8); // Remove /static/
                // Prevent directory traversal
                if (resourcePath.contains("..")) return ResponseInfo.notFound();
                String contentType = this.getContentType(resourcePath);
                return this.serveResource(resourcePath, contentType);
            } else {
                return ResponseInfo.methodNotAllowed();
            }
        }

        if (this.isAuthenticated(request)) {
            if (path.equals("/")) {
                return this.serveResource("web/index.html", "text/html");
            } else if (path.equals("/api/artifacts")) {
                return this.handleArtifacts();
            } else if (path.equals("/api/publishers")) {
                return this.handlePublishers();
            } else if (path.equals("/api/clear")) {
                if (request.method().equalsIgnoreCase("POST")) {
                    this.fileCollector.clearFiles();
                    return ResponseInfo.success(new GsonObject().add("status", "ok").toString());
                } else {
                    return ResponseInfo.methodNotAllowed();
                }
            } else if (path.startsWith("/api/publish/")) {
                String publisherName = path.substring(13); // Remove /api/publish/
                return this.handlePublish(publisherName);
            } else if (path.startsWith("/api/progress/")) {
                String taskId = path.substring(14); // Remove /api/progress/
                return this.handleProgress(taskId);
            } else if (path.equals("/api/logout")) {
                return this.handleLogout(request);
            }
            return ResponseInfo.notFound();
        }

        if (request.method().equalsIgnoreCase("GET")) {
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Location", Collections.singletonList("/login"));
            return ResponseInfo.of(302, headers, new byte[0]);
        } else {
            return ResponseInfo.methodNotAllowed();
        }
    }

    private ResponseInfo handleLogin(final RequestInfo request) throws Throwable {
        String body = new String(request.body().readAllBytes(), StandardCharsets.UTF_8);
        try {
            GsonObject json = GsonParser.parse(body).asObject();
            String username = json.getString("username");
            String password = json.getString("password");
            if (this.username.equals(username) && this.password.equals(password)) {
                String token = UUID.randomUUID().toString();
                this.sessions.put(token, username);
                Map<String, List<String>> headers = new HashMap<>();
                headers.put("Set-Cookie", List.of("session=" + token + "; Path=/; HttpOnly; SameSite=Strict"));
                return ResponseInfo.of(200, headers, new GsonObject().add("status", "ok").toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable t) {
            log.debug("Login failed", t);
        }
        return ResponseInfo.of(401, "Invalid credentials");
    }

    private ResponseInfo handleLogout(final RequestInfo request) {
        String token = this.getSessionToken(request);
        if (token != null) {
            this.sessions.remove(token);
        }
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Set-Cookie", Collections.singletonList("session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"));
        return ResponseInfo.of(200, headers, new GsonObject().add("status", "ok").toString().getBytes(StandardCharsets.UTF_8));
    }

    private boolean isAuthenticated(final RequestInfo request) {
        String token = this.getSessionToken(request);
        return token != null && this.sessions.containsKey(token);
    }

    private String getSessionToken(final RequestInfo request) {
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
        GsonArray artifacts = this.fileCollector.getFiles().keySet().stream()
                .map(GsonPrimitive::new)
                .collect(GsonCollectors.toArray());
        return ResponseInfo.success(artifacts.toString());
    }

    private ResponseInfo handlePublishers() {
        GsonArray publishers = this.publisher.getPublisherServices().stream()
                .map(PublisherService::getName)
                .map(GsonPrimitive::new)
                .collect(GsonCollectors.toArray());
        return ResponseInfo.success(publishers.toString());
    }

    private ResponseInfo handlePublish(final String publisherName) {
        PublisherService target = this.publisher.getPublisherServices().stream()
                .filter(publisher -> publisher.getName().equals(publisherName))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return ResponseInfo.notFound();
        }

        String taskId = UUID.randomUUID().toString();
        PublishTask task = new PublishTask();
        this.tasks.put(taskId, task);
        this.executorService.submit(() -> {
            try {
                target.publish(this.fileCollector.getFiles(), (progress, step, totalSteps) -> {
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

        GsonObject json = new GsonObject();
        json.add("taskId", taskId);
        return ResponseInfo.success(json.toString());
    }

    private ResponseInfo handleProgress(final String taskId) {
        PublishTask task = this.tasks.get(taskId);
        if (task == null) return ResponseInfo.notFound();

        GsonObject json = new GsonObject();
        json.add("progress", task.progress);
        json.add("step", task.step);
        json.add("totalSteps", task.totalSteps);
        json.add("completed", task.completed);
        json.add("success", task.success);
        json.add("error", task.error);
        return ResponseInfo.success(json.toString());
    }

    private ResponseInfo serveResource(final String path, final String contentType) throws Throwable {
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
