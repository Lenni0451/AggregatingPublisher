package net.lenni0451.aggregatingpublisher.web;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lenni0451.aggregatingpublisher.utils.EmptyInputStream;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseInfo {

    public static ResponseInfo success(@Nullable final String response) {
        if (response == null) {
            return of(200, Collections.emptyMap(), -1, EmptyInputStream.INSTANCE);
        } else {
            return of(200, response);
        }
    }

    public static ResponseInfo notFound() {
        return of(404, "Not Found");
    }

    public static ResponseInfo methodNotAllowed() {
        return of(405, "Method Not Allowed");
    }

    public static ResponseInfo of(final int statusCode, final String body) {
        return of(statusCode, Map.of("Content-Type", List.of("text/plain")), body.getBytes(StandardCharsets.UTF_8));
    }

    public static ResponseInfo of(final int statusCode, final Map<String, List<String>> headers, final byte[] body) {
        return new ResponseInfo(statusCode, headers, body.length, new ByteArrayInputStream(body));
    }

    public static ResponseInfo of(final int statusCode, final Map<String, List<String>> headers, final int contentLength, final InputStream body) {
        return new ResponseInfo(statusCode, headers, contentLength, body);
    }


    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final int contentLength;
    private final InputStream body;

}
