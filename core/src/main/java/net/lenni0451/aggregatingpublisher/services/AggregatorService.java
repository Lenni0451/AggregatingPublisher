package net.lenni0451.aggregatingpublisher.services;

import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.web.RequestHandler;

public interface AggregatorService {

    default void load(final AggregatingPublisher publisher) {
    }

    String getName();

    RequestHandler getHandler();

}
