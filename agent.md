# feed2mastodon - AI Agent Context

## Projektübersicht
`feed2mastodon` ist eine Full-Stack-Anwendung, die primär als Bot dient, um RSS/Atom-Feeds zu überwachen und neue Einträge automatisch als "Toots" auf Mastodon zu veröffentlichen. Zudem bietet sie ein webbasiertes Frontend (React/Vite), um weitere Funktionen wie Server-Status, Tankstellen-Informationen und mehr anzuzeigen.

## Wichtige Technologien
### Backend (Java)
- **Sprache:** Java 25
- **Framework:** Quarkus 3.34.3 (REST, Scheduler, Hibernate ORM Panache, JDBC PostgreSQL, Flyway)
- **Datenbank:** PostgreSQL (zur Vermeidung von Duplikaten und Speicherung von Konfigurationen)
- **Feed-Verarbeitung:** ROME (RSS/Atom), Jsoup (HTML Parsing)
- **KI-Integration:** Google Gemini (`google-genai` Bibliothek) für Inhaltsgenerierung und Embeddings
- **Weitere APIs:** Mastodon API (via REST Client), Mail (Jakarta Mail), Telegram (Camel), Home Assistant (via REST)

### Frontend (React / TypeScript)
- **Framework:** React 19 (mit React Router)
- **Build-Tool:** Vite (integriert via Quarkus Quinoa)
- **Sprache:** TypeScript
- **Styling:** Tailwind CSS, Framer Motion für Animationen
- **UI-Komponenten/Icons:** Lucide React, Recharts (für Diagramme)
- **Verzeichnis:** `src/main/webui`

## Architektur & Kernkomponenten
### Backend
- `FeedReaderService`: Liest und verarbeitet die RSS/Atom-Feeds.
- `FeedToTootScheduler`: Plant die regelmäßige Ausführung des Feed-Abrufs.
- `PostedEntry` / `MonitoredFeed`: Entitäten (Panache) zur Speicherung bereits geposteter Einträge und der Feed-Konfiguration.
- `mastodon`, `ai`, `mail`, `telegram`, `homeassistant`, `urlshortener`: Spezifische Integrationen und Service-Module.

### Frontend
- **Routing:** Definiert in `App.tsx` (Routen: `/`, `/server-status`, `/impressum`, `/datenschutz`, `/mail-test`, `/tanken`).
- **Komponenten:** Lazy-Loading für Seiten (z.B. `Home`, `ServerStatus`, `Tanken`).
- **Integration:** Das Frontend wird durch die Quarkus-Quinoa-Extension während des Maven-Builds automatisch mitgebaut und ausgeliefert.

## Konfiguration (application.properties)
- `feed.url`: Zu überwachender Feed.
- `mastodon.api.url` / `mastodon.access.token`: Mastodon-Zugangsdaten.
- `gemini.access.token`: Google Gemini API-Key.
- Datenbank-Credentials (`quarkus.datasource.*`).

## Entwicklungs-Workflow
- **Backend & Frontend (Dev-Modus):** `./mvnw quarkus:dev` (startet das Quarkus Backend und den Vite Dev-Server für das Frontend via Quinoa gleichzeitig).
- **Backend Build (inklusive Frontend):** `./mvnw package`
- **Native Build:** `./mvnw package -Dnative`
- **Frontend isoliert (optional):** Im Ordner `src/main/webui` kann `npm run dev` ausgeführt werden.

## Hinweise für den Agenten
- Verwende Panache (Active Record oder Repository Pattern) für Datenbankoperationen im Backend.
- Beachte asynchrone und reaktive Muster im Backend (Quarkus REST ist reaktiv).
- Änderungen an der Datenbank erfordern Flyway-Migrationen (`src/main/resources/db/migration`).
- KI-Features können für Zusammenfassungen oder Hashtag-Generierung genutzt werden.
- Bei Änderungen am Frontend: Das Frontend nutzt funktionale React-Komponenten, TypeScript, TailwindCSS für das Styling und React Router für die Navigation.
