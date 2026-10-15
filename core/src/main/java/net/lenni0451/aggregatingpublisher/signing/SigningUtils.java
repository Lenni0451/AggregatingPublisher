package net.lenni0451.aggregatingpublisher.signing;

import net.lenni0451.aggregatingpublisher.services.SigningService;
import net.lenni0451.aggregatingpublisher.utils.HashUtils;
import net.lenni0451.aggregatingpublisher.utils.ProgressConsumer;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SigningUtils {

    public static boolean shouldSign(final String path, final Map<String, byte[]> allFiles) {
        if (path.endsWith(".md5") || path.endsWith(".sha1") || path.endsWith(".sha256") || path.endsWith(".sha512")) {
            return false;
        }
        if (path.endsWith(".asc") || path.endsWith(".sig")) {
            return false;
        }
        if (path.endsWith("maven-metadata.xml") || path.endsWith("maven-metadata-local.xml")) {
            return false;
        }
        if (allFiles.containsKey(path + ".asc")) {
            return false;
        }
        return true;
    }

    public static List<String> getFilesToSign(final Map<String, byte[]> files) {
        List<String> toSign = new ArrayList<>();
        for (String path : files.keySet()) {
            if (shouldSign(path, files)) {
                toSign.add(path);
            }
        }
        return toSign;
    }

    public static Map<String, byte[]> signFiles(final SigningService signer, final Map<String, byte[]> files, @Nullable final ProgressConsumer progressConsumer) throws Exception {
        List<String> toSign = getFilesToSign(files);
        Map<String, byte[]> generatedFiles = new HashMap<>();
        int processed = 0;
        for (String path : toSign) {
            byte[] data = files.get(path);
            byte[] signature = signer.sign(path, data);
            String ascPath = path + ".asc";
            generatedFiles.put(ascPath, signature);
            generatedFiles.put(ascPath + ".md5", HashUtils.md5(signature).getBytes(StandardCharsets.UTF_8));
            generatedFiles.put(ascPath + ".sha1", HashUtils.sha1(signature).getBytes(StandardCharsets.UTF_8));
            generatedFiles.put(ascPath + ".sha256", HashUtils.sha256(signature).getBytes(StandardCharsets.UTF_8));
            generatedFiles.put(ascPath + ".sha512", HashUtils.sha512(signature).getBytes(StandardCharsets.UTF_8));
            processed++;
            if (progressConsumer != null) {
                progressConsumer.accept((float) processed / toSign.size(), processed, toSign.size());
            }
        }
        return generatedFiles;
    }

}
