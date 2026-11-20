// Helpers shared by the page-index and OCR-check tools.
import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCode } from '../build.mjs';

const here = dirname(fileURLToPath(import.meta.url));

export const DEFAULT_DATA_DIR = resolve(here, '../../../app/src/main/assets/content');

// Same word-matching code the workflow runs (lib/diff-lib.js).
export const diff = new Function(`${readFileSync(join(here, '../lib/diff-lib.js'), 'utf8')}; return { wordsOf, hunksOf, locateWindow, compareSong, pageTextOf };`)();

// Title matching (lib/title-lib.js), the same code the workflow's Route node runs.
export const titles = new Function(`${readFileSync(join(here, '../lib/title-lib.js'), 'utf8')}; return { normalizeTitle, sameTitle };`)();

// The book table from the workflow's Config node (file, collection, idField, textField, imageFolder).
export const config = new Function(loadCode('00-config.js'))()[0].json;

export function loadSongs(dataDir, songType) {
  const book = config.books[songType];
  const data = JSON.parse(readFileSync(join(dataDir, book.file), 'utf8'));
  const list = book.collection ? data[book.collection] : data;
  return list
    .filter((item) => typeof item[book.textField] === 'string' && item[book.textField].trim() !== '')
    .map((item) => ({ id: String(item[book.idField]), title: item.title ?? '', text: item[book.textField] }));
}

export const IMAGE_FILE = /\.(jpe?g|png)$/i;

// page2.jpg before page10.jpg; files without a number sort last.
export function pageFiles(folder) {
  const numberOf = (name) => Number(name.match(/(\d+)/)?.[1] ?? Number.MAX_SAFE_INTEGER);
  return readdirSync(folder)
    .filter((name) => IMAGE_FILE.test(name))
    .sort((a, b) => numberOf(a) - numberOf(b) || a.localeCompare(b));
}

export function parseArgs(argv) {
  const positional = [];
  const options = {};
  for (let i = 0; i < argv.length; i += 1) {
    if (argv[i].startsWith('--')) { options[argv[i].slice(2)] = argv[i + 1]; i += 1; } else positional.push(argv[i]);
  }
  return { positional, options };
}
