# Library QA Lab

A practice project for **JUnit, Postman/Newman, JMeter and GitHub Actions with Allure**.
The system under test (SUT) is a small library REST API (Spring Boot). It starts with a books catalog
and is built to grow: members, loans, auth, a web frontend (see [Roadmap](#roadmap)).

```
                       ┌──────────── GitHub Actions (ci.yml) ─────────────┐
 app/      Spring Boot │ build ─ ./mvnw verify ─ JUnit + JaCoCo ──┐       │
           REST API    │   │                                     │       │
 postman/  collection  │   ├─ api-tests ─ jar ▶ Newman ──────────┼─▶ report ─▶ GitHub Pages
 jmeter/   load test   │   └─ perf-smoke ─ jar ▶ JMeter ▶ SLA ───┘   (Allure + coverage + JMeter)
                       └──────────────────────────────────────────────────┘
```

| Folder | What | Tool |
|---|---|---|
| [app/](app/) | The SUT and its JUnit tests (unit, slice, integration) | Maven, JUnit Jupiter, Mockito, AssertJ, RestAssured, JaCoCo |
| [postman/](postman/) | API tests as a Postman collection, run headless with Newman | Postman, Newman |
| [jmeter/](jmeter/) | Load test plus SLA gates | JMeter |
| [.github/](.github/) | CI pipeline, reusable "start SUT" action, Allure categories | GitHub Actions, Allure |
| [docs/](docs/) | [LEARNING-PATH.md](docs/LEARNING-PATH.md) (exercises) and [CHEATSHEET.md](docs/CHEATSHEET.md) (Jest/Jenkins → new tools) | |

All three tools write **Allure results**, so CI merges them into **one** report.

## Prerequisites

| Tool | Version | Install (macOS) |
|---|---|---|
| JDK | 25 or newer (27 works) | already installed |
| Node.js | 24 | already installed |
| JMeter | 5.6.3 | `brew install jmeter` (brings its own JDK 21, see below) |
| Allure Report | 3.x | nothing to install, runs with `npx allure@3` (Node.js) |
| Postman app | any | optional, for editing the collection |
| PostgreSQL | 18 | already installed (`/Library/PostgreSQL/18`), the app's database |
| Docker Desktop | any | already installed; must be **running** for `./mvnw verify` (Testcontainers) |

Maven is not needed: the project uses the Maven Wrapper (`./mvnw`), which downloads Maven on first use.

> **One project, two JDKs.** The app is compiled for Java 25 (`maven.compiler.release=25`), so any JDK 25+
> can build and run it. JMeter 5.6.3 bundles Groovy 3.0.20, which **fails on JDK 27** (verified): JSR223/Groovy
> elements die with "Unsupported class file major version 71". JDK 21 is the safe choice for JMeter. Homebrew's
> `jmeter` already runs on its own JDK 21, and CI installs JDK 21 for JMeter and JDK 25 for the app.

## Quick start

One-time database setup (asks for the `postgres` password you chose when installing PostgreSQL):

```bash
/Library/PostgreSQL/18/bin/psql -h localhost -U postgres \
  -c "CREATE ROLE library LOGIN PASSWORD 'library'" \
  -c "CREATE DATABASE library OWNER library"
```

Then start the app:

```bash
cd app
./mvnw spring-boot:run -Dspring-boot.run.profiles=test-support
```

- API: <http://localhost:8080/api/books>
- Swagger UI: <http://localhost:8080/swagger-ui.html> (OpenAPI spec: `/v3/api-docs`, importable into Postman)
- Database: `/Library/PostgreSQL/18/bin/psql -h localhost -U library library` (password `library`), or pgAdmin 4
- Health: <http://localhost:8080/actuator/health>

On the first start, Flyway creates the schema and seeds 21 books ([V2__seed_books.sql](app/src/main/resources/db/migration/V2__seed_books.sql)).
The data is persistent: it survives restarts, so whatever Postman or JMeter created is still there next time.
With the `test-support` profile, `POST /api/test/reset` restores the seed state.

### The API (Phase 1)

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/api/books?title=&author=&page=0&size=20` | 200, page of books | 400 if `size` is not 1..100 |
| GET | `/api/books/{id}` | 200 | 404 |
| POST | `/api/books` | 201 + `Location` header | 400 validation, 409 duplicate ISBN |
| PUT | `/api/books/{id}` | 200 | 400, 404, 409 |
| DELETE | `/api/books/{id}` | 204 | 404 |

Business rules (good targets for boundary value analysis):
- `isbn`: valid **ISBN-13** with checksum, prefix 978/979. Hyphens and spaces are allowed and get stripped, so `978-3-16-148410-0` and `9783161484100` are the same book (409).
- `publishedYear`: from 1450 up to the **current year**. The year comes from an injected `Clock`, so tests can fix "now".
- `copiesTotal` ≥ 0, `title` 1..200 chars, `author` 1..120 chars.

Errors are [RFC 9457 problem details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`):

```json
{ "status": 400, "title": "Bad Request", "detail": "Validation failed", "instance": "/api/books",
  "errors": [ { "field": "isbn", "message": "must be a valid ISBN-13" } ] }
```

## Running the tests locally

### JUnit (in `app/`)

```bash
./mvnw clean verify                        # everything: unit + slice (Surefire) and *IT (Failsafe), coverage gate
./mvnw test                                # only unit + slice tests (fast)
./mvnw test -Dgroups=smoke                 # only tests tagged @Tag("smoke")
./mvnw test -Dtest=IsbnValidatorTest       # one class (Jest: test.only / -t)
./mvnw verify -Dit.test=BookApiIT          # one integration test class
npx allure@3 open target/allure-results     # generate and open the Allure report
open target/site/jacoco/index.html         # coverage report (like Istanbul's lcov-report)
```

`BookRepositoryTest` and `BookApiIT` start their own throwaway PostgreSQL in Docker (Testcontainers, see
[PostgresTestcontainer](app/src/test/java/dev/qalab/library/support/PostgresTestcontainer.java)), so Docker Desktop must
be running. They never touch your local `library` database. The other tests need neither Docker nor a database.

Use `clean` before re-running if you look at Allure: the results folder is not cleared automatically, so old runs
would show up as retries.

| Test class | Level | What it shows |
|---|---|---|
| [IsbnValidatorTest](app/src/test/java/dev/qalab/library/common/IsbnValidatorTest.java) | Unit | `@ParameterizedTest`, `@CsvSource`, `@Nested`, equivalence classes, boundary values |
| [BookServiceTest](app/src/test/java/dev/qalab/library/book/BookServiceTest.java) | Unit with mocks | Mockito `@Mock`, `when/thenReturn`, `verify`, `ArgumentCaptor`, fixed `Clock`, `Allure.step` |
| [BookControllerTest](app/src/test/java/dev/qalab/library/book/BookControllerTest.java) | Web slice | `@WebMvcTest`, MockMvc, `@MockitoBean`, HTTP contract and error JSON |
| [BookRepositoryTest](app/src/test/java/dev/qalab/library/book/BookRepositoryTest.java) | Persistence slice | `@DataJpaTest` against the real Flyway migrations on PostgreSQL (Testcontainers, `@ServiceConnection`) |
| [BookApiIT](app/src/test/java/dev/qalab/library/book/BookApiIT.java) | Integration/API | `@SpringBootTest(RANDOM_PORT)`, RestAssured, JSON schema, `@Step` methods, HTTP calls attached in Allure |

### Postman (in `postman/`, app must be running)

```bash
npm ci
npm test                 # = test:api (folders 00-02) + test:data (folder 03 with data/books.csv)
npx allure@3 open allure-results
```

In the Postman app: **Import** `library-api.postman_collection.json` and `env/local.postman_environment.json`,
select the "local" environment, then run requests or the whole collection (Runner). After changing things in the app,
**export** the collection (v2.1) over the file and commit it, because the JSON file is the source of truth.

### JMeter (in `jmeter/`, app must be running)

```bash
jmeter -t library-load.jmx                 # GUI: for building and debugging (enable "View Results Tree")
jmeter -n -t library-load.jmx -l results/results.jtl -j results/jmeter.log -e -o report \
       -Jthreads=10 -Jduration=60          # non-GUI: for real runs (report/ must not exist yet)
open report/index.html                     # HTML dashboard
node scripts/sla-check.mjs --stats report/statistics.json --out results/allure-results --p95 500 --error-pct 1
```

Properties you can override with `-J`: `host`, `port`, `threads`, `writers`, `rampup`, `duration`, `maxResponseMs`.

## CI: GitHub Actions + Allure

[ci.yml](.github/workflows/ci.yml) runs on every push to `main`, on pull requests and manually ("Run workflow",
with inputs for the JMeter load).

| Job | Needs | Does | Artifacts |
|---|---|---|---|
| **Build + JUnit** | | `./mvnw verify` (tests + JaCoCo gate ≥ 80% lines, PostgreSQL via Testcontainers) | `app-jar`, `allure-results-junit`, `coverage-report` |
| **API tests** | build | starts PostgreSQL (service container) and the jar ([start-sut](.github/actions/start-sut/action.yml)), runs Newman | `allure-results-postman` |
| **Performance smoke** | build | starts PostgreSQL and the jar, runs JMeter, checks SLA gates | `allure-results-perf`, `jmeter-report` |
| **Allure report** | all | merges all results, adds history + metadata, generates the report (Allure 3), comments on PRs, publishes to `gh-pages` (only from `main`) | `allure-report` |

The report job also runs when tests failed (`if: !cancelled()`), because that's when you need the report most.
Trend charts appear from the second run on: the previous report's `history.jsonl` is copied in before generating.
Report name, history and failure categories are set in [allurerc.mjs](.github/allure/allurerc.mjs).

On pull requests the job also posts the results as a comment ([allure-action](https://github.com/allure-framework/allure-action))
and updates it on every push. Pages only ever shows `main`; a PR's full report is the `allure-report` artifact of its run:
download, unzip, `npx allure@3 open allure-report`.

## Publish on GitHub

1. Create an **empty public** repository on GitHub, e.g. `library-qa-lab` (no README, no .gitignore).
   GitHub Pages is free for public repos only.
2. Commit and push:
   ```bash
   git add .
   git commit -m "Library QA Lab: Phase 1"
   git remote add origin https://github.com/<your-user>/library-qa-lab.git
   git push -u origin main
   ```
3. Watch the run under **Actions**. The first run on `main` creates the `gh-pages` branch.
4. **Settings → Pages → Build and deployment**: Source "Deploy from a branch", branch `gh-pages`, folder `/ (root)`.
5. A minute later the report is at `https://<your-user>.github.io/library-qa-lab/`,
   coverage at `…/coverage/`, the JMeter dashboard at `…/jmeter/`.

If the publish step fails with a 403: **Settings → Actions → General → Workflow permissions** must allow the
`GITHUB_TOKEN` to write (or at least not block the job-level `contents: write`).

## Lessons already learned building this

These came up while building Phase 1 and are typical traps:

- **Tool JDK ≠ app JDK.** JMeter 5.6.3's Groovy breaks on new JDKs. And it fails *silently*: the thread dies,
  but the summary still says 0% errors. Always check `jmeter.log` for `ERROR` lines.
- **AspectJ weaving scope.** The Allure `@Step` weaver must only weave test step classes (`*Steps`). Otherwise Spring's
  own aspects (from `spring-aspects`, pulled in by Spring Data JPA) get woven into production classes during tests. See [aop.xml](app/src/test/resources/META-INF/aop.xml).
- **Locale.** Validation messages followed the JVM language (German locally, English in CI). The app now pins
  English (`spring.web.locale`), and the test JVM runs with `-Duser.language=en`.
- **Data-driven tests in Allure.** Allure identifies a test by its history id. Newman builds it from the request URL, so
  10 CSV rows against the same URL would show up as 1 test with 9 retries. That's why the data-driven
  request has a `?dataRow=N` marker.
- **Shared test data under load.** Readers searching "test" found books that writers deleted a moment later, which gave
  sporadic 404s. Load test data must not overlap between thread groups (exercise M4).

## Roadmap

| Phase | Adds | New test topics |
|---|---|---|
| **1 (done)** | Books catalog | All basics, see above |
| 2 | Members + loans: max 3 active loans, 21-day loan period, 1 extension, late fee €0.50/day capped at €10, no new loans with fees > €5 | BVA with `@MethodSource`, time-based tests with `Clock`, stateful Postman flows, JMeter transactions |
| 3 | Spring Security + JWT | Postman auth (token fetch in collection pre-request), JMeter token correlation, `@WithMockUser` |
| 4 | PostgreSQL + Testcontainers (**done**), Dockerfile, docker-compose | Testcontainers in JUnit, service containers in Actions |
| 5 | Small web frontend | Playwright E2E (your home turf), contract tests against OpenAPI, nightly load test |
