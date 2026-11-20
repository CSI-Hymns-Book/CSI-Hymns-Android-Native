#!/usr/bin/env node
// Reads every page image with Google Cloud Vision, matches the text against the stored songs, and writes
// page-index.json (song -> page files) into the images repo.
//
//   GOOGLE_VISION_API_KEY=... node tools/build-page-index.mjs --images "<images repo folder>" [--data <dir>] [--books Hymn,Keerthane] [--min-coverage 0.55]
import { existsSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { buildBookIndex } from './page-index.mjs';
import { DEFAULT_DATA_DIR, config, loadSongs, pageFiles, parseArgs } from './shared.mjs';
import { ocrImage } from './vision.mjs';

const { options } = parseArgs(process.argv.slice(2));
if (!options.images) {
  console.error('Usage: node tools/build-page-index.mjs --images "<images repo folder>" [--data <dir>] [--books Hymn,Keerthane,...]');
  process.exit(1);
}
const dataDir = options.data ?? DEFAULT_DATA_DIR;
const minCoverage = Number(options['min-coverage'] ?? 0.55);
const wanted = options.books ? options.books.split(',').map((name) => name.trim()) : Object.keys(config.books);
const languageHints = config.ocrLanguageHints;

const index = { generatedAt: new Date().toISOString(), books: {} };

for (const songType of wanted) {
  const book = config.books[songType];
  if (!book) { console.error(`Unknown book "${songType}". Known: ${Object.keys(config.books).join(', ')}`); process.exit(1); }
  const folder = join(options.images, book.imageFolder);
  const files = existsSync(folder) ? pageFiles(folder) : [];
  if (files.length === 0) { console.log(`${songType}: no page images in ${book.imageFolder}/, skipped`); continue; }

  console.log(`${songType}: reading ${files.length} page image(s)...`);
  const pages = [];
  for (const file of files) {
    pages.push({ file, text: await ocrImage(join(folder, file), { languageHints }) });
    process.stdout.write(`  ${file}\r`);
  }

  const songs = loadSongs(dataDir, songType);
  const result = buildBookIndex({ songs, pages, minCoverage });
  index.books[songType] = result.songs;

  const matched = Object.keys(result.songs).length;
  console.log(`${songType}: ${matched} of ${songs.length} songs placed on pages (${result.unmatched.length} not found)`);
  result.warnings.slice(0, 15).forEach((warning) => console.log(`  warning: ${warning}`));
  result.unmatched.slice(0, 15).forEach((item) => console.log(`  not found: ${item.id} ${item.title} (${item.reason})`));
  if (result.unmatched.length > 15) console.log(`  ... and ${result.unmatched.length - 15} more not found`);
  if (pages.length < songs.length / 6) console.log('  note: few pages so far, so most songs are expected to be missing until you add the rest.');
}

const target = join(options.images, 'page-index.json');
writeFileSync(target, `${JSON.stringify(index, null, 1)}\n`);
console.log(`\nWrote ${target}`);
