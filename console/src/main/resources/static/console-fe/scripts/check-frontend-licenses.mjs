import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = path.resolve(root, '../../../../../..');
const packageLockPath = path.join(root, 'package-lock.json');
const distributionLicensePath = path.join(repoRoot, 'distribution', 'LICENSE-namingserver');

const allowedLicenses = new Set([
  '0BSD',
  'Apache-2.0',
  'BSD-2-Clause',
  'BSD-3-Clause',
  'CC0-1.0',
  'ISC',
  'MIT',
  'Unlicense',
  'Zlib',
]);

const deniedLicensePatterns = [
  /^GPL(?:-|$)/i,
  /^AGPL(?:-|$)/i,
  /^LGPL(?:-|$)/i,
  /^MPL(?:-|$)/i,
  /^EPL(?:-|$)/i,
  /^CDDL(?:-|$)/i,
  /^SSPL(?:-|$)/i,
  /Commons Clause/i,
  /Business Source License/i,
  /\bsource-available\b/i,
  /\bcopyleft\b/i,
  /SEE LICENSE IN/i,
  /\bcustom\b/i,
  /\bunknown\b/i,
  /\bmissing\b/i,
];

const obsoleteFrontendNames = [
  '@alicloud/console-components',
  '@alicloud/console-components-actions',
  '@alicloud/console-components-app-layout',
  '@alicloud/console-components-console-menu',
  '@alifd',
  'dva',
  'jquery',
  'moment',
  'react-router-redux',
  'redux',
  'styled-components',
  'webpack',
];

const managedSectionStart = 'BEGIN MANAGED CONSOLE FRONTEND RUNTIME DEPENDENCIES';
const managedSectionEnd = 'END MANAGED CONSOLE FRONTEND RUNTIME DEPENDENCIES';

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function splitLicenseExpression(expression) {
  return String(expression)
    .replace(/[()]/g, ' ')
    .split(/\s+OR\s+|\s+AND\s+|\s*\/\s*|\s*,\s*/i)
    .map((item) => item.trim())
    .filter(Boolean);
}

function licenseStatus(license) {
  if (!license || typeof license !== 'string') {
    return { ok: false, reason: 'missing' };
  }
  const tokens = splitLicenseExpression(license);
  if (tokens.length === 0) {
    return { ok: false, reason: 'missing' };
  }
  const denied = tokens.filter(
    (token) => deniedLicensePatterns.some((pattern) => pattern.test(token)) || !allowedLicenses.has(token),
  );
  return denied.length === 0 ? { ok: true, reason: 'allowed' } : { ok: false, reason: denied.join(', ') };
}

function installedPackageJson(lockPath) {
  return path.join(root, lockPath, 'package.json');
}

function licenseForPackage(lockPath, metadata) {
  if (metadata.license) {
    return metadata.license;
  }
  const packageJsonPath = installedPackageJson(lockPath);
  if (fs.existsSync(packageJsonPath)) {
    return readJson(packageJsonPath).license;
  }
  return undefined;
}

function packageNameFromLockPath(lockPath) {
  const marker = 'node_modules/';
  const relativePath = lockPath.slice(lockPath.lastIndexOf(marker) + marker.length);
  return relativePath.startsWith('@') ? relativePath.split('/').slice(0, 2).join('/') : relativePath.split('/')[0];
}

function resolveDependency(lock, fromLockPath, dependencyName) {
  let currentPath = fromLockPath;
  while (currentPath.startsWith('node_modules/')) {
    const nestedPath = `${currentPath}/node_modules/${dependencyName}`;
    if (lock.packages[nestedPath]) {
      return nestedPath;
    }
    const parentNodeModulesIndex = currentPath.lastIndexOf('/node_modules/');
    if (parentNodeModulesIndex === -1) {
      break;
    }
    currentPath = currentPath.slice(0, parentNodeModulesIndex);
  }
  const topLevelPath = `node_modules/${dependencyName}`;
  return lock.packages[topLevelPath] ? topLevelPath : null;
}

