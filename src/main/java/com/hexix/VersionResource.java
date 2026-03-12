package com.hexix;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Path("/api/version")
@ApplicationScoped
public class VersionResource {

    private String version;
    private String buildTimestamp;

    @PostConstruct
    void init() {
        Properties props = new Properties();
        try (InputStream is = VersionResource.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // ignore
        }
        this.version = props.getProperty("application.version", "unknown");
        this.buildTimestamp = props.getProperty("build.timestamp", "unknown");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public VersionInfo getVersion() {
        return new VersionInfo(version, buildTimestamp);
    }

    public static class VersionInfo {
        public String version;
        public String buildTimestamp;

        public VersionInfo(String version, String buildTimestamp) {
            this.version = version;
            this.buildTimestamp = buildTimestamp;
        }
    }
}
