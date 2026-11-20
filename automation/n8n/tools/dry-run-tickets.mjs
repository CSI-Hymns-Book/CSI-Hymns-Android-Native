#!/usr/bin/env node
// Replays existing lyrics tickets through the same logic as the workflow WITHOUT touching Jira or GitHub:
// pick the book (by ticket title), find the song's page photo(s) via page-index.json, read them (cached OCR),
// compare, and say what the workflow would have done.
//
//   node tools/dry-run-tickets.mjs --tickets tickets.json --images "<images repo folder>" [--data <dir>] [--out report.md]
//
// tickets.json: [{ "key": "CHB-1", "type": "Hymn", "num": 3, "title": "...", "desc": "" }, ...]
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { DEFAULT_DATA_DIR, config, diff, loadSongs, parseArgs, titles } from './shared.mjs';
import { ocrImage } from './vision.mjs';

const { options } = parseArgs(process.argv.slice(2));
if (!options.tickets || !options.images) {
  console.error('Usage: node tools/dry-run-tickets.mjs --tickets tickets.json --images "<images repo folder>" [--data <dir>] [--out report.md]');
  process.exit(1);
}
const dataDir = options.data ?? DEFAULT_DATA_DIR;
const tickets = JSON.parse(readFileSync(options.tickets, 'utf8'));
const pageIndex = JSON.parse(readFileSync(join(options.images, 'page-index.json'), 'utf8'));
const limits = { maxHunks: config.maxAutoEditHunks, maxHunkChars: config.maxHunkChars };

const songsByBook = {};
const songsOf = (songType) => (songsByBook[songType] ??= loadSongs(dataDir, songType));
const pageText = new Map();
const readPage = async (songType, file) => {
  const key = `${songType}/${file}`;
  if (!pageText.has(key)) pageText.set(key, await ocrImage(join(options.images, config.books[songType].imageFolder, file), { languageHints: config.ocrLanguageHints }));
  return pageText.get(key);
};

// Same book choice as the Route node: trust the title over the book name.
function chooseBook(ticket) {
  const declared = songsOf(ticket.type).find((song) => song.id === String(ticket.num));
  if (!ticket.title || !declared || titles.sameTitle(declared.title, ticket.title)) return { songType: ticket.type, relabelled: false };
  if (ticket.type === 'Hymn') {
    const mt = songsOf('M.T. Hymn').find((song) => song.id === String(ticket.num));
    if (mt && titles.sameTitle(mt.title, ticket.title)) return { songType: 'M.T. Hymn', relabelled: true };
  }
  return { songType: ticket.type, relabelled: false, titleProblem: true };
}

const results = new Map(); // "book id" -> result, since many tickets repeat the same song
const rows = [];
for (const ticket of tickets) {
  const { songType, relabelled, titleProblem } = chooseBook(ticket);
  const id = String(ticket.num);
  const label = `${songType} ${id}`;
  let outcome;
  if (!songsOf(songType).some((song) => song.id === id) || titleProblem) {
    outcome = { kind: 'human', why: titleProblem ? 'title does not match any book' : 'song not found in the data' };
  } else if (!pageIndex.books?.[songType]?.[id]) {
    outcome = { kind: 'no-photo', why: 'no page photo yet' };
  } else {
    if (!results.has(label)) {
      const pages = pageIndex.books[songType][id].pages;
      const text = (await Promise.all(pages.map((file) => readPage(songType, file)))).join('\n');
      const song = songsOf(songType).find((item) => item.id === id);
      const result = diff.compareSong(song.text, text, limits);
      results.set(label, { pages, song, result });
    }
    const { result } = results.get(label);
    const kind = !result.found || result.coverage < config.minWindowCoverage ? 'human'
      : result.hunks.length === 0 ? 'verified' : result.fixable ? 'fix' : 'human';
    outcome = { kind: ticket.desc && kind === 'verified' ? 'human' : kind, why: '' };
  }
  rows.push({ ticket, label, relabelled, outcome });
}

const tally = (list) => list.reduce((acc, row) => ({ ...acc, [row.outcome.kind]: (acc[row.outcome.kind] ?? 0) + 1 }), {});
const names = { verified: 'would reply "correct" and close', fix: 'would open a draft correction PR', human: 'would flag for a person', 'no-photo': 'no page photo yet (flagged for a person)' };
const lines = [];
lines.push(`# Dry run of ${tickets.length} open lyrics tickets (nothing was sent to Jira or GitHub)`, '');
const counts = tally(rows);
const fixSongs = new Set(rows.filter((row) => row.outcome.kind === 'fix').map((row) => row.label));
Object.keys(names).forEach((kind) => lines.push(`- ${names[kind]}: **${counts[kind] ?? 0}**${kind === 'fix' ? ` (but only ${fixSongs.size} distinct correction PR${fixSongs.size === 1 ? '' : 's'}: one per song, later tickets just link it)` : ''}`));
lines.push(`- tickets re-labelled to M.T. Hymn by their title: **${rows.filter((row) => row.relabelled).length}**`, '');
lines.push('## Songs with a page photo', '');
[...results.entries()].sort((a, b) => a[0].localeCompare(b[0], undefined, { numeric: true })).forEach(([label, { pages, song, result }]) => {
  const count = rows.filter((row) => row.label === label).length;
  lines.push(`### ${label} "${song.title}" (${count} ticket${count === 1 ? '' : 's'}; page photo ${pages.join(', ')})`);
  lines.push(result.found ? `${Math.round(result.coverage * 100)}% of its letters found, ${(result.similarity * 100).toFixed(1)}% identical, ${result.hunks.length} difference(s)` : 'not found on the page');
  result.hunks.forEach((h) => lines.push(`- stored \`${h.stored}\` vs page \`${h.page}\`${h.substitution ? ' (would be corrected)' : ' (left for a person)'}`));
  lines.push(`Tickets: ${rows.filter((row) => row.label === label).map((row) => row.ticket.key).join(', ')}`, '');
});
const noPhoto = rows.filter((row) => row.outcome.kind === 'no-photo');
lines.push('## Tickets waiting for page photos', '', `${noPhoto.length} tickets across ${new Set(noPhoto.map((row) => row.label)).size} songs. Songs with the most tickets:`);
const byLabel = noPhoto.reduce((acc, row) => ({ ...acc, [row.label]: (acc[row.label] ?? 0) + 1 }), {});
Object.entries(byLabel).sort((a, b) => b[1] - a[1]).slice(0, 15).forEach(([label, n]) => lines.push(`- ${label}: ${n} ticket(s)`));

const flagged = rows.filter((row) => row.outcome.kind === 'human' && row.outcome.why);
if (flagged.length > 0) {
  lines.push('', '## Tickets flagged for a person before any photo is checked', '');
  flagged.forEach((row) => lines.push(`- ${row.ticket.key} ${row.label} "${row.ticket.title}": ${row.outcome.why}`));
}

const report = `${lines.join('\n')}\n`;
if (options.out) writeFileSync(options.out, report);
console.log(report);
