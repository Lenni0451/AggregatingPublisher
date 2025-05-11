package net.lenni0451.aggregatingpublisher.auth;

import net.lenni0451.commons.httpclient.HeaderStore;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;

public record TokenAuth(String token) implements Authentication {

    @Override
    public void apply(HeaderStore<?> headers) {
        headers.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
    }

}
