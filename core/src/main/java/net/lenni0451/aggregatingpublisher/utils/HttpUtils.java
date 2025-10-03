package net.lenni0451.aggregatingpublisher.utils;

import net.lenni0451.aggregatingpublisher.auth.Authentication;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.content.HttpContent;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.lenni0451.commons.httpclient.requests.impl.PutRequest;

import javax.annotation.Nullable;
import java.io.IOException;

public class HttpUtils {

    private static final int TIMEOUT = 60_000;
    private static final HttpClient HTTP_CLIENT = new HttpClient()
            .setReadTimeout(TIMEOUT)
            .setConnectTimeout(TIMEOUT);

    public static byte[] get(final String url, @Nullable final Authentication authentication) throws IOException {
        HttpRequest request = new GetRequest(url);
        if (authentication != null) authentication.apply(request);
        return HTTP_CLIENT.execute(request, httpResponse -> {
            if (httpResponse.getStatusCode() / 100 != 2) return null;
            return httpResponse.getContent();
        });
    }

    public static void put(final String url, final byte[] data, @Nullable final Authentication authentication) throws IOException {
        HttpRequest request = new PutRequest(url).setContent(HttpContent.bytes(data));
        if (authentication != null) authentication.apply(request);
        HTTP_CLIENT.execute(request, httpResponse -> {
            if (httpResponse.getStatusCode() / 100 != 2) {
                throw new IOException("Failed to upload file: " + httpResponse.getStatusCode() + " " + httpResponse.getStatusMessage());
            }
            return null;
        });
    }

    public static HttpResponse execute(final HttpRequest request, @Nullable final Authentication authentication) throws IOException {
        if (authentication != null) authentication.apply(request);
        return HTTP_CLIENT.execute(request, httpResponse -> {
            if (httpResponse.getStatusCode() / 100 != 2) {
                throw new IOException("Failed to execute request: " + httpResponse.getStatusCode() + " " + httpResponse.getStatusMessage());
            }
            return httpResponse;
        });
    }

}
