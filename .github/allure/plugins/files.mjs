// Shared by the custom Allure plugins: copy a finished HTML report (JaCoCo, JMeter) into the plugin's folder, or
// write a placeholder page and card when that report is missing.

import { readdir, readFile } from "node:fs/promises";
import { join, relative } from "node:path";

export async function addDirectory(reportFiles, dir) {
  const entries = await readdir(dir, { recursive: true, withFileTypes: true });
  for (const entry of entries.filter((e) => e.isFile())) {
    const file = join(entry.parentPath, entry.name);
    await reportFiles.addFile(relative(dir, file), await readFile(file));
  }
}

// Upstream jobs that failed early upload no report. The part still gets a folder and an (empty) card: Allure only
// builds the overview page with two or more cards, and otherwise moves the test report to the root of the output.
export async function addPlaceholder(reportFiles, title, message) {
  const html = `<!doctype html><meta charset="utf-8"><title>${title}</title>` +
    `<body style="font-family: sans-serif; margin: 2rem"><h1>${title}</h1><p>${message}</p></body>`;
  await reportFiles.addFile("index.html", Buffer.from(html));
}

export function placeholderSummary(name, plugin) {
  return { name, plugin, status: "unknown", stats: { total: 0 }, duration: 0 };
}
