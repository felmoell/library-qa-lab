#!/usr/bin/env node
// Turns JMeter's dashboard statistics into pass/fail SLA gates.
//
//   node scripts/sla-check.mjs --stats report/statistics.json --out results/allure-results \
//        [--p95 500] [--error-pct 1] [--no-fail]
//
// For every sampler (and the "Total" row) it checks:
//   - 95th percentile response time <= --p95 ms
//   - error rate                    <= --error-pct %
// Each check is written as an Allure result file, so the gates show up in the same report
// as the JUnit and Postman tests. Exit code 1 if any gate fails (unless --no-fail).
// With $GITHUB_STEP_SUMMARY set (GitHub Actions), a Markdown table is added to the job summary.

import { createHash, randomUUID } from "node:crypto";
import { appendFileSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { parseArgs } from "node:util";

const { values: args } = parseArgs({
  options: {
    stats: { type: "string", default: "report/statistics.json" },
    out: { type: "string", default: "results/allure-results" },
    p95: { type: "string", default: "500" },
    "error-pct": { type: "string", default: "1" },
    "no-fail": { type: "boolean", default: false },
  },
});

const maxP95 = Number(args.p95);
const maxErrorPct = Number(args["error-pct"]);
const statistics = JSON.parse(readFileSync(args.stats, "utf8"));
mkdirSync(args.out, { recursive: true });

const now = Date.now();
const results = [];

for (const [label, s] of Object.entries(statistics)) {
  results.push(
    gate(label, "p95 response time", s.pct2ResTime, maxP95, "ms", s),
    gate(label, "error rate", s.errorPct, maxErrorPct, "%", s),
  );
}

for (const result of results) {
  writeFileSync(join(args.out, `${result.uuid}-result.json`), JSON.stringify(result, null, 2));
}

const failed = results.filter((r) => r.status === "failed");
printSummary(results);
process.exit(failed.length > 0 && !args["no-fail"] ? 1 : 0);

function gate(label, metric, actual, limit, unit, stats) {
  const passed = actual <= limit;
  const name = `${label}: ${metric} <= ${limit} ${unit}`;
  const fullName = `jmeter.sla.${label}.${metric}`;
  return {
    uuid: randomUUID(),
    historyId: md5(fullName),
    testCaseId: md5(fullName),
    fullName,
    name,
    status: passed ? "passed" : "failed",
    statusDetails: passed ? {} : { message: `${metric} was ${round(actual)} ${unit}, limit is ${limit} ${unit}` },
    stage: "finished",
    start: now,
    stop: now,
    description: `${stats.sampleCount} samples, mean ${round(stats.meanResTime)} ms, ` +
      `p90 ${round(stats.pct1ResTime)} ms, p99 ${round(stats.pct3ResTime)} ms, ` +
      `throughput ${round(stats.throughput)} req/s`,
    labels: [
      { name: "epic", value: "Performance" },
      { name: "feature", value: "Load smoke (JMeter)" },
      { name: "story", value: label },
      { name: "parentSuite", value: "JMeter" },
      { name: "suite", value: "SLA gates" },
      { name: "severity", value: label === "Total" ? "critical" : "normal" },
      { name: "framework", value: "jmeter" },
    ],
    parameters: [
      { name: "actual", value: `${round(actual)} ${unit}` },
      { name: "limit", value: `${limit} ${unit}` },
    ],
    steps: [],
    attachments: [],
  };
}

function printSummary(all) {
  const rows = all.map((r) => `| ${r.status === "passed" ? "✅" : "❌"} | ${r.name} | ${r.parameters[0].value} |`);
  const table = ["| | Gate | Actual |", "|---|---|---|", ...rows].join("\n");
  console.log(table);
  if (process.env.GITHUB_STEP_SUMMARY) {
    appendFileSync(process.env.GITHUB_STEP_SUMMARY, `### JMeter SLA gates\n\n${table}\n\n`);
  }
}

function md5(value) {
  return createHash("md5").update(value).digest("hex");
}

function round(value) {
  return Math.round(value * 100) / 100;
}
