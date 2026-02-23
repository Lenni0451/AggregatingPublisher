package net.lenni0451.aggregatingpublisher.aggregator.maven;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.web.RequestHandler;
import net.lenni0451.aggregatingpublisher.web.RequestInfo;
import net.lenni0451.aggregatingpublisher.web.ResponseInfo;

import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
public class MavenRequestHandler extends RequestHandler {

    private final MavenAggregator aggregator;

    @Override
    public ResponseInfo handle(RequestInfo request) throws Throwable {
        String path = request.uri().getPath();
        if (path.startsWith("/")) path = path.substring(1);
        path = path.substring(path.indexOf("/") + 1); //Cut off /aggregate/
        path = path.substring(path.indexOf("/") + 1); //Cut off /maven/

        if (request.method().equalsIgnoreCase("PUT")) {
            String auth = request.headers().containsKey("Authorization") ? request.headers().get("Authorization").get(0) : null;
            if (!this.aggregator.getAggregatingPublisher().checkAuth(auth)) {
                return ResponseInfo.of(401, "Unauthorized");
            }

            if (path.endsWith("/")) {
                log.warn("Maven PUT request with trailing slash: {}", path);
                return ResponseInfo.notFound();
            } else {
                log.info("Maven PUT request for {}", path);
                try (InputStream is = request.body()) {
                    byte[] artifact = is.readAllBytes();
                    this.aggregator.getAggregatingPublisher().aggregateFile(path, artifact);
                }
                return ResponseInfo.success("OK");
            }
        } else if (request.method().equalsIgnoreCase("GET")) {
            //If the maven publisher requests anything, 404 should be returned.
            //Normally, the maven publisher tries to get the maven-metadata.xml file to append the new version to it
            // but returning 404 ensures a new file is created and pushed to the repository instead.
            log.info("Maven GET request for path: {}", path);
            return ResponseInfo.notFound();
        } else {
            return ResponseInfo.methodNotAllowed();
        }
    }

}
