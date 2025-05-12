package net.lenni0451.aggregatingpublisher.publisher.maven;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.auth.Authentication;
import net.lenni0451.aggregatingpublisher.services.PublisherService;
import net.lenni0451.aggregatingpublisher.utils.HashUtils;
import net.lenni0451.aggregatingpublisher.utils.HttpUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MavenPublisher implements PublisherService {

    private final String name;
    private final String url;
    private final Authentication authentication;

    public MavenPublisher(final String name, final String url) {
        this(name, url, null);
    }

    public MavenPublisher(final String name, final String url, @Nullable final Authentication authentication) {
        this.name = name;
        this.url = url;
        this.authentication = authentication;
    }

    @Override
    public String getName() {
        return this.name + " (Maven)";
    }

    @Override
    public void publish(Map<String, byte[]> deployment) throws IOException {
        log.info("Publishing {} files to {}", deployment.size(), this.url);
        Map<String, byte[]> files = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : deployment.entrySet()) {
            if (entry.getKey().endsWith("/maven-metadata.xml")) {
                String mavenMetadata = entry.getKey();
                String mavenMetadataMd5 = mavenMetadata + ".md5";
                String mavenMetadataSha1 = mavenMetadata + ".sha1";
                String mavenMetadataSha256 = mavenMetadata + ".sha256";
                String mavenMetadataSha512 = mavenMetadata + ".sha512";

                String onlineMetadata = this.requestOnlineMetadata(mavenMetadata);
                if (onlineMetadata != null) {
                    log.info("Merging {} with online metadata", mavenMetadata);
                    String mergedMetadata = MavenMetadataMerger.merge(onlineMetadata, new String(entry.getValue(), StandardCharsets.UTF_8));
                    byte[] metadataBytes = mergedMetadata.getBytes(StandardCharsets.UTF_8);
                    files.put(mavenMetadata, metadataBytes);
                    files.put(mavenMetadataMd5, HashUtils.md5(metadataBytes).getBytes(StandardCharsets.UTF_8));
                    files.put(mavenMetadataSha1, HashUtils.sha1(metadataBytes).getBytes(StandardCharsets.UTF_8));
                    files.put(mavenMetadataSha256, HashUtils.sha256(metadataBytes).getBytes(StandardCharsets.UTF_8));
                    files.put(mavenMetadataSha512, HashUtils.sha512(metadataBytes).getBytes(StandardCharsets.UTF_8));
                } else {
                    files.put(mavenMetadata, entry.getValue());
                }
            } else if (!files.containsKey(entry.getKey())) {
                files.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String url = this.url + (this.url.endsWith("/") ? "" : "/") + entry.getKey();
            log.info("Uploading file: {} to {}", entry.getKey(), url);
            HttpUtils.put(url, entry.getValue(), this.authentication);
        }
        log.info("Published {} files to {}", files.size(), this.url);
    }

    @Nullable
    private String requestOnlineMetadata(final String path) throws IOException {
        byte[] response = HttpUtils.get(this.url + (this.url.endsWith("/") ? "" : "/") + path, this.authentication);
        if (response == null) return null;
        return new String(response, StandardCharsets.UTF_8);
    }

}
