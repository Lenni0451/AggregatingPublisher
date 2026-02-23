package net.lenni0451.aggregatingpublisher;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.services.AggregatorService;
import net.lenni0451.aggregatingpublisher.services.AuthenticationService;
import net.lenni0451.aggregatingpublisher.services.DeploymentManagerService;
import net.lenni0451.aggregatingpublisher.services.PublisherService;
import net.lenni0451.aggregatingpublisher.web.WebServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class AggregatingPublisher {

    @Getter
    private final WebServer webServer;
    private final List<AggregatorService> aggregatorServices = new ArrayList<>();
    private final List<PublisherService> publisherServices = new ArrayList<>();
    private final List<DeploymentManagerService> deploymentManagerServices = new ArrayList<>();
    private final List<AuthenticationService> authenticationServices = new ArrayList<>();
    private boolean started = false;

    public AggregatingPublisher(final String bindAddress, final int bindPort) throws IOException {
        this.webServer = new WebServer(bindAddress, bindPort);
    }

    public AggregatingPublisher registerAggregator(final AggregatorService service) {
        if (this.started) throw new IllegalStateException("Cannot register aggregator service after starting");
        service.load(this);
        this.webServer.registerHandler("/aggregate/" + service.getName(), service.getHandler());
        this.aggregatorServices.add(service);
        return this;
    }

    public AggregatingPublisher registerPublisher(final PublisherService service) {
        if (this.started) throw new IllegalStateException("Cannot register publisher service after starting");
        service.load(this);
        this.publisherServices.add(service);
        return this;
    }

    public AggregatingPublisher registerDeploymentManagement(final DeploymentManagerService service) {
        if (this.started) throw new IllegalStateException("Cannot register deployment manager service after starting");
        service.load(this);
        this.deploymentManagerServices.add(service);
        return this;
    }

    public AggregatingPublisher registerAuthentication(final AuthenticationService service) {
        if (this.started) throw new IllegalStateException("Cannot register authentication service after starting");
        service.load(this);
        this.authenticationServices.add(service);
        return this;
    }

    public void start() {
        if (this.aggregatorServices.isEmpty()) throw new IllegalStateException("No aggregator services registered");
        if (this.publisherServices.isEmpty()) throw new IllegalStateException("No publisher services registered");
        if (this.deploymentManagerServices.isEmpty()) throw new IllegalStateException("No deployment manager services registered");
        this.webServer.start();
        this.started = true;
    }

    public List<AggregatorService> getAggregatorServices() {
        return Collections.unmodifiableList(this.aggregatorServices);
    }

    public List<PublisherService> getPublisherServices() {
        return Collections.unmodifiableList(this.publisherServices);
    }

    public List<DeploymentManagerService> getDeploymentManagerServices() {
        return Collections.unmodifiableList(this.deploymentManagerServices);
    }

    public List<AuthenticationService> getAuthenticationServices() {
        return Collections.unmodifiableList(this.authenticationServices);
    }

    public void aggregateFile(final String name, final byte[] data) {
        for (DeploymentManagerService service : this.deploymentManagerServices) {
            service.aggregateFile(name, data);
        }
    }

}
