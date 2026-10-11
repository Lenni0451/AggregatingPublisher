package net.lenni0451.aggregatingpublisher.services;

import net.lenni0451.aggregatingpublisher.auth.BasicAuth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GradlePropertiesAuthenticator extends BasicAuth {

    private static File getSystemGradleProperties() {
        String home = System.getProperty("gradle.user.home");
        if (home == null || home.isEmpty()) home = System.getenv("GRADLE_USER_HOME");
        if (home == null || home.isEmpty()) home = System.getProperty("user.home", ".") + "/.gradle";
        return new File(home, "gradle.properties");
    }

    private static String[] loadCredentials(final File file, final String usernameKey, final String passwordKey) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        }
        String username = properties.getProperty(usernameKey);
        String password = properties.getProperty(passwordKey);
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username or password key not found in " + file.getAbsolutePath());
        }
        return new String[]{username, password};
    }


    public GradlePropertiesAuthenticator(final String usernameKey, final String passwordKey) throws IOException {
        this(getSystemGradleProperties(), usernameKey, passwordKey);
    }

    public GradlePropertiesAuthenticator(final File file, final String usernameKey, final String passwordKey) throws IOException {
        this(loadCredentials(file, usernameKey, passwordKey));
    }

    private GradlePropertiesAuthenticator(final String[] credentials) {
        super(credentials[0], credentials[1]);
    }

}
