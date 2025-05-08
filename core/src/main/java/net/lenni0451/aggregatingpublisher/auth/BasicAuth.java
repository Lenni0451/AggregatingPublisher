package net.lenni0451.aggregatingpublisher.auth;

import net.lenni0451.commons.httpclient.HeaderStore;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record BasicAuth(String username, String password) implements Authentication {

    @Override
    public void apply(HeaderStore<?> headers) {
        headers.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8)));
    }

}
