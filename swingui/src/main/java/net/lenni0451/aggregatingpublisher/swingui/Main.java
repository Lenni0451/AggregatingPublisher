package net.lenni0451.aggregatingpublisher.swingui;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.aggregator.maven.MavenAggregator;
import net.lenni0451.aggregatingpublisher.auth.Authentication;
import net.lenni0451.aggregatingpublisher.auth.BasicAuth;
import net.lenni0451.aggregatingpublisher.auth.TokenAuth;
import net.lenni0451.aggregatingpublisher.publisher.maven.MavenPublisher;
import net.lenni0451.aggregatingpublisher.publisher.sonatype.SonatypePublisher;
import net.lenni0451.commons.gson.GsonParser;
import net.lenni0451.commons.gson.elements.GsonArray;
import net.lenni0451.commons.gson.elements.GsonElement;
import net.lenni0451.commons.gson.elements.GsonObject;

import javax.annotation.Nullable;
import javax.swing.*;
import java.io.*;
import java.util.Objects;

@Slf4j
public class Main {

    private static final File configFile = new File("config.json");
    public static AggregatingPublisher aggregatingPublisher;
    public static FileCollector fileCollector;

    public static void main(String[] args) {
        try {
            GsonObject config = loadConfig();
            if (config == null) {
                Popup.show("Config file created.\nPlease configure the settings and restart the application.", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Tray.showIcon();
            aggregatingPublisher = new AggregatingPublisher(config.getString("bindAddress", "0.0.0.0"), config.getInt("bindPort", 8080));
            fileCollector = new FileCollector();
            aggregatingPublisher.registerDeploymentManagement(fileCollector);
            loadAggregators(config);
            loadPublishers(config);
            aggregatingPublisher.start();
        } catch (Throwable t) {
            log.error("An error occurred while starting the application", t);
            Popup.showException("An error occurred while starting the application", t);
        }
    }

    @Nullable
    private static GsonObject loadConfig() throws IOException {
        if (!configFile.exists()) {
            try (InputStream is = Main.class.getResourceAsStream("/config.json");
                 FileOutputStream fos = new FileOutputStream(configFile)) {
                is.transferTo(fos);
            }
            return null;
        }
        try (FileReader reader = new FileReader(configFile)) {
            return GsonParser.parse(reader).asObject();
        }
    }

    private static void loadAggregators(final GsonObject config) {
        GsonArray aggregators = config.getArray("aggregators", new GsonArray());
        for (GsonElement rawAggregator : aggregators) {
            GsonObject aggregator = rawAggregator.asObject();
            String type = aggregator.getString("type");
            if ("maven".equalsIgnoreCase(type)) {
                aggregatingPublisher.registerAggregator(new MavenAggregator());
                log.info("Registered Maven aggregator");
            } else {
                throw new IllegalArgumentException("Unknown aggregator type: " + type);
            }
        }
    }

    private static void loadPublishers(final GsonObject config) {
        GsonArray publishers = config.getArray("publishers", new GsonArray());
        for (GsonElement rawPublisher : publishers) {
            GsonObject publisher = rawPublisher.asObject();
            String type = Objects.requireNonNull(publisher.getString("type"), "Publisher type is missing");
            String name = Objects.requireNonNull(publisher.getString("name"), "Publisher name is missing");
            GsonObject authentication = publisher.getObject("authentication");
            if ("maven".equalsIgnoreCase(type)) {
                String url = Objects.requireNonNull(publisher.getString("url"), "Publisher URL is missing");
                aggregatingPublisher.registerPublisher(new MavenPublisher(name, url, parseAuthentication(authentication)));
                log.info("Registered Maven publisher: {}", name);
            } else if ("sonatype".equalsIgnoreCase(type)) {
                Authentication auth = Objects.requireNonNull(parseAuthentication(authentication), "Sonatype publisher authentication is missing");
                aggregatingPublisher.registerPublisher(new SonatypePublisher(name, auth));
                log.info("Registered Sonatype publisher: {}", name);
            } else {
                throw new IllegalArgumentException("Unknown publisher type: " + type);
            }
        }
    }

    private static Authentication parseAuthentication(@Nullable final GsonObject auth) {
        if (auth == null) return null;
        String type = auth.getString("type");
        if ("basic".equalsIgnoreCase(type)) {
            String username = Objects.requireNonNull(auth.getString("username"), "Username is missing");
            String password = Objects.requireNonNull(auth.getString("password"), "Password is missing");
            return new BasicAuth(username, password);
        } else if ("token".equalsIgnoreCase(type)) {
            String token = Objects.requireNonNull(auth.getString("token"), "Token is missing");
            return new TokenAuth(token);
        } else {
            throw new IllegalArgumentException("Unknown authentication type: " + type);
        }
    }

}
