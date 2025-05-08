package net.lenni0451.aggregatingpublisher.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;

public record RequestInfo(InetSocketAddress remoteAddress, InetSocketAddress localAddress, URI uri, String protocol, String method, Map<String, List<String>> headers,
        InputStream body) {

    public static RequestInfo of(final HttpExchange exchange) {
        return new RequestInfo(
                exchange.getRemoteAddress(),
                exchange.getLocalAddress(),
                exchange.getRequestURI(),
                exchange.getProtocol(),
                exchange.getRequestMethod(),
                exchange.getRequestHeaders(),
                exchange.getRequestBody()
        );
    }

}
