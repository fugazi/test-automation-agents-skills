import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SKILL_ROOT = path.resolve(__dirname, '..');
const TEMPLATE_DIR = path.join(SKILL_ROOT, 'assets', 'templates');

function usage(exitCode = 0) {
  const text = `
qa_artifacts.mjs - ISTQB QA Artifacts Generator

Usage:
  node scripts/qa_artifacts.mjs list
  node scripts/qa_artifacts.mjs create <artifact> [options]

Options:
  --out <dir>        Output directory (default: current directory)
  --force            Overwrite existing files
  --project <name>   Project name
  --release <id>     Release/version identifier
  --feature <name>   Feature name
  --title <text>     Title (for bug reports)
  --owner <name>     Document owner
  --approvers <text> Approvers list
  --env <text>       Environment details

Artifacts:
  test-plan             ISTQB-aligned test plan document
  test-summary          End-of-cycle test summary report
  test-cases            Test cases CSV with traceability
  test-conditions       Test conditions derived from test basis
  traceability          Requirements-to-tests traceability matrix
  bug-report            Detailed defect report
  bug-log               Defect tracking log CSV
  regression-suite      Regression suite definition
  playwright-spec       Playwright test file scaffold
  exploratory-charter   Session-based exploratory testing charter
  environment-checklist Test environment readiness checklist
  risk-assessment       Quality risk assessment matrix

Examples:
  node scripts/qa_artifacts.mjs create test-plan --project "MyApp" --release "v1.0"
  node scripts/qa_artifacts.mjs create bug-report --title "Login fails on Safari" --out ./bugs
  node scripts/qa_artifacts.mjs create risk-assessment --project "MyApp" --release "v1.0"
`.trim();
  // eslint-disable-next-line no-console
  console.log(text);
  process.exit(exitCode);
}

function parseArgs(argv) {
  const args = { _: [] };
  for (let i = 0; i < argv.length; i += 1) {
    const value = argv[i];
    if (!value.startsWith('--')) {
      args._.push(value);
      continue;
    }
    const key = value.slice(2);
    if (key === 'force') {
      args.force = true;
      continue;
    }
    const next = argv[i + 1];
    if (!next || next.startsWith('--')) {
      throw new Error(`Missing value for --${key}`);
    }
    args[key] = next;
    i += 1;
  }
  return args;
}

function nowISODate() {
  return new Date().toISOString().slice(0, 10);
}

function slugify(value) {
  const raw = String(value ?? '').trim().toLowerCase();
  const slug = raw
    .replaceAll(/[^a-z0-9]+/g, '-')
    .replaceAll(/^-+|-+$/g, '')
    .slice(0, 80);
  return slug || 'item';
}

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true });
}

function renderTemplate(templateText, replacements) {
  let text = templateText;
  for (const [key, value] of Object.entries(replacements)) {
    text = text.replaceAll(`{{${key}}}`, value ?? '');
  }
  return text;
}

