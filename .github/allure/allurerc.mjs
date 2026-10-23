// Allure Report 3 config for the CI report job (see ci.yml): allure generate --config .github/allure/allurerc.mjs
// A plain object instead of defineConfig() from "allure": npx runs Allure from its cache, so that import
// would not resolve here.

import { fileURLToPath } from "node:url";

// Allure loads plugins with require.resolve(), which needs a file path (not a URL, not relative to this file)
const localPlugin = (file) => fileURLToPath(new URL(`./plugins/${file}`, import.meta.url));

export default {
  name: "Library QA Lab",
  // Trend charts and "new" / "flaky" detection come from this file (one JSON line per run). The report job restores it
  // from the previous report on gh-pages before generating and publishes the updated file next to the report.
  historyPath: "./history.jsonl",
  historyLimit: 20,
  // Failure buckets in the Categories tab. A failed test lands in the first rule that matches.
  // (Allure 2 read these from categories.json in the results folder; Allure 3 only takes them from here.)
  categories: [
    { name: "SLA violations (JMeter)", matchedStatuses: ["failed"], messageRegex: ".*limit is.*" },
    { name: "Product defects (assertion failed)", matchedStatuses: ["failed"] },
    { name: "Test defects (test code crashed)", matchedStatuses: ["broken"] },
  ],
  // One report with three parts. Each plugin writes to its own folder, named by its key (tests/, coverage/, jmeter/),
  // and Allure adds an overview page (index.html) with a card per part. The report job downloads the inputs to extra/.
  plugins: {
    tests: { import: "awesome", options: { reportName: "Tests (JUnit, Postman, JMeter SLA)" } },
    coverage: { import: localPlugin("jacoco.mjs"), options: { reportDir: "extra/coverage", minLineCoverage: 0.8 } },
    jmeter: { import: localPlugin("jmeter.mjs"), options: { reportDir: "extra/jmeter/report" } },
  },
};
