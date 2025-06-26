package net.lenni0451.aggregatingpublisher.publisher.maven;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.auth.Authentication;
import net.lenni0451.aggregatingpublisher.services.PublisherService;
import net.lenni0451.aggregatingpublisher.utils.HashUtils;
import net.lenni0451.aggregatingpublisher.utils.HttpUtils;
import net.lenni0451.aggregatingpublisher.utils.ProgressConsumer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    public void publish(Map<String, byte[]> deployment, ProgressConsumer progressConsumer) throws IOException {
        log.info("Publishing {} files to {}", deployment.size(), this.url);
        Map<String, byte[]> files = new HashMap<>();
        Set<String> snapshotMetadata = new HashSet<>();
        for (Map.Entry<String, byte[]> entry : deployment.entrySet()) {
            if (entry.getKey().endsWith("/maven-metadata.xml")) {
                String mavenMetadataPath = entry.getKey();
                String onlineMetadata = this.requestOnlineMetadata(mavenMetadataPath);
                if (onlineMetadata != null) {
                    log.info("Merging {} with online metadata", mavenMetadataPath);
                    String metadata = new String(entry.getValue(), StandardCharsets.UTF_8);
                    if (metadata.contains("snapshotVersions")) {
                        //Crude check if the metadata is for a snapshot version to quickly filter out non-snapshot metadata
                        //A more robust check is implemented in the MavenSnapshotIncrementer
                        snapshotMetadata.add(mavenMetadataPath);
                    }
                    String mergedMetadata = MavenMetadataMerger.merge(onlineMetadata, metadata);
                    this.updateMetadata(files, mavenMetadataPath, mergedMetadata);
                } else {
                    files.put(mavenMetadataPath, entry.getValue());
                }
            } else if (!files.containsKey(entry.getKey())) {
                files.put(entry.getKey(), entry.getValue());
            }
        }
        for (String metadataPath : snapshotMetadata) {
            log.info("Incrementing snapshot version for {}", metadataPath);
            String metadata = new String(files.get(metadataPath), StandardCharsets.UTF_8);
            String incrementedMetadata = MavenSnapshotIncrementer.incrementSnapshotVersion(metadataPath, metadata, files);
            if (incrementedMetadata != null) {
                this.updateMetadata(files, metadataPath, incrementedMetadata);
            }
        }
        int uploaded = 0;
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String url = this.url + (this.url.endsWith("/") ? "" : "/") + entry.getKey();
            log.info("Uploading file: {} to {}", entry.getKey(), url);
            HttpUtils.put(url, entry.getValue(), this.authentication);
            progressConsumer.accept(1F / files.size() * ++uploaded);
        }
        log.info("Published {} files to {}", files.size(), this.url);
    }

    @Nullable
    private String requestOnlineMetadata(final String path) throws IOException {
        byte[] response = HttpUtils.get(this.url + (this.url.endsWith("/") ? "" : "/") + path, this.authentication);
        if (response == null) return null;
        return new String(response, StandardCharsets.UTF_8);
    }

    private void updateMetadata(final Map<String, byte[]> files, final String metadataPath, final String metadata) {
        byte[] incrementedMetadataByte = metadata.getBytes(StandardCharsets.UTF_8);
        files.put(metadataPath, incrementedMetadataByte);
        files.put(metadataPath + ".md5", HashUtils.md5(incrementedMetadataByte).getBytes(StandardCharsets.UTF_8));
        files.put(metadataPath + ".sha1", HashUtils.sha1(incrementedMetadataByte).getBytes(StandardCharsets.UTF_8));
        files.put(metadataPath + ".sha256", HashUtils.sha256(incrementedMetadataByte).getBytes(StandardCharsets.UTF_8));
        files.put(metadataPath + ".sha512", HashUtils.sha512(incrementedMetadataByte).getBytes(StandardCharsets.UTF_8));
    }

}
