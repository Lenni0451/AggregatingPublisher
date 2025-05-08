package net.lenni0451.aggregatingpublisher.auth;

import net.lenni0451.commons.httpclient.HeaderStore;

public interface Authentication {

    void apply(final HeaderStore<?> headers);

}
