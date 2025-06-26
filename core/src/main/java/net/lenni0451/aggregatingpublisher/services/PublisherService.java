package net.lenni0451.aggregatingpublisher.services;

import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.utils.ProgressConsumer;

import java.io.IOException;
import java.util.Map;

public interface PublisherService {

    default void load(final AggregatingPublisher publisher) {
    }

    String getName();

    void publish(final Map<String, byte[]> deployment, final ProgressConsumer progressConsumer) throws IOException;

}
