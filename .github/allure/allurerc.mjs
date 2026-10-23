// Allure Report 3 config for the CI report job (see ci.yml): allure generate --config .github/allure/allurerc.mjs
// A plain object instead of defineConfig() from "allure": npx runs Allure from its cache, so that import would not resolve here.
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
};
