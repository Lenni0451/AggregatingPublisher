package net.lenni0451.aggregatingpublisher.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HttpBasicAuthenticator implements AuthenticationService {

    private final String encoded;

    public HttpBasicAuthenticator(final String encoded) {
        this.encoded = encoded;
    }

    public HttpBasicAuthenticator(final String username, final String password) {
        this.encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean verify(final String authHeader) {
        if (authHeader == null) return false;
        if (!authHeader.startsWith("Basic ")) return false;
        String token = authHeader.substring(6);
        return this.encoded.equals(token);
    }

}
