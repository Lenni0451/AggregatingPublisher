package net.lenni0451.aggregatingpublisher.publisher.maven;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.services.PublisherService;
import net.lenni0451.aggregatingpublisher.utils.ProgressConsumer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MavenLocalPublisher implements PublisherService {

    private final String name;
    @Getter
    private final File directory;

    public MavenLocalPublisher(final String name, @Nullable final File directory) {
        this.name = name;
        this.directory = directory == null ? this.getDefaultMavenLocalDir() : directory;
    }

    private File getDefaultMavenLocalDir() {
        String repoLocal = System.getProperty("maven.repo.local");
        if (repoLocal != null && !repoLocal.isEmpty()) {
            return new File(repoLocal);
        }
        return new File(System.getProperty("user.home", "."), ".m2" + File.separator + "repository");
    }

    @Override
    public String getName() {
        return this.name + " (Maven Local)";
    }

    @Override
    public void publish(final Map<String, byte[]> deployment, final ProgressConsumer progressConsumer) throws IOException {
        log.info("Publishing {} files to {}", deployment.size(), this.directory.getAbsolutePath());
        Map<String, byte[]> files = new HashMap<>();
        Set<String> snapshotMetadata = new HashSet<>();
        { //Check files and merge metadata with local metadata
            int processed = 0;
            for (Map.Entry<String, byte[]> entry : deployment.entrySet()) {
                if (entry.getKey().endsWith("/maven-metadata.xml")) {
                    String mavenMetadataPath = entry.getKey();
                    mavenMetadataPath = mavenMetadataPath.substring(0, mavenMetadataPath.length() - 4) + "-local.xml";
                    String localMetadata = this.readLocalMetadata(mavenMetadataPath);
                    if (localMetadata != null) {
                        log.info("Merging {} with local metadata", mavenMetadataPath);
                        String metadata = new String(entry.getValue(), StandardCharsets.UTF_8);
                        if (metadata.contains("snapshotVersions")) {
                            //Crude check if the metadata is for a snapshot version to quickly filter out non-snapshot metadata
                            //A more robust check is implemented in the MavenSnapshotIncrementer
                            snapshotMetadata.add(mavenMetadataPath);
                        }
                        String mergedMetadata = MavenMetadataMerger.merge(localMetadata, metadata);
                        this.updateMetadata(files, mavenMetadataPath, mergedMetadata);
                    } else {
                        files.put(mavenMetadataPath, entry.getValue());
                    }
                } else if (!files.containsKey(entry.getKey())) {
                    files.put(entry.getKey(), entry.getValue());
                }
                progressConsumer.accept(1F / deployment.size() * ++processed, 1, 3);
            }
        }
        { //Increment snapshot versions if necessary
            int updated = 0;
            for (String metadataPath : snapshotMetadata) {
                log.info("Incrementing snapshot version for {}", metadataPath);
                String metadata = new String(files.get(metadataPath), StandardCharsets.UTF_8);
                String incrementedMetadata = MavenSnapshotIncrementer.incrementSnapshotVersion(metadataPath, metadata, files);
                if (incrementedMetadata != null) {
                    this.updateMetadata(files, metadataPath, incrementedMetadata);
                }
                progressConsumer.accept(1F / snapshotMetadata.size() * ++updated, 2, 3);
            }
        }
        {
            int written = 0;
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                if (entry.getKey().endsWith(".md5")) continue;
                if (entry.getKey().endsWith(".sha1")) continue;
                if (entry.getKey().endsWith(".sha256")) continue;
                if (entry.getKey().endsWith(".sha512")) continue;

                File file = new File(this.directory, entry.getKey().replace("/", File.separator));
                log.info("Writing file: {} to {}", entry.getKey(), file.getAbsolutePath());
                file.getParentFile().mkdirs();
                Files.write(file.toPath(), entry.getValue());
                progressConsumer.accept(1F / files.size() * ++written, 3, 3);
            }
        }
        log.info("Published {} files to {}", files.size(), this.directory.getAbsolutePath());
    }

    @Nullable
    private String readLocalMetadata(final String path) throws IOException {
        File file = new File(this.directory, path.replace("/", File.separator));
        if (!file.isFile()) return null;
        byte[] content = Files.readAllBytes(file.toPath());
        return new String(content, StandardCharsets.UTF_8);
    }

    private void updateMetadata(final Map<String, byte[]> files, final String metadataPath, final String metadata) {
        byte[] incrementedMetadataByte = metadata.getBytes(StandardCharsets.UTF_8);
        files.put(metadataPath, incrementedMetadataByte);
//        files.put(metadataPath + ".md5", HashUtils.md5(incrementedMetadataByte).getBytes(StandardCharsets.UTF_8));
//        files.put(metadataPath + ".sha1", HashUtils.sha1(incrementedMetadataByte).getBytes(StandardCharsets.UTF_8));
//        files.put(metadataPath + ".sha256", HashUtils.sha256(incrementedMetadataByte).getBytes(StandardCharsets.UTF_8));
//        files.put(metadataPath + ".sha512", HashUtils.sha512(incrementedMetadataByte).getBytes(StandardCharsets.UTF_8));
    }

}