function artifactConfig(artifact, opts) {
  const date = nowISODate();
  const project = opts.project ?? 'Project';
  const release = opts.release ?? 'Release';
  const feature = opts.feature ?? 'Feature';
  const title = opts.title ?? 'Bug title';
  const releaseSlug = slugify(release);
  const featureSlug = slugify(feature);
  const titleSlug = slugify(title);

  const common = {
    date,
    project,
    release,
    feature,
    title,
    owner: opts.owner ?? '',
    approvers: opts.approvers ?? '',
    reported_by: opts.reported_by ?? '',
    env: opts.env ?? '',
  };

  switch (artifact) {
    case 'test-plan':
      return {
        template: 'test-plan.md',
        defaultName: `test-plan-${releaseSlug}.md`,
        replacements: common,
      };
    case 'test-summary':
      return {
        template: 'test-summary-report.md',
        defaultName: `test-summary-${releaseSlug}.md`,
        replacements: common,
      };
    case 'test-cases':
      return {
        template: 'test-cases.csv',
        defaultName: `test-cases-${featureSlug}.csv`,
        replacements: common,
      };
    case 'traceability':
      return {
        template: 'traceability-matrix.csv',
        defaultName: `traceability-${featureSlug}.csv`,
        replacements: common,
      };
    case 'bug-report':
      return {
        template: 'bug-report.md',
        defaultName: `bug-${date}-${titleSlug}.md`,
        replacements: common,
      };
    case 'bug-log':
      return {
        template: 'bug-log.csv',
        defaultName: `bug-log-${releaseSlug}.csv`,
        replacements: common,
      };
    case 'regression-suite':
      return {
        template: 'regression-suite.md',
        defaultName: 'regression-suite.md',
        replacements: common,
      };
    case 'playwright-spec':
      return {
        template: 'playwright-spec.ts',
        defaultName: `${featureSlug}.spec.ts`,
        replacements: common,
      };
    case 'exploratory-charter':
      return {
        template: 'exploratory-charter.md',
        defaultName: `exploratory-charter-${featureSlug}.md`,
        replacements: common,
      };
    case 'test-conditions':
      return {
        template: 'test-conditions.md',
        defaultName: `test-conditions-${featureSlug}.md`,
        replacements: common,
      };
    case 'environment-checklist':
      return {
        template: 'test-environment-checklist.md',
        defaultName: `environment-checklist-${releaseSlug}.md`,
        replacements: common,
      };
    case 'risk-assessment':
      return {
        template: 'risk-assessment-matrix.md',
        defaultName: `risk-assessment-${releaseSlug}.md`,
        replacements: common,
      };
    default:
      throw new Error(`Unknown artifact: ${artifact}`);
  }
}

function listArtifacts() {
  // eslint-disable-next-line no-console
  console.log(
    [
      'test-plan             - ISTQB-aligned test plan',
      'test-summary          - End-of-cycle summary report',
      'test-cases            - Test cases CSV',
      'test-conditions       - Test conditions from test basis',
      'traceability          - Requirements traceability matrix',
      'bug-report            - Detailed defect report',
      'bug-log               - Defect tracking log',
      'regression-suite      - Regression suite definition',
      'playwright-spec       - Playwright test scaffold',
      'exploratory-charter   - Exploratory testing charter',
      'environment-checklist - Environment readiness checklist',
      'risk-assessment       - Quality risk assessment matrix',
    ].join('\n'),
  );
}

function createArtifact(artifact, opts) {
  const config = artifactConfig(artifact, opts);
  const templatePath = path.join(TEMPLATE_DIR, config.template);
  const outDir = path.resolve(opts.out ?? process.cwd());
  const outPath = path.join(outDir, config.defaultName);

  if (!fs.existsSync(templatePath)) {
    throw new Error(`Template not found: ${templatePath}`);
  }

  if (fs.existsSync(outPath) && !opts.force) {
    throw new Error(`Refusing to overwrite existing file: ${outPath} (use --force)`);
  }

  ensureDir(outDir);
  const raw = fs.readFileSync(templatePath, 'utf8');
  const rendered = renderTemplate(raw, config.replacements);
  fs.writeFileSync(outPath, rendered, 'utf8');

  // eslint-disable-next-line no-console
  console.log(outPath);
}

function main() {
  const argv = process.argv.slice(2);
  if (argv.length === 0 || argv.includes('--help') || argv.includes('-h')) usage(0);

  const [command, ...rest] = argv;
  const args = parseArgs(rest);

  if (command === 'list') {
    listArtifacts();
    return;
  }

  if (command === 'create') {
    const artifact = args._[0];
    if (!artifact) usage(2);
    createArtifact(artifact, args);
    return;
  }

  usage(2);
}

try {
  main();
} catch (err) {
  // eslint-disable-next-line no-console
  console.error(err?.message ?? err);
  process.exit(1);
}
