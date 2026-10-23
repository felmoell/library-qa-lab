# Learning path

Every module starts from a **worked example** that already exists in the repo. Read it, run it, then do the
exercises. Each exercise has a goal, a hint, and a "done when" so you know when to stop.
Work on a branch per module (`git switch -c learn/junit-j2`) and open a PR. Then CI and the Allure report
review your work, just like at a client.

Order: Module 0 first, then J, P, M in any order, G last (it ties everything together).

---

## Module 0: Orientation (30 min)

1. Start the app ([README](../README.md#quick-start)), click through Swagger UI, then send a few requests with curl and look at the `book` table in psql or pgAdmin.
2. Run `./mvnw clean verify` in `app/`, then `npx allure@3 open target/allure-results`. Find:
   the **Behaviors** tab (Epic → Feature → Story), the **Suites** tab (by class), one test with steps and an HTTP attachment.
3. With the app running: `npm test` in `postman/`, and the non-GUI JMeter command in `jmeter/`.

**Done when:** you have seen all three tools pass locally and can say which test level each JUnit class is.

---

## Module J: JUnit

Worked examples: the five test classes in the [README table](../README.md#junit-in-app).
Look up anything unfamiliar in the [cheat sheet](CHEATSHEET.md#jest--junit-jupiter).

### J1: Parameterized tests with `@MethodSource`
Add a test to `IsbnValidatorTest` that **generates** 100 valid ISBNs (reuse `TestIsbn.random()`) and checks them all.
- Hint: `@ParameterizedTest @MethodSource("randomIsbns")` + `static Stream<String> randomIsbns()`.
- **Done when:** 100 extra test cases show up in Allure, each with its ISBN in the name.

### J2: Cover the reset hook
`TestSupportController` has 0% coverage (see JaCoCo). Write an integration test for it.
- Hint: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ActiveProfiles("test-support")`. Create a book, reset, assert `totalElements == 21`.
- Pitfall: Spring caches contexts. A different profile means a second context, which costs startup time. Notice the build time.
- **Done when:** JaCoCo shows `TestSupportController` covered and line coverage goes up.

### J3: The missing web-layer error path
`GlobalExceptionHandler.handleInvalidField` (future publication year) is only tested at service level.
- Add a `BookControllerTest` case where the mocked service throws `InvalidFieldException("publishedYear", ...)`.
- **Done when:** the test asserts status 400 **and** `errors[0].field == "publishedYear"`.

### J4: AssertJ-style MockMvc
Rewrite `unknownIdReturns404` with `MockMvcTester` (Spring 6.2+) instead of `MockMvc`.
- Hint: `@Autowired MockMvcTester mvc;` then `assertThat(mvc.get().uri("/api/books/999")).hasStatus(404).bodyJson()...`
- **Done when:** both styles exist side by side and you have an opinion on which reads better.

### J5: Mockito deeper
- Verify call **order**: in `update()`, `findById` happens before `save` (`InOrder`).
- Use `@Spy` on a real object once and explain when a spy is a code smell.
- **Done when:** both tests pass. Then deliberately swap the order in `BookService` and watch the test fail.

### J6: Test selection like a pro
- Add `@Tag("regression")` to a few tests. Run only those: `./mvnw test -Dgroups=regression`.
- Create a Maven profile `smoke` in `pom.xml` that sets `<groups>smoke</groups>` for Surefire **and** Failsafe.
- **Done when:** `./mvnw verify -Psmoke` runs only smoke tests at both levels.

### J7: Parallel execution
Set `junit.jupiter.execution.parallel.enabled=true` and `junit.jupiter.execution.parallel.mode.default=concurrent`
in `junit-platform.properties`.
- Find out which tests break and why (hint: shared seed data, counts).
- Fix them with `@ResourceLock` or `@Isolated`, not by turning parallelism off.
- **Done when:** the build is green in parallel mode and faster than before (compare the Allure timeline).

### J8: Allure annotations
- Add `@Owner`, `@Link`, `@Issue("42")` and set `allure.link.issue.pattern=https://github.com/<you>/library-qa-lab/issues/{}` in `allure.properties`.
- Add an `@Attachment` method to `BookApiSteps` that attaches a short text summary of each created book.
- **Done when:** the links are clickable in the report and the attachment shows inside the step.

### J9: TDD a feature: sorting
Add `?sort=title` / `?sort=-publishedYear` to `GET /api/books` (whitelist of fields, 400 for unknown ones).
- Write the failing tests **first**, at controller level (400 for `sort=foo`), at repository level (order), and in the IT.
- **Done when:** all levels are green, coverage ≥ 80%, and the Postman collection has a request for it too.

---

## Module P: Postman / Newman

Worked example: [library-api.postman_collection.json](../postman/library-api.postman_collection.json):
smoke checks, a chained CRUD flow, negative tests, a data-driven folder, and a collection-level response-time test.

### P1: GUI ↔ CLI
Import the collection and the `local` environment into the Postman app. Run it with the **Collection Runner**, then with
`npm test`. Compare the output.
- **Done when:** you can explain where variables come from (collection vs environment vs data vs local `pm.variables`).

### P2: OpenAPI import
Import `http://localhost:8080/v3/api-docs` into Postman as a new collection.
- **Done when:** you have listed what the generated collection gives you for free and what it lacks (tests, chaining, data).

### P3: Pagination and test data state
Add a folder "04 Paging": `size=5` → `totalPages == 5`; `page=4` → 1 item; `page=99` → empty `content`.
- Pitfall: these numbers only hold for the seed data (21 books). Make the folder robust with a first request that calls the
  reset utility, and run CI with the `test-support` profile (it already does).
- **Done when:** the folder passes twice in a row, even after the CRUD folder ran.

### P4: DRY scripts
The ISBN generator lives in a single request's pre-request script. Move it to the **collection level** (so every request can
use it), or to a Postman package (`pm.require`). Check whether Newman supports your choice.
- **Done when:** the logic exists once and both GUI and Newman still pass.

### P5: More data-driven boundaries
Extend `data/books.csv`: title with exactly 200 chars (201 → 400), author 120/121, a 979-prefixed ISBN.
- Hint: long strings in CSV are ugly. Use a placeholder like `{{title200}}` and fill it in a pre-request script with `"x".repeat(200)`.
- **Done when:** every new row is its own test in Allure.

### P6: Explore and write down the answers
What does the API do with `Accept: text/xml`? With `Content-Type: text/plain` on POST? With a JSON body that has extra
fields? With `"publishedYear": "2020"` as a string? Write the answers as tests and mark surprises.
- **Done when:** you have at least 4 new tests, and a note for each surprise saying whether you consider it a bug.

### P7: A failing test, end to end
Change one expected value so a test fails. Look at it in the console, in Allure (categories!), and in the CI job summary.
Then revert.

---

## Module M: JMeter

Worked example: [library-load.jmx](../jmeter/library-load.jmx). It has two thread groups (readers, librarians), CSV data,
a JSON Extractor for correlation, an If Controller, a JSR223/Groovy pre-processor, and Response/JSON/Duration assertions.
[sla-check.mjs](../jmeter/scripts/sla-check.mjs) turns the dashboard statistics into pass/fail gates.

### M1: Debugging in the GUI
Open the plan in the GUI, enable "View Results Tree", set threads = 1 and duration = 5. Inspect a request/response pair,
and see `bookId` change in the "Debug" view (add a Debug Sampler).
- **Done when:** you can show where the extracted id goes into the next request.

### M2: Find the knee
Run increasing loads locally: `-Jthreads=10, 50, 100, 200` with `-Jduration=60`. Note throughput and p95 for each.
- **Done when:** you have a small table and can point to where throughput stops growing while response time climbs.

### M3: Transactions
Wrap POST + DELETE in a **Transaction Controller** named "Manage book".
- **Done when:** the dashboard and the SLA gates show the transaction as its own row.

### M4: Reproduce a race condition
Change the librarians' book title back to "Load **Test** Book ...". Readers searching for "test" now sometimes open a book
that a librarian just deleted.
- **Done when:** you have seen the 404s, can explain them, and have a rule for load-test data isolation.

### M5: Silent failures
Break the Groovy script (a typo). Run the test: the summary still says 0% errors, because the librarian thread just died.
- Add a CI step that fails the job if `jmeter.log` contains `ERROR`.
- **Done when:** the broken script turns the perf job red.

### M6: A throughput gate
Extend `sla-check.mjs` with `--min-throughput` (req/s for "Total").
- **Done when:** it shows up as an extra gate in Allure, and `workflow_dispatch` can pass a value.

### M7: Extractors compared
Replace the JSON Extractor with a Regular Expression Extractor and then with a Boundary Extractor.
- **Done when:** you can say when you would use which.

---

## Module G: GitHub Actions + Allure

Worked example: [ci.yml](../.github/workflows/ci.yml) + [start-sut](../.github/actions/start-sut/action.yml).
Read it side by side with the [Jenkins/Bitbucket mapping](CHEATSHEET.md#jenkins--bitbucket--github-actions).

### G1: First publish
Follow [Publish on GitHub](../README.md#publish-on-github). Push a second commit.
- **Done when:** the Pages report shows trend charts (they need at least two runs).

### G2: Manual parameters
Add a `p95` input to `workflow_dispatch` and pass it to `sla-check.mjs` (the `threads` input shows how).
- **Done when:** a manual run with `p95=5` fails the perf job and the failures appear under "SLA violations".

### G3: Build matrix
Run the build job on Java 25 **and** 27 (`strategy.matrix`).
- Pitfall 1: artifact names must be unique per matrix leg (`allure-results-junit-${{ matrix.java }}`), and you must decide
  which leg's jar the later jobs use.
- Pitfall 2: both legs produce the same test ids, so Allure merges them into one test with a "retry". Decide how to keep them
  apart (only report one leg? one report per leg? a label per leg?).
- **Done when:** both legs are green and the report tells you honestly what ran on which Java version.

### G4: Nightly performance run
Add `on: schedule` (cron) that runs only the perf job with a higher load.
- Hint: `if: github.event_name == 'schedule'` on jobs, or a separate workflow with a reusable workflow (`workflow_call`).
- **Done when:** a scheduled run happened (or you triggered it with `workflow_dispatch`), with its results in the report.

### G5: Branch protection
Protect `main`: require PRs and the checks "Build + JUnit" and "API tests (Postman/Newman)".
- **Done when:** a PR with a failing test cannot be merged.

### G6: Allure quality gate
Add a `qualityGate` to [allurerc.mjs](../.github/allure/allurerc.mjs), e.g. `{ rules: [{ maxFailures: 0 }] }`.
The PR comment action then also creates an "Allure Quality Gate" check.
- Pitfall: when the gate fails, `allure generate` fails the step, so the steps after it (artifact upload, PR comment)
  are skipped. Decide which of them must still run.
- **Done when:** a PR with a failing test shows a red quality gate check and the comment says which rule failed.

---

## Phase 2 and later

The [roadmap](../README.md#roadmap) lists the next features. Suggested way to work: take the loan rules as a
**specification**, derive test cases first (equivalence classes, boundaries, state transitions: available → loaned →
extended → overdue → returned), and write them as JUnit/Postman tests before implementing. Ask Claude to implement a phase
when you want to compare your tests against a finished implementation.