function packageRecord(lockPath, metadata) {
  const license = licenseForPackage(lockPath, metadata);
  return {
    name: packageNameFromLockPath(lockPath),
    version: metadata.version,
    license: license ?? 'missing',
    lockPath,
  };
}

function runtimeDependencyRecords(lock) {
  const rootDependencies = Object.keys(lock.packages?.['']?.dependencies ?? {});
  const queue = rootDependencies.map((dependencyName) => `node_modules/${dependencyName}`);
  const visited = new Set();
  const missing = [];
  const runtimeDependencies = [];

  while (queue.length > 0) {
    const lockPath = queue.shift();
    if (!lockPath || visited.has(lockPath)) {
      continue;
    }
    const metadata = lock.packages?.[lockPath];
    if (!metadata) {
      missing.push(lockPath);
      continue;
    }
    visited.add(lockPath);
    runtimeDependencies.push(packageRecord(lockPath, metadata));
    const dependencyNames = Object.keys({ ...(metadata.dependencies ?? {}), ...(metadata.optionalDependencies ?? {}) });
    for (const dependencyName of dependencyNames) {
      queue.push(resolveDependency(lock, lockPath, dependencyName));
    }
  }

  runtimeDependencies.sort(comparePackageRecords);
  return { rootDependencies, runtimeDependencies, missing };
}

function distributionDependencyRecords(runtimeDependencies) {
  const byPackageVersion = new Map();
  for (const dependency of runtimeDependencies) {
    const key = `${dependency.name}@${dependency.version} ${dependency.license}`;
    if (!byPackageVersion.has(key)) {
      byPackageVersion.set(key, {
        name: dependency.name,
        version: dependency.version,
        license: dependency.license,
      });
    }
  }
  return [...byPackageVersion.values()].sort(comparePackageRecords);
}

function comparePackageRecords(left, right) {
  return left.name.localeCompare(right.name) || left.version.localeCompare(right.version) || left.license.localeCompare(right.license);
}

function licenseFailures(records) {
  return records
    .map((record) => ({ record, status: licenseStatus(record.license) }))
    .filter(({ status }) => !status.ok)
    .map(({ record, status }) => ({ ...record, reason: status.reason }));
}

function allInstalledDependencyRecords(lock) {
  const records = [];
  for (const [lockPath, metadata] of Object.entries(lock.packages ?? {})) {
    if (!lockPath.startsWith('node_modules/')) {
      continue;
    }
    const packageJsonPath = installedPackageJson(lockPath);
    if (!metadata.license && metadata.optional && !fs.existsSync(packageJsonPath)) {
      continue;
    }
    records.push(packageRecord(lockPath, metadata));
  }
  return records.sort(comparePackageRecords);
}

function noticeCandidates(runtimeDependencies) {
  return runtimeDependencies.flatMap((dependency) => {
    const packageDir = path.join(root, dependency.lockPath);
    const noticeFiles = fs.existsSync(packageDir)
      ? fs.readdirSync(packageDir).filter((fileName) => /^notice(?:$|[._-])/i.test(fileName)).sort()
      : [];
    if (noticeFiles.length === 0) {
      return [];
    }
    return [{ ...dependency, noticeFiles }];
  });
}

function managedSectionRows(distributionDependencies) {
  return distributionDependencies.map((dependency) => `    ${dependency.name} ${dependency.version} ${dependency.license}`);
}

function parseManagedSection(licenseText) {
  const lines = licenseText.split(/\r?\n/);
  const startIndex = lines.findIndex((line) => line.trim() === managedSectionStart);
  const endIndex = lines.findIndex((line) => line.trim() === managedSectionEnd);
  if (startIndex === -1 || endIndex === -1 || endIndex <= startIndex) {
    return { found: false, rows: [] };
  }
  const rows = lines
    .slice(startIndex + 1, endIndex)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('Source:'));
  return { found: true, rows };
}

