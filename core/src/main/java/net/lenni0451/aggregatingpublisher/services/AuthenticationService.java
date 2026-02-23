package net.lenni0451.aggregatingpublisher.services;

import net.lenni0451.aggregatingpublisher.AggregatingPublisher;

public interface AuthenticationService {

    default void load(final AggregatingPublisher publisher) {
    }

    boolean verify(final String authHeader);

}
