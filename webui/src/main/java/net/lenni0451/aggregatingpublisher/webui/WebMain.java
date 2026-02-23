package net.lenni0451.aggregatingpublisher.webui;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.utils.ConfigUtils;
import net.lenni0451.commons.gson.elements.GsonObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Slf4j
public class WebMain {

    private static final File configFile = new File("config.json");
    public static AggregatingPublisher aggregatingPublisher;
    public static WebFileCollector fileCollector;

    public static void main(String[] args) {
        try {
            GsonObject config = ConfigUtils.loadConfig(configFile);
            if (config == null) {
                log.info("Config file created. Please configure the settings and restart the application.");
                return;
            }

            String username = Objects.requireNonNull(config.getString("username"), "Username is missing in config");
            String password = Objects.requireNonNull(config.getString("password"), "Password is missing in config");

            aggregatingPublisher = new AggregatingPublisher(config.getString("bindAddress", "0.0.0.0"), config.getInt("bindPort", 8080));

            // Set up authenticator for aggregator (Basic Auth)
            aggregatingPublisher.setAuthenticator(authHeader -> {
                if (authHeader == null) return false;
                if (!authHeader.startsWith("Basic ")) return false;
                String token = authHeader.substring(6);
                try {
                    String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
                    return (username + ":" + password).equals(decoded);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            });

            fileCollector = new WebFileCollector();
            aggregatingPublisher.registerDeploymentManagement(fileCollector);

            aggregatingPublisher.getWebServer().registerHandler("/", new WebInterfaceHandler(username, password, fileCollector, aggregatingPublisher));

            ConfigUtils.loadAggregators(aggregatingPublisher, config);
            ConfigUtils.loadPublishers(aggregatingPublisher, config);
            aggregatingPublisher.start();
            log.info("Aggregating Publisher Web UI started on port {}", config.getInt("bindPort", 8080));
        } catch (Throwable t) {
            log.error("An error occurred while starting the application", t);
        }
    }

}
