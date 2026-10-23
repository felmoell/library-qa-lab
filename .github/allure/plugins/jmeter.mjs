// Allure 3 plugin: adds the JMeter HTML dashboard to the Allure report.
//
// Copies the dashboard into the plugin's folder (named by its key in allurerc.mjs) and adds a card to the overview
// page. The card counts requests: successful = passed, errors = failed. Status follows the SLA gates that
// jmeter/scripts/sla-check.mjs writes as Allure results (label framework=jmeter), so the limits live in one place.
//
// Options: reportDir (JMeter dashboard with index.html and statistics.json)

import { existsSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { addDirectory, addPlaceholder, placeholderSummary } from "./files.mjs";

export default class JmeterPlugin {
  constructor(options = {}) {
    this.reportDir = options.reportDir;
  }

  get available() {
    return existsSync(join(this.reportDir, "statistics.json"));
  }

  async done(context) {
    if (this.available) {
      await addDirectory(context.reportFiles, this.reportDir);
    } else {
      await addPlaceholder(context.reportFiles, "Load test (JMeter)",
        "No JMeter dashboard in this run: the performance job did not upload one.");
    }
  }

  async info(context, store) {
    if (!this.available) {
      return placeholderSummary("Load test (JMeter)", "JMeter");
    }
    const { Total: total } = JSON.parse(await readFile(join(this.reportDir, "statistics.json"), "utf8"));
    const gates = (await store.allTestResults()).filter((tr) =>
      tr.labels.some((l) => l.name === "framework" && l.value === "jmeter"),
    );
    // Without SLA results (e.g. sla-check did not run) fall back to "any request failed"
    const failed = gates.length > 0 ? gates.some((tr) => tr.status !== "passed") : total.errorCount > 0;
    return {
      name: "Load test (JMeter)",
      plugin: "JMeter",
      status: failed ? "failed" : "passed",
      stats: { total: total.sampleCount, passed: total.sampleCount - total.errorCount, failed: total.errorCount },
      // Throughput is samples per second of test time, so this is how long the load test ran
      duration: Math.round((total.sampleCount / total.throughput) * 1000),
    };
  }
}
