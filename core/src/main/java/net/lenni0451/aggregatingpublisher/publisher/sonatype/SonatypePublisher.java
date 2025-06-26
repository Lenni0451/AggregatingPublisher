package net.lenni0451.aggregatingpublisher.publisher.sonatype;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.auth.Authentication;
import net.lenni0451.aggregatingpublisher.services.PublisherService;
import net.lenni0451.aggregatingpublisher.utils.HttpUtils;
import net.lenni0451.aggregatingpublisher.utils.ProgressConsumer;
import net.lenni0451.aggregatingpublisher.utils.ProgressingByteArrayInputStream;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.content.HttpContent;
import net.lenni0451.commons.httpclient.content.StreamedHttpContent;
import net.lenni0451.commons.httpclient.content.impl.MultiPartFormContent;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RequiredArgsConstructor
public class SonatypePublisher implements PublisherService {

    private static final String API_URL = "https://central.sonatype.com/api/v1/publisher/upload";

    private final String name;
    private final Authentication authentication;

    @Override
    public String getName() {
        return this.name + " (Sonatype)";
    }

    @Override
    public void publish(Map<String, byte[]> deployment, ProgressConsumer progressConsumer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : deployment.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        String bundleName = this.getCommonPrefix(deployment.keySet());
        if (bundleName.isEmpty()) {
            bundleName = "bundle.zip";
        } else {
            bundleName = bundleName.substring(bundleName.lastIndexOf('/') + 1);
        }
        log.info("Using bundle name: {}", bundleName);

        MultiPartFormContent multiPartFormContent = new MultiPartFormContent();
        multiPartFormContent.addPart("bundle", HttpContent.bytes(baos.toByteArray()), bundleName);
        ProgressingByteArrayInputStream in = new ProgressingByteArrayInputStream(progressConsumer, multiPartFormContent.getAsBytes());
        PostRequest request = new PostRequest(API_URL);
        request.setContent(new StreamedHttpContent(multiPartFormContent.getContentType(), in, in.available()));
        HttpResponse response = HttpUtils.execute(request, this.authentication);
        log.info("Uploaded bundle to Sonatype: {}", response.getContentAsString());
    }

    private String getCommonPrefix(final Set<String> paths) {
        if (paths.isEmpty()) return "";
        String commonPrefix = paths.iterator().next();
        for (String path : paths) {
            while (!path.startsWith(commonPrefix)) {
                int lastSlash = commonPrefix.lastIndexOf('/');
                if (lastSlash < 0) return "";
                commonPrefix = commonPrefix.substring(0, lastSlash);
            }
        }
        return commonPrefix;
    }

}
