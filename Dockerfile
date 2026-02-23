# Use Eclipse Temurin for the JRE 17 as specified in gradle.properties
FROM eclipse-temurin:17-jre-alpine

# Set the working directory for the application data
# This is the "run dir" where config.json and logs will be stored
WORKDIR /app

# Copy the built JAR from the webui module's build output
# We use a wildcard to avoid issues with version changes
# Assumes the JAR has been built with `./gradlew :webui:jar`
COPY webui/build/libs/AggregatingPublisher-*.jar /usr/share/aggregating-publisher-webui.jar

# Define /app as a volume for persistence (config.json, etc.)
# This allows the "run dir" to be mapped to a host folder or named volume
VOLUME /app

# Expose the default port for the web interface
EXPOSE 8080

# Run the application
# We use -Djava.awt.headless=true to avoid issues with AWT in a headless environment
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/usr/share/aggregating-publisher-webui.jar"]
