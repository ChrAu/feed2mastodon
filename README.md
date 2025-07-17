# feed2mastodon | RSS to Mastodon Bot

Dieses Projekt ist ein in Java mit Quarkus entwickelter Bot, der automatisch neue Einträge aus einem RSS/Atom-Feed liest und sie auf einem Mastodon-Konto veröffentlicht. Er nutzt eine Datenbank, um den Überblick über bereits geteilte Beiträge zu behalten und Duplikate zu vermeiden.

## Hauptfunktionen

-   **Feed-Überwachung:** Überwacht einen beliebigen RSS/Atom-Feed auf neue Einträge.
-   **Automatisches Posten:** Veröffentlicht neue Feed-Einträge als "Toots" auf einer konfigurierten Mastodon-Instanz.
-   **Vermeidung von Duplikaten:** Eine PostgreSQL-Datenbank speichert die GUIDs der bereits geposteten Einträge (`PostedEntry`), um sicherzustellen, dass jeder Beitrag nur einmal geteilt wird.
-   **KI-Integration (optional):** Nutzt Google Gemini, um Inhalte zu verarbeiten. Die Tests (`GoogleAiTest.java`) demonstrieren Funktionen wie:
    -   **Inhaltsgenerierung:** Automatische Erstellung von Social-Media-Posts zu einem Thema im JSON-Format, inklusive Weblinks und Hashtags.
    -   **Semantische Ähnlichkeit:** Berechnung der Ähnlichkeit zwischen Texten mittels Embeddings, um potenziell relevante Inhalte zu filtern oder zu empfehlen.

## Konfiguration

Für den Betrieb der Anwendung müssen die folgenden Eigenschaften in der `application.properties` oder als Umgebungsvariablen konfiguriert werden:

-   `feed.url`: Die URL des zu überwachenden RSS/Atom-Feeds.
-   `mastodon.api.url`: Die URL Ihrer Mastodon-Instanz (z.B. `https://mastodon.social`).
-   `mastodon.access.token`: Ihr Mastodon-Zugangstoken mit den erforderlichen Berechtigungen zum Posten.
-   `gemini.access.token`: Ihr API-Schlüssel für Google Gemini (optional, für KI-Funktionen).
-   **Datenbank:**
    -   `quarkus.datasource.jdbc.url`: Die JDBC-URL Ihrer PostgreSQL-Datenbank.
    -   `quarkus.datasource.username`: Der Benutzername für die Datenbank.
    -   `quarkus.datasource.password`: Das Passwort für die Datenbank.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/feed2mastodon-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- Scheduler ([guide](https://quarkus.io/guides/scheduler)): Schedule jobs and tasks

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
