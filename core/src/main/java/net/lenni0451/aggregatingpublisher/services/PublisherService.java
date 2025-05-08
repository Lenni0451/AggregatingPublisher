package net.lenni0451.aggregatingpublisher.services;

import net.lenni0451.aggregatingpublisher.AggregatingPublisher;

import java.io.IOException;
import java.util.Map;

public interface PublisherService {

    default void load(final AggregatingPublisher publisher) {
    }

    String getName();

    void publish(final Map<String, byte[]> deployment) throws IOException;

}
