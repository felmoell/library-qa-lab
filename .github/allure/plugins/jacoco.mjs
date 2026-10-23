// Allure 3 plugin: adds the JaCoCo coverage report to the Allure report.
//
// Copies JaCoCo's HTML report into the plugin's folder (named by its key in allurerc.mjs) and adds a card to the
// overview page. The card counts lines: covered = passed, missed = failed, so its ring shows the line coverage.
// Status is "failed" below minLineCoverage, the same gate as coverage.line.minimum in app/pom.xml.
//
// Options: reportDir (JaCoCo output with index.html and jacoco.xml), minLineCoverage (0..1, default 0.8)

import { existsSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { addDirectory, addPlaceholder, placeholderSummary } from "./files.mjs";

export default class JacocoPlugin {
  constructor(options = {}) {
    this.reportDir = options.reportDir;
    this.minLineCoverage = options.minLineCoverage ?? 0.8;
  }

  get available() {
    return existsSync(join(this.reportDir, "jacoco.xml"));
  }

  async done(context) {
    if (this.available) {
      await addDirectory(context.reportFiles, this.reportDir);
    } else {
      await addPlaceholder(context.reportFiles, "Line coverage",
        "No JaCoCo report in this run: the build job did not upload one.");
    }
  }

  async info() {
    if (!this.available) {
      return placeholderSummary(`Line coverage (min ${this.minLineCoverage * 100} %)`, "JaCoCo");
    }
    const xml = await readFile(join(this.reportDir, "jacoco.xml"), "utf8");
    // The report-wide counters come after the last </package>
    const totals = xml.slice(xml.lastIndexOf("</package>"));
    const [, missed, covered] = totals.match(/<counter type="LINE" missed="(\d+)" covered="(\d+)"\/>/).map(Number);
    // One session per test JVM (Surefire, Failsafe), with the time it started and dumped its coverage data
    const sessions = [...xml.matchAll(/<sessioninfo [^>]*start="(\d+)" dump="(\d+)"/g)].map(([, start, dump]) => ({
      start: Number(start),
      dump: Number(dump),
    }));
    return {
      name: `Line coverage (min ${this.minLineCoverage * 100} %)`,
      plugin: "JaCoCo",
      status: covered / (covered + missed) >= this.minLineCoverage ? "passed" : "failed",
      stats: { total: covered + missed, passed: covered, failed: missed },
      duration: sessions.reduce((sum, s) => sum + s.dump - s.start, 0),
      createdAt: Math.max(...sessions.map((s) => s.dump)),
    };
  }
}
