# Cheat sheet: from your current stack to this one

## Jest → JUnit Jupiter

JUnit "5" and "6" are the same programming model (Jupiter); Spring Boot 4 brings JUnit 6.
Assertions come from **AssertJ**, mocks from **Mockito**.

| Jest | JUnit Jupiter + AssertJ + Mockito | In this repo |
|---|---|---|
| `describe("x", ...)` | `@Nested class X` + `@DisplayName("x")` | `IsbnValidatorTest.Accepts` |
| `it(...)` / `test(...)` | `@Test void name()` | everywhere |
| `test.each([...])` | `@ParameterizedTest` + `@CsvSource` / `@ValueSource` / `@MethodSource` | `IsbnValidatorTest` |
| `beforeEach` / `afterEach` | `@BeforeEach` / `@AfterEach` | `BookServiceTest.setUp` |
| `beforeAll` | `@BeforeAll` (static method) | |
| `test.skip` | `@Disabled("reason")` | |
| `test.only`, `-t pattern` | `./mvnw test -Dtest=Class#method` | |
| tags via naming / `--testNamePattern` | `@Tag("smoke")` + `-Dgroups=smoke` | `IsbnValidatorTest`, `BookApiIT` |
| `expect(a).toBe(b)` | `assertThat(a).isEqualTo(b)` | |
| `expect(list).toContain(x)` | `assertThat(list).contains(x)` / `containsExactly(...)` | `BookRepositoryTest` |
| `expect(obj).toMatchObject({...})` | `assertThat(obj).extracting(A::a, A::b).containsExactly(...)` | `BookServiceTest` |
| `expect(fn).toThrow(Err)` | `assertThatThrownBy(() -> ...).isInstanceOf(Err.class)` | `BookServiceTest` |
| `jest.fn()`, `jest.mock(...)` | `@Mock` + `@ExtendWith(MockitoExtension.class)` | `BookServiceTest` |
| mock inside the app container | `@MockitoBean` (replaces a Spring bean) | `BookControllerTest` |
| `mockReturnValue(x)` | `when(mock.call()).thenReturn(x)` | |
| `mockImplementation(fn)` | `thenAnswer(invocation -> ...)` | `BookServiceTest` |
| `toHaveBeenCalledWith(x)` | `verify(mock).call(x)` | |
| `not.toHaveBeenCalled()` | `verify(mock, never()).call(any())` / `verifyNoInteractions` | |
| `mock.calls[0][0]` | `ArgumentCaptor` | `BookServiceTest.savedBook` |
| `jest.useFakeTimers()` | inject `java.time.Clock`, use `Clock.fixed(...)` in tests | `ClockConfig`, `BookServiceTest` |
| `--coverage` (Istanbul) | JaCoCo (`jacoco-maven-plugin`) → `target/site/jacoco` | `pom.xml` |
| `coverageThreshold` | `jacoco:check` rule | `coverage.line.minimum` in `pom.xml` |
| `jest.config.js` | `pom.xml` (Surefire/Failsafe) + `junit-platform.properties` | |
| unit vs e2e projects | Surefire (`*Test`, phase `test`) vs Failsafe (`*IT`, phase `integration-test`) | |
| `allure-jest` | `allure-jupiter` | `pom.xml` |
| `allure.step(...)` | `Allure.step("...", () -> ...)` or `@Step` methods | `BookServiceTest`, `BookApiSteps` |

## Playwright API testing → RestAssured

| Playwright (`request`) | RestAssured |
|---|---|
| `request.newContext({ baseURL })` | `new RequestSpecBuilder().setBaseUri(...).setPort(...).build()` |
| `await request.post(url, { data })` | `given(spec).contentType(JSON).body(map).post(url)` |
| `expect(res).toBeOK()` / `res.status()` | `.then().statusCode(200)` |
| `expect(await res.json()).toMatchObject(...)` | `.body("title", equalTo("x"))` (GPath + Hamcrest) |
| `(await res.json()).id` | `.extract().jsonPath().getLong("id")` |
| `res.headers()["location"]` | `.header("Location", containsString(...))` |
| JSON schema via ajv | `.body(matchesJsonSchemaInClasspath("schemas/book.json"))` |
| `test.step(...)` | `@Step` method in a `*Steps` class |
| trace / request attachments | `.filter(new AllureRestAssured())` |

