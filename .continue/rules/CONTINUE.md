# CONTINUE.md - Dein Projekt-Leitfaden

Willkommen im Projekt! Dieser Leitfaden soll dir und deinem Team helfen, dieses Projekt effektiv zu verstehen, einzurichten und dazu beizutragen.

## 1. Projektübersicht

Dieses Projekt ist eine Full-Stack-Anwendung mit einem Java (Quarkus) Backend und einem React (TypeScript) Frontend. Die Anwendung dient als vielseitiges Dashboard und als Integrations-Hub für verschiedene Dienste.

**Schlüsseltechnologien:**

*   **Backend:**
    *   Java 21
    *   Quarkus 3.32.4
    *   Maven
    *   PostgreSQL (mit Flyway für Migrationen)
    *   Apache Camel (für Telegram-Integration)
*   **Frontend:**
    *   React 19
    *   TypeScript
    *   Vite
    *   Tailwind CSS
*   **Bereitstellung:**
    *   Docker

**Übergeordnete Architektur:**

Die Anwendung besteht aus einem Quarkus-Backend, das eine REST-API bereitstellt, und einer React Single-Page-Anwendung (SPA), die diese API konsumiert. Das Backend integriert mehrere externe Dienste, darunter:

*   Mastodon
*   Home Assistant
*   KI-Dienste (Google Gemini und/oder Ollama)
*   Telegram (über einen Bot)
*   Kuma (zur Überwachung)
*   Proxmox

## 2. Erste Schritte

### Voraussetzungen

*   Java 21
*   Maven 3.8+
*   Node.js und npm (Version siehe `src/main/webui/package.json`)
*   Docker und Docker Compose

### Installation

1.  **Repository klonen:**
    ```bash
    git clone <repository-url>
    cd <repository-verzeichnis>
    ```

2.  **Frontend-Abhängigkeiten installieren:**
    ```bash
    cd src/main/webui
    npm install
    cd ../../..
    ```

3.  **Anwendung im Entwicklungsmodus starten:**
    ```bash
    ./mvnw quarkus:dev
    ```
    Dadurch wird die Quarkus-Anwendung im Entwicklungsmodus gestartet, die auch das Frontend bereitstellt. Die Anwendung ist unter `http://localhost:8080` verfügbar.

### Tests ausführen

*   **Backend-Tests:**
    ```bash
    ./mvnw test
    ```

*   **Frontend-Tests (falls vorhanden):**
    ```bash
    cd src/main/webui
    npm test
    ```

## 3. Projektstruktur

*   `src/main/java/com/hexix/`: Java-Hauptquellcode.
    *   `ai/`: Dienste für künstliche Intelligenz, einschließlich eines Telegram-Bots.
    *   `homeassistant/`: Integration mit Home Assistant.
    *   `mastodon/`: Mastodon-API-Client und -Dienste.
    *   `scheduler/`: Geplante Aufgaben.
    *   `traffic/`: Proxmox- und Server-Metriken.
    *   `urlshortener/`: URL-Kürzungsdienst.
*   `src/main/resources/`: Anwendungskonfiguration und Ressourcen.
    *   `application.properties`: Hauptkonfigurationsdatei für Quarkus.
    *   `db/migration/`: Flyway-Datenbankmigrationsskripte.
*   `src/main/webui/`: React-Frontend-Quellcode.
    *   `src/`: Haupt-Quellordner für die React-Anwendung.
    *   `public/`: Statische-Dateien.
    *   `package.json`: Frontend-Abhängigkeiten und Skripte.
*   `src/main/docker/`: Dockerfiles für verschiedene Bereitstellungsszenarien.
*   `pom.xml`: Maven-Projektkonfiguration.

## 4. Entwicklungs-Workflow

### Codierungsstandards

*   **Backend:** Halte dich an die standardmäßigen Java- und Quarkus-Konventionen.
*   **Frontend:** Befolge die bewährten Praktiken für React und TypeScript. ESLint ist zur Durchsetzung von Codierungsstandards konfiguriert.

### Build und Bereitstellung

*   **Für die Produktion builden:**
    ```bash
    ./mvnw package
    ```

*   **Mit Docker ausführen:**
    ```bash
    docker-compose up
    ```

### Richtlinien für Beiträge

*(Bitte füge hier die Beitragsrichtlinien deines Teams ein.)*

## 5. Schlüsselkonzepte

*   **Quarkus:** Ein Full-Stack, Kubernetes-natives Java-Framework, das auf GraalVM und OpenJDK HotSpot zugeschnitten ist.
*   **Quinoa:** Eine Quarkus-Erweiterung zum Erstellen von Web-Benutzeroberflächen mit Node.js-basierten Frameworks wie React.
*   **Panache:** Eine Quarkus-Erweiterung, die den Datenbankzugriff mit einem Active-Record-Muster vereinfacht.

## 6. Häufige Aufgaben

### Hinzufügen eines neuen REST-Endpunkts

1.  Erstelle eine neue Klasse im Paket `com.hexix` (oder einem Unterpaket).
2.  Annotiere die Klasse mit `@Path("/dein-pfad")`.
3.  Füge Methoden mit Annotationen wie `@GET`, `@POST` usw. hinzu, um Anfragen zu bearbeiten.

### Hinzufügen einer neuen Frontend-Komponente

1.  Erstelle eine neue `.tsx`-Datei in `src/main/webui/src/components/`.
2.  Implementiere deine React-Komponente.
3.  Verwende die Komponente auf einer der Seiten in `src/main/webui/src/pages/`.

## 7. Fehlerbehebung

*(Bitte füge hier häufige Probleme und deren Lösungen ein.)*

## 8. Referenzen

*   [Quarkus Guides](https://quarkus.io/guides/)
*   [React-Dokumentation](https://react.dev/)
*   [Vite-Dokumentation](https://vitejs.dev/)
*   [Tailwind CSS-Dokumentation](https://tailwindcss.com/docs)
