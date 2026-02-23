package net.lenni0451.aggregatingpublisher.utils;

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
import java.io.*;
import java.util.Objects;

@Slf4j
public class ConfigUtils {

    @Nullable
    public static GsonObject loadConfig(File configFile) throws IOException {
        if (!configFile.exists()) {
            try (InputStream is = ConfigUtils.class.getResourceAsStream("/config.json");
                 FileOutputStream fos = new FileOutputStream(configFile)) {
                if (is != null) {
                    is.transferTo(fos);
                }
            }
            return null;
        }
        try (FileReader reader = new FileReader(configFile)) {
            return GsonParser.parse(reader).asObject();
        }
    }

    public static void loadAggregators(final AggregatingPublisher aggregatingPublisher, final GsonObject config) {
        GsonArray aggregators = config.getArray("aggregators", new GsonArray());
        for (GsonObject aggregator : aggregators.asList(GsonElement::asObject)) {
            String type = Objects.requireNonNull(aggregator.getString("type"), "Aggregator type is missing");
            if ("maven".equalsIgnoreCase(type)) {
                aggregatingPublisher.registerAggregator(new MavenAggregator());
                log.info("Registered Maven aggregator");
            } else {
                throw new IllegalArgumentException("Unknown aggregator type: " + type);
            }
        }
    }

    public static void loadPublishers(final AggregatingPublisher aggregatingPublisher, final GsonObject config) {
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
        String type = Objects.requireNonNull(auth.getString("type"), "Auth type is missing");
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
