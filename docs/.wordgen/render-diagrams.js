const { execFileSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const diagramsDir = path.join(__dirname, "diagrams");
const outDir = path.join(__dirname, "images");
fs.mkdirSync(outDir, { recursive: true });

const files = fs
  .readdirSync(diagramsDir)
  .filter((f) => f.endsWith(".mmd"))
  .sort();

const mmdcBin = path.join(
  __dirname,
  "node_modules",
  ".bin",
  process.platform === "win32" ? "mmdc.cmd" : "mmdc"
);

for (const file of files) {
  const input = path.join(diagramsDir, file);
  const output = path.join(outDir, file.replace(/\.mmd$/, ".png"));
  console.log("Rendering", file, "->", path.basename(output));
  execFileSync(
    mmdcBin,
    [
      "-i", input,
      "-o", output,
      "-b", "white",
      "-s", "3",
      "-p", path.join(__dirname, "puppeteer-config.json"),
      "-c", path.join(__dirname, "mermaid-config.json"),
    ],
    { stdio: "inherit", shell: process.platform === "win32" }
  );
}

console.log("All diagrams rendered to", outDir);
