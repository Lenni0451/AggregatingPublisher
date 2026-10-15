package net.lenni0451.aggregatingpublisher.utils;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.AggregatingPublisher;
import net.lenni0451.aggregatingpublisher.aggregator.maven.MavenAggregator;
import net.lenni0451.aggregatingpublisher.auth.Authentication;
import net.lenni0451.aggregatingpublisher.auth.BasicAuth;
import net.lenni0451.aggregatingpublisher.auth.TokenAuth;
import net.lenni0451.aggregatingpublisher.publisher.maven.MavenLocalPublisher;
import net.lenni0451.aggregatingpublisher.publisher.maven.MavenPublisher;
import net.lenni0451.aggregatingpublisher.publisher.sonatype.SonatypePublisher;
import net.lenni0451.aggregatingpublisher.services.GradlePropertiesAuthenticator;
import net.lenni0451.aggregatingpublisher.signing.GpgSigner;
import net.lenni0451.aggregatingpublisher.signing.GradlePropertiesSigner;
import net.lenni0451.aggregatingpublisher.signing.PgpSigner;
import net.lenni0451.commons.gson.GsonParser;
import net.lenni0451.commons.gson.elements.GsonArray;
import net.lenni0451.commons.gson.elements.GsonElement;
import net.lenni0451.commons.gson.elements.GsonObject;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
            } else if ("mavenLocal".equalsIgnoreCase(type)) {
                String path = publisher.getString("path");
                File directory = path == null ? null : new File(path);
                aggregatingPublisher.registerPublisher(new MavenLocalPublisher(name, directory));
                log.info("Registered Maven Local publisher: {}", name);
            } else {
                throw new IllegalArgumentException("Unknown publisher type: " + type);
            }
        }
    }

    public static void loadSigning(final AggregatingPublisher aggregatingPublisher, final GsonObject config) {
        GsonObject signing = config.getObject("signing");
        if (signing == null) return;
        String type = Objects.requireNonNull(signing.getString("type"), "Signing type is missing");
        String name = signing.getString("name");
        if ("pgp".equalsIgnoreCase(type)) {
            String key = signing.getString("key");
            String file = signing.getString("file");
            String keyId = signing.getString("keyId");
            String password = signing.getString("password");
            try {
                if (key != null) {
                    if (key.contains("-----BEGIN") || key.contains("\n")) {
                        aggregatingPublisher.registerSigning(new PgpSigner(key.getBytes(StandardCharsets.UTF_8), password, keyId, name));
                    } else {
                        File keyFile = new File(key);
                        if (keyFile.isFile()) {
                            aggregatingPublisher.registerSigning(new PgpSigner(keyFile, password, keyId, name));
                        } else {
                            aggregatingPublisher.registerSigning(new PgpSigner(key.getBytes(StandardCharsets.UTF_8), password, keyId, name));
                        }
                    }
                } else if (file != null) {
                    aggregatingPublisher.registerSigning(new PgpSigner(new File(file), password, keyId, name));
                } else {
                    throw new IllegalArgumentException("Either 'key' or 'file' must be specified for PGP signing");
                }
                log.info("Registered PGP signer: {}", aggregatingPublisher.getSigningService().getName());
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to load PGP signer", t);
            }
        } else if ("gradle".equalsIgnoreCase(type)) {
            String file = signing.getString("file");
            String keyId = signing.getString("keyId");
            String password = signing.getString("password");
            try {
                if (file != null) {
                    aggregatingPublisher.registerSigning(new GradlePropertiesSigner(new File(file), keyId, password, name));
                } else {
                    aggregatingPublisher.registerSigning(new GradlePropertiesSigner());
                }
                log.info("Registered Gradle properties signer: {}", aggregatingPublisher.getSigningService().getName());
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to load Gradle properties signer", t);
            }
        } else if ("gpg".equalsIgnoreCase(type)) {
            String key = signing.getString("key");
            String executable = signing.getString("executable");
            String password = signing.getString("password");
            List<String> args = null;
            if (signing.has("args")) {
                args = signing.getArray("args").asList(GsonElement::asString);
            }
            aggregatingPublisher.registerSigning(new GpgSigner(key, password, executable, args, name));
            log.info("Registered GPG signer: {}", aggregatingPublisher.getSigningService().getName());
        } else {
            throw new IllegalArgumentException("Unknown signing type: " + type);
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
        } else if ("gradle".equalsIgnoreCase(type)) {
            String usernameKey = Objects.requireNonNull(auth.getString("usernameKey"), "usernameKey is missing");
            String passwordKey = Objects.requireNonNull(auth.getString("passwordKey"), "passwordKey is missing");
            String file = auth.getString("file");
            try {
                if (file != null) {
                    return new GradlePropertiesAuthenticator(new File(file), usernameKey, passwordKey);
                } else {
                    return new GradlePropertiesAuthenticator(usernameKey, passwordKey);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load gradle.properties", e);
            }
        } else {
            throw new IllegalArgumentException("Unknown authentication type: " + type);
        }
    }

}