function obsoleteMatches(text) {
  return obsoleteFrontendNames.filter((name) => {
    if (name.startsWith('@')) {
      return text.includes(name);
    }
    return new RegExp(`(^|[\\s/])${name}(?:$|[\\s/])`).test(text);
  });
}

function verifyDistribution(lock) {
  const { runtimeDependencies, missing } = runtimeDependencyRecords(lock);
  const distributionDependencies = distributionDependencyRecords(runtimeDependencies);
  const expectedRows = managedSectionRows(distributionDependencies).map((line) => line.trim());
  const licenseText = fs.readFileSync(distributionLicensePath, 'utf8');
  const managedSection = parseManagedSection(licenseText);
  const actualRows = managedSection.rows;
  const actualSet = new Set(actualRows);
  const expectedSet = new Set(expectedRows);
  const missingRows = expectedRows.filter((row) => !actualSet.has(row));
  const extraRows = actualRows.filter((row) => !expectedSet.has(row));
  const obsoleteInManagedSection = obsoleteMatches(actualRows.join('\n'));

  return {
    result: managedSection.found && missing.length === 0 && missingRows.length === 0 && extraRows.length === 0 && obsoleteInManagedSection.length === 0 ? 'passed' : 'failed',
    runtimeDependencyCount: runtimeDependencies.length,
    distributionDependencyCount: distributionDependencies.length,
    managedSectionFound: managedSection.found,
    missingLockPaths: missing,
    missingRows,
    extraRows,
    obsoleteInManagedSection,
  };
}

function printJson(payload) {
  console.log(JSON.stringify(payload, null, 2));
}

function runAll(lock) {
  const records = allInstalledDependencyRecords(lock);
  const failures = licenseFailures(records);
  const payload = {
    result: failures.length === 0 ? 'passed' : 'failed',
    checked: records.length,
    failures,
  };
  printJson(payload);
  if (failures.length > 0) {
    process.exit(1);
  }
}

function runRuntime(lock) {
  const { rootDependencies, runtimeDependencies, missing } = runtimeDependencyRecords(lock);
  const distributionDependencies = distributionDependencyRecords(runtimeDependencies);
  const failures = licenseFailures(runtimeDependencies);
  const payload = {
    result: missing.length === 0 && failures.length === 0 ? 'passed' : 'failed',
    rootDependencies,
    runtimeDependencyCount: runtimeDependencies.length,
    distributionDependencyCount: distributionDependencies.length,
    missingLockPaths: missing,
    failures,
    runtimeDependencies,
    distributionDependencies,
  };
  printJson(payload);
  if (payload.result !== 'passed') {
    process.exit(1);
  }
}

function runNotice(lock) {
  const { runtimeDependencies, missing } = runtimeDependencyRecords(lock);
  const candidates = noticeCandidates(runtimeDependencies);
  const payload = {
    result: missing.length === 0 ? 'passed' : 'failed',
    runtimeDependencyCount: runtimeDependencies.length,
    noticeCandidateCount: candidates.length,
    noticeCandidates: candidates,
    missingLockPaths: missing,
  };
  printJson(payload);
  if (payload.result !== 'passed') {
    process.exit(1);
  }
}

function runVerifyDistribution(lock) {
  const payload = verifyDistribution(lock);
  printJson(payload);
  if (payload.result !== 'passed') {
    process.exit(1);
  }
}

const lock = readJson(packageLockPath);
const args = new Set(process.argv.slice(2));

if (args.size === 0 || args.has('--all')) {
  runAll(lock);
} else if (args.has('--runtime')) {
  runRuntime(lock);
} else if (args.has('--notice')) {
  runNotice(lock);
} else if (args.has('--verify-distribution')) {
  runVerifyDistribution(lock);
} else {
  console.error(`Unsupported arguments: ${[...args].join(' ')}`);
  process.exit(2);
}
