import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const targets = ['src/main.tsx', 'src/app.tsx', 'src/router.tsx', 'src/api', 'src/components', 'src/layout', 'src/pages', 'src/styles', 'vite.config.ts', 'index.html'];
const forbidden = ['dangerouslySetInnerHTML', '.innerHTML', 'insertAdjacentHTML'];
const failures = [];

function collectFiles(target) {
  const absolutePath = path.join(root, target);
  if (!fs.existsSync(absolutePath)) {
    return [];
  }
  const stat = fs.statSync(absolutePath);
  if (stat.isFile()) {
    return [absolutePath];
  }
  return fs.readdirSync(absolutePath, { withFileTypes: true }).flatMap((entry) => collectFiles(path.join(target, entry.name)));
}

for (const filePath of targets.flatMap(collectFiles)) {
  const content = fs.readFileSync(filePath, 'utf8');
  for (const token of forbidden) {
    if (content.includes(token)) {
      failures.push({ file: path.relative(root, filePath), token });
    }
  }
}

if (failures.length > 0) {
  console.error(JSON.stringify({ result: 'failed', failures }, null, 2));
  process.exit(1);
}

console.log(JSON.stringify({ result: 'passed', scannedTargets: targets }, null, 2));