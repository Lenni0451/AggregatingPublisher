package net.lenni0451.aggregatingpublisher.aggregator.maven;

import lombok.Getter;
import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.services.AggregatorService;
import net.lenni0451.aggregatingpublisher.web.RequestHandler;

public class MavenAggregator implements AggregatorService {

    @Getter
    private AggregatingPublisher aggregatingPublisher;

    @Override
    public void load(AggregatingPublisher publisher) {
        this.aggregatingPublisher = publisher;
    }

    @Override
    public String getName() {
        return "maven";
    }

    @Override
    public RequestHandler getHandler() {
        return new MavenRequestHandler(this);
    }

}
