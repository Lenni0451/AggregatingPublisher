package net.lenni0451.aggregatingpublisher.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {

    private final ExecutorService connectionHandlerExecutor;
    private final HttpServer httpServer;
    private State state = State.INIT;

    public WebServer(final String bindAddress, final int port) throws IOException {
        this.connectionHandlerExecutor = Executors.newWorkStealingPool();
        this.httpServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        this.httpServer.setExecutor(this.connectionHandlerExecutor);
    }

    public void start() {
        if (!this.state.equals(State.INIT)) throw new IllegalStateException("WebServer is already started or stopped");
        this.httpServer.start();
        this.state = State.RUNNING;
    }

    public void registerHandler(final String context, final RequestHandler handler) {
        if (!this.state.equals(State.INIT)) throw new IllegalStateException("WebServer is already running or stopped");
        this.httpServer.createContext(context, handler);
    }

    public void stop() {
        if (!this.state.equals(State.RUNNING)) throw new IllegalStateException("WebServer is not running");
        this.httpServer.stop(0);
        this.connectionHandlerExecutor.shutdown();
        this.state = State.STOPPED;
    }


    private enum State {
        INIT, RUNNING, STOPPED
    }

}
