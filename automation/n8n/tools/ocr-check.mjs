#!/usr/bin/env node
// Reads ONE page image with Google Cloud Vision and shows how well it matches your stored songs.
//
//   GOOGLE_VISION_API_KEY=... node tools/ocr-check.mjs "<image file>" --book Hymn [--data <dir>] [--fresh true]
import { basename } from 'node:path';
import { buildBookIndex } from './page-index.mjs';
import { DEFAULT_DATA_DIR, config, diff, loadSongs, parseArgs } from './shared.mjs';
import { ocrImage } from './vision.mjs';

const { positional, options } = parseArgs(process.argv.slice(2));
const image = positional[0];
const songType = options.book ?? 'Hymn';
if (!image || !config.books[songType]) {
  console.error(`Usage: node tools/ocr-check.mjs "<image file>" --book <${Object.keys(config.books).join(' | ')}>`);
  process.exit(1);
}

const text = await ocrImage(image, { languageHints: config.ocrLanguageHints, useCache: options.fresh !== 'true' });
console.log('--- text Cloud Vision read from the page ---');
console.log(text.trim());
console.log('--- end ---\n');

const songs = loadSongs(options.data ?? DEFAULT_DATA_DIR, songType);
const found = buildBookIndex({ songs, pages: [{ file: basename(image), text }], minCoverage: 0.45 });
const limits = { maxHunks: config.maxAutoEditHunks, maxHunkChars: config.maxHunkChars };

const ids = Object.keys(found.songs);
if (ids.length === 0) console.log('No stored song matched this page well enough. Is --book right, or is the photo unreadable?');

let exact = 0;
ids.forEach((id) => {
  const song = songs.find((item) => item.id === id);
  const result = diff.compareSong(song.text, text, limits);
  const outcome = result.hunks.length === 0 ? 'auto-close (exact match)'
    : result.fixable ? `draft correction PR (${result.applied.length} fix${result.applied.length === 1 ? '' : 'es'}${result.skipped.length ? `, ${result.skipped.length} left for you` : ''})` : 'human review';
  if (result.hunks.length === 0) exact += 1;
  console.log(`${songType} ${id} "${song.title}": ${Math.round(result.coverage * 100)}% of its letters found, ${Math.round(result.similarity * 1000) / 10}% identical, ${result.hunks.length} difference(s) -> ${outcome}`);
  result.hunks.slice(0, 12).forEach((h) => console.log(`    stored "${h.stored}"  page "${h.page}"${h.substitution ? '' : '  (not auto-fixable)'}`));
});

console.log(`\n${ids.length} song(s) found on this page; ${exact} read as an exact match.`);
