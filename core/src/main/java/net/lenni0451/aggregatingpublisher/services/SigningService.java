package net.lenni0451.aggregatingpublisher.services;

import net.lenni0451.aggregatingpublisher.AggregatingPublisher;

public interface SigningService {

    default void load(final AggregatingPublisher publisher) {
    }

    String getName();

    byte[] sign(final String path, final byte[] data) throws Exception;

}
