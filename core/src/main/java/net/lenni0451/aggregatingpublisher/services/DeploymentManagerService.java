package net.lenni0451.aggregatingpublisher.services;

import net.lenni0451.aggregatingpublisher.AggregatingPublisher;

public interface DeploymentManagerService {

    default void load(final AggregatingPublisher publisher) {
    }

    void aggregateFile(final String path, final byte[] file);

}