## Cypress / Playwright → Postman

| Cypress / Playwright | Postman |
|---|---|
| `cy.request()` / `request.get()` | a request |
| spec file / `describe` | folder |
| `cypress.env.json` / `.env` | environment (`env/*.postman_environment.json`) |
| `Cypress.env("x")` | `pm.environment.get("x")` / `pm.variables.get("x")` |
| alias / variable shared between tests | `pm.collectionVariables.set("bookId", ...)` |
| `beforeEach` | collection- or folder-level pre-request script |
| shared assertion after each test | collection-level test script (response time check) |
| fixtures / `test.each` with data | data file: `newman run ... -d data/books.csv` + `pm.iterationData` |
| `expect(x).to.eq(y)` (Chai) | `pm.expect(x).to.eql(y)` (Chai too) |
| headless `npx cypress run` | `newman run collection.json -e env.json` |

## Jenkins / Bitbucket → GitHub Actions

| Jenkinsfile | Bitbucket Pipelines | GitHub Actions |
|---|---|---|
| `pipeline { agent ... }` | `image:` | `runs-on: ubuntu-24.04` |
| `stage('x')` | `step:` | a **job** (`jobs.x`), each on a fresh VM |
| `steps { sh '...' }` | `script:` | `steps: - run: ...` |
| sequential stages | steps run in order | jobs run **in parallel** unless `needs:` |
| `parallel { }` | `parallel:` | jobs without `needs` |
| `stash` / `unstash` | `artifacts:` | `actions/upload-artifact` / `download-artifact` |
| `post { always { } }` | `after-script:` | `if: always()` (or `!cancelled()`) |
| `post { failure { } }` | | `if: failure()` |
| `when { branch 'main' }` | `branches: main:` | `if: github.ref == 'refs/heads/main'` |
| `parameters { string(...) }` | custom pipeline variables | `on.workflow_dispatch.inputs` |
| `triggers { cron(...) }` | scheduled pipelines | `on.schedule: - cron: ...` |
| `credentials('id')` | secured repository variables | `${{ secrets.NAME }}` |
| `tools { jdk 'x' }` | Docker image with the JDK | `actions/setup-java` |
| shared library | pipes | composite action (`.github/actions/start-sut`) or reusable workflow |
| `matrix { }` | | `strategy.matrix` |
| `options { disableConcurrentBuilds() }` | | `concurrency:` |
| Allure Jenkins plugin (history kept by Jenkins) | | `allure generate` (Allure 3) + `history.jsonl` from `gh-pages` + GitHub Pages |
| Jenkins build page | | job summary (`$GITHUB_STEP_SUMMARY`) |
| Jenkinsfile in repo | `bitbucket-pipelines.yml` | `.github/workflows/*.yml` |

Security habits on GitHub Actions (also in [ci.yml](../.github/workflows/ci.yml)):
- Pin third-party actions to a **commit SHA** (with a version comment). Dependabot updates them.
- `permissions: contents: read` at the top, and grant `write` only in the job that needs it.
- Never put `${{ inputs.x }}` directly into a `run:` script. Pass it via `env:` (prevents script injection).

## Who writes Allure results here

| Source | Producer | Where |
|---|---|---|
| JUnit | `allure-jupiter` (+ AspectJ agent for `@Step`) | `app/target/allure-results` |
| Postman | `newman-reporter-allure` (labels via `// @allure.label.x:"value"` comments in test scripts) | `postman/allure-results` |
| JMeter | `sla-check.mjs` writes result JSON files directly | `jmeter/results/allure-results` |
| CI | merges all three + `history.jsonl`, `executor.json`, `environment.properties`; categories in `allurerc.mjs` | `report` job |

Allure results are just JSON files (`*-result.json`). Any tool that can write such a file can show up in the report.
Coverage and the JMeter dashboard are not test results. Two custom Allure 3 plugins
([.github/allure/plugins](../.github/allure/plugins/)) add them to the same report as extra parts with their own card.
