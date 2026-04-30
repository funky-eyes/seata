import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const consoleFeRoot = path.resolve(scriptDir, '..');

function findRepoRoot(startDir) {
  let current = startDir;
  while (current !== path.dirname(current)) {
    if (fs.existsSync(path.join(current, 'pom.xml')) && fs.existsSync(path.join(current, 'saga'))) {
      return current;
    }
    current = path.dirname(current);
  }
  throw new Error('Unable to locate repository root for Saga designer copy.');
}

const repoRoot = findRepoRoot(consoleFeRoot);
const sourceDir = path.join(repoRoot, 'saga', 'seata-saga-statemachine-designer', 'dist');
const targetDir = path.join(consoleFeRoot, 'dist', 'saga-statemachine-designer');
const indexHtml = path.join(targetDir, 'index.html');

if (!fs.existsSync(sourceDir)) {
  throw new Error(`Saga designer source directory does not exist: ${sourceDir}`);
}

fs.rmSync(targetDir, { recursive: true, force: true });
fs.mkdirSync(path.dirname(targetDir), { recursive: true });
fs.cpSync(sourceDir, targetDir, { recursive: true });

const designerHtml = path.join(targetDir, 'designer.html');
if (fs.existsSync(indexHtml)) {
  fs.renameSync(indexHtml, designerHtml);
}

console.log(`Copied Saga designer assets to ${path.relative(consoleFeRoot, targetDir)}`);