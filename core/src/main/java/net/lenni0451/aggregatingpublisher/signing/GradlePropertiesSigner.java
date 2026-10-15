package net.lenni0451.aggregatingpublisher.signing;

import net.lenni0451.aggregatingpublisher.services.SigningService;
import org.bouncycastle.openpgp.PGPException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class GradlePropertiesSigner implements SigningService {

    private static File getSystemGradleProperties() {
        String home = System.getProperty("gradle.user.home");
        if (home == null || home.isEmpty()) home = System.getenv("GRADLE_USER_HOME");
        if (home == null || home.isEmpty()) home = System.getProperty("user.home", ".") + "/.gradle";
        return new File(home, "gradle.properties");
    }


    private final PgpSigner delegate;
    private final String name;

    public GradlePropertiesSigner() throws IOException, PGPException {
        this(getSystemGradleProperties(), null, null, null);
    }

    public GradlePropertiesSigner(final File file) throws IOException, PGPException {
        this(file, null, null, null);
    }

    public GradlePropertiesSigner(final File file, @Nullable final String overrideKeyId, @Nullable final String overridePassword, @Nullable final String name) throws IOException, PGPException {
        if (!file.exists()) {
            throw new IllegalArgumentException("Gradle properties file does not exist: " + file.getAbsolutePath());
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        }

        String key = properties.getProperty("signing.key");
        if (key == null) key = properties.getProperty("signingKey");
        String fileRing = properties.getProperty("signing.secretKeyRingFile");
        String password = overridePassword != null ? overridePassword : properties.getProperty("signing.password");
        if (password == null && overridePassword == null) password = properties.getProperty("signingPassword");
        String keyId = overrideKeyId != null ? overrideKeyId : properties.getProperty("signing.keyId");

        if (key != null) {
            this.delegate = new PgpSigner(key.getBytes(StandardCharsets.UTF_8), password, keyId, name);
        } else if (fileRing != null) {
            File ringFile = this.resolveRingFile(fileRing, file.getParentFile());
            this.delegate = new PgpSigner(ringFile, password, keyId, name);
        } else {
            throw new IllegalArgumentException("Neither signing.key/signingKey nor signing.secretKeyRingFile found in " + file.getAbsolutePath());
        }

        this.name = name != null ? name : this.delegate.getName() + " (Gradle)";
    }

    private File resolveRingFile(final String path, final File baseDir) {
        if (path.startsWith("~/") || path.startsWith("~\\")) {
            return new File(System.getProperty("user.home", "."), path.substring(2));
        }
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        }
        return new File(baseDir, path);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public byte[] sign(final String path, final byte[] data) throws Exception {
        return this.delegate.sign(path, data);
    }

}
