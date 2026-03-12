package com.hexix.traffic;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;
import java.util.Objects;

@ApplicationScoped
public class TrafficService {
    @Inject
    @RestClient
    ProxmoxClient proxmoxClient;

    // Liest den Token aus der Config (wird via .env/Umgebungsvariable befüllt)
    @ConfigProperty(name = "proxmox.api.token")
    String authHeader;

    @ConfigProperty(name = "proxmox.api.node-name")
    String nodeName;

    // Wir nutzen einen Processor als "Verteilerstation"
    private final BroadcastProcessor<ServerMetrics> processor = BroadcastProcessor.create();

    void onStart(@Observes StartupEvent ev) {
        // Use the event parameter to avoid it being considered unused by static analysis
        Objects.requireNonNull(ev);
        
        Multi.createFrom().ticks().every(Duration.ofSeconds(2))
                .subscribe().with(tick -> {
                    try {
                        ServerMetrics metrics = fetchAllMetrics();
                        processor.onNext(metrics);
                    } catch (Exception e) {
                        // Falls der Client noch nicht bereit ist oder Proxmox zickt
                        System.err.println("Fehler beim Abruf: " + e.getMessage());
                    }
                });
    }
    public Multi<ServerMetrics> getSharedStream() {
        return processor;
    }

    private synchronized ServerMetrics fetchAllMetrics() {
        try {

            // 1. Status (CPU/RAM Snapshot)
            JsonNode statusResp = proxmoxClient.getNodeStatus(nodeName, authHeader);
            JsonNode sData = statusResp.has("data") ? statusResp.get("data") : statusResp;

            // 2. RRD Daten (Historie & aktueller Trend)
            JsonNode rrdResp = proxmoxClient.getRrdData(nodeName, "hour", authHeader);
            JsonNode rrdArray = rrdResp.get("data");

            // Wir nehmen das allerletzte Element im Array (der aktuellste Datenpunkt)
            JsonNode lastEntry = rrdArray.get(rrdArray.size() - 1);

            // RRD Werte sind bereits "pro Sekunde" (Bytes/s)
            double netInBytesPerSec = lastEntry.get("netin").asDouble();
            double netOutBytesPerSec = lastEntry.get("netout").asDouble();

            // Umrechnung in MB/s (1024 * 1024)
            double mbsIn = netInBytesPerSec / 1048576.0;
            double mbsOut = netOutBytesPerSec / 1048576.0;

            // CPU & RAM aus dem Status-Objekt (für Echtzeit-Gefühl)
            double cpuPercent = sData.get("cpu").asDouble() * 100;
            double memUsed = sData.get("memory").get("used").asDouble();
            double memTotal = sData.get("memory").get("total").asDouble();

            return new ServerMetrics(
                    mbsIn,
                    mbsOut,
                    cpuPercent,
                    (memUsed / memTotal) * 100,
                    sData.get("uptime").asLong()
            );

        } catch (Exception e) {
            return new ServerMetrics(0, 0, 0, 0, 0);
        }
    }


}
