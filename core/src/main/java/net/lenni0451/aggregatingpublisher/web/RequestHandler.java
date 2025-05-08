package net.lenni0451.aggregatingpublisher.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class RequestHandler implements HttpHandler {

    @Override
    public final void handle(HttpExchange exchange) {
        try (exchange) {
            ResponseInfo responseInfo = this.handle(RequestInfo.of(exchange));
            if (responseInfo == null) {
                log.warn("ResponseInfo is null, returning 500");
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().write("Internal Server Error".getBytes(StandardCharsets.UTF_8));
            } else {
                exchange.getResponseHeaders().putAll(responseInfo.getHeaders());
                exchange.sendResponseHeaders(responseInfo.getStatusCode(), responseInfo.getContentLength());
                responseInfo.getBody().transferTo(exchange.getResponseBody());
            }
        } catch (Throwable t) {
            log.error("Error handling request", t);
        }
    }

    public abstract ResponseInfo handle(final RequestInfo request) throws Throwable;

}
