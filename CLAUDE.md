# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

feed2mastodon is an RSS-to-Mastodon bot built with **Quarkus** (Java) and includes a React web UI. It monitors RSS/Atom feeds and automatically posts new entries to Mastodon, with features for:
- Duplicate detection via PostgreSQL
- AI integration (Google Gemini) for content processing
- Home Assistant integration
- Telegram bot support
- Mail OAuth integration
- URL shortening utilities

## Tech Stack

- **Backend:** Java 25 with Quarkus 3.34.6, Maven
- **Database:** PostgreSQL with Flyway schema migrations
- **Frontend:** React 19 with TypeScript, Vite, Tailwind CSS
- **AI:** Google Gemini API for embeddings and content generation
- **HTTP Client:** Quarkus REST Client with Jackson serialization

## Build & Development Commands

### Backend (Java/Quarkus)
```bash
# Development mode with live reload (http://localhost:8080)
./mvnw quarkus:dev

# Run all tests
./mvnw test

# Run a single test file
./mvnw test -Dtest=ClassName

# Run integration tests only (marked with IT suffix)
./mvnw verify -DskipITs=false

# Package JAR (builds webui too via Quinoa plugin)
./mvnw clean package

# Build native executable (requires GraalVM)
./mvnw package -Dnative

# Run Sonar code analysis (used in CI)
./mvnw clean verify sonar:sonar -Dsonar.projectKey=hexix:feed2mastodon
```

### Frontend (Web UI)
```bash
# Dev server with HMR (runs via Quinoa during quarkus:dev)
cd src/main/webui
npm install
npm run dev

# Build for production
npm run build

# Type check only
npm run build -- --mode check
```

## Project Structure

**Java Package Organization:**
- `de.hexix.mastodon.*` — Mastodon API client and REST resources
- `de.hexix.feed.*` — Feed reading and fetching logic
- `de.hexix.ai.bot.*` — AI features (Gemini) and Telegram bot
- `de.hexix.homeassistant.*` — Home Assistant integration
- `de.hexix.mail.*` — Mail and OAuth handling
- `de.hexix.scheduler.*` — Scheduled tasks and feed monitoring
- `de.hexix.user.*` — User management
- `de.hexix.util.*` — Utility functions

**Database:**
- Migrations stored in `src/main/resources/db/migration/` using Flyway
- Entities use Hibernate Panache for ORM
- `PostedEntry` model tracks already-posted feed entries to prevent duplicates

**Web UI:**
- Entry point: `src/main/webui/src/main.tsx`
- Vite config in `src/main/webui/vite.config.ts`
- Built artifacts served by Quarkus via Quinoa plugin

**Configuration:**
- `src/main/resources/application.properties` — primary config, uses environment variable substitution
- Key properties: `feed.url`, `mastodon.api.url`, `mastodon.access.token`, PostgreSQL credentials, Gemini API key

## Architecture Notes

**REST Endpoints:**
- Quarkus REST (Jakarta) replaces ResteasyClassic—use `@jakarta.ws.rs.*` annotations
- Microprofile Config injection for runtime configuration
- Jackson for JSON serialization via `quarkus-rest-jackson`

**Data Layer:**
- Hibernate Panache provides `PanacheEntity` base for active record pattern
- Flyway handles schema versioning at startup (`quarkus.flyway.migrate-at-start=true`)
- No code-first schema generation—Flyway scripts are authoritative

**External APIs:**
- REST clients configured via `mp-rest/url` properties
- Ollama integration for local embeddings
- Google Gemini for semantic similarity and content generation
- Mastodon API client wraps public Mastodon instance endpoints

**Scheduled Jobs:**
- Uses Quarkus Scheduler (`@Scheduled` annotation) for periodic feed checks
- Camel integration for complex routing (Telegram, timers, direct channels)

## Testing Strategy

**Test Structure:**
- `*Test.java` — unit tests run with `mvn test`
- `*IT.java` — integration tests run with `mvn verify`, require live DB/services
- `MockFeedResource` — mock Mastodon feed server for testing

**Key Test Classes:**
- `FeedFetcherTest` — tests feed reading and entry parsing
- `GoogleAiTest`, `KiTestCases` — AI integration tests (requires GEMINI_ACCESS_TOKEN)
- `JsoupTest` — HTML parsing tests
- `GreetingResourceTest` / `GreetingResourceIT` — HTTP endpoint tests

**Running Tests:**
- Skip integration tests in CI: `mvn test` (default `skipITs=true`)
- Run both: `mvn verify`

## CI/CD

**GitHub Actions Workflows** (in `.github/workflows/`):
- `build.yaml` — Maven build, tests, SonarQube analysis
- `push-image.yaml` — Docker image build and push on release
- `codeql.yml` — CodeQL security scanning
- `manuel-push.yml` — Manual workflow dispatcher

**Key Behaviors:**
- Auto-versioning via `maven-deploy-plugin` (see version bumps in recent commits)
- SonarQube integration for code quality gates
- Docker images pushed to registry on tags

## Configuration Management

**Environment Variables (used in `application.properties`):**
- `MASTODON_ACCESS_TOKEN` — required for posting
- `GEMINI_ACCESS_TOKEN` — Google Gemini API key
- `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` — database credentials
- `TELEGRAM_BOT_TOKEN` — for Telegram bot feature
- `OL_HOST`, `OL_PORT` — Ollama service (local embeddings)

**Database Setup:**
- Flyway auto-runs migrations at startup
- `quarkus.flyway.baseline-on-migrate=true` — allows baseline on non-empty DB
- PostgreSQL-specific config in `quarkus.datasource.jdbc.url`

## Common Workflows

**Adding a New REST Endpoint:**
1. Create a resource class annotated with `@Path` in `de.hexix.mastodon.resource.*`
2. Use `@GET`, `@POST`, etc. with Jakarta REST annotations (not Resteasy)
3. Inject dependencies via `@Inject` (Quarkus CDI)
4. Write tests in `*Test.java` (unit) or `*IT.java` (integration)

**Adding a Scheduled Task:**
1. Use `@Scheduled` from Quarkus (specify `cron` or fixed delay)
2. Inject database/API clients
3. Test with `quarkus:dev` to verify execution

**Database Changes:**
1. Create a new migration script in `src/main/resources/db/migration/V{N}__description.sql`
2. Flyway runs at startup in sequence
3. Never modify existing migration scripts

**Updating Dependencies:**
- Quarkus BOM version in `pom.xml` controls platform versions
- Keep frontend deps up-to-date via `npm audit fix` in webui
- Review Dependabot PRs (configured in `.github/dependabot.yml`)

## Performance & Deployment

**Native Image:**
- Requires GraalVM; Quarkus handles most reflection configuration automatically
- Build with `mvn package -Dnative` or container build with `-Dquarkus.native.container-build=true`
- Faster startup, lower memory than JVM mode

**Development Tips:**
- Dev UI at `http://localhost:8080/q/dev` during `quarkus:dev`
- Hot reload works for Java code and web UI (Vite HMR)
- Logs output to console in dev mode
- Debugger port available on 5005 (Quarkus default)
