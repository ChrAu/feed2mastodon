package com.hexix.util;

import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;

public class DurationLogger implements AutoCloseable {

    private final String blockName;
    private final long startTime;
    private final Logger logger;
    private final Logger.Level level;

    /**
     * Erstellt einen Logger zur Zeitmessung mit JBoss Logging.
     * @param blockName Name des Code-Blocks zur Identifizierung in den Logs.
     * @param logger Der JBoss Logger, der für die Ausgabe verwendet werden soll.
     * @param level Das Loglevel (z.B. Logger.Level.INFO, Logger.Level.DEBUG).
     */
    public DurationLogger(String blockName, Logger logger, Logger.Level level) {
        this.blockName = blockName;
        this.logger = logger;
        this.level = level;
        this.startTime = System.nanoTime();

        // Verwendet logf für formatiertes, performantes Logging
        this.logger.logf(this.level, "Start: '%s'", this.blockName);
    }

    /**
     * Bequemer Konstruktor, der standardmäßig Level.INFO verwendet.
     * @param blockName Name des Code-Blocks.
     * @param logger Der zu verwendende JBoss Logger.
     */
    public DurationLogger(String blockName, Logger logger) {
        this(blockName, logger, Logger.Level.INFO);
    }

    @Override
    public void close() {
        long endTime = System.nanoTime();
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Die Parameter werden nur dann in den String eingefügt,
        // wenn das Loglevel aktiv ist. Das ist sehr performant.
        this.logger.logf(this.level, "Ende:  '%s' wurde in %d ms abgeschlossen.", this.blockName, durationMillis);
    }
}
