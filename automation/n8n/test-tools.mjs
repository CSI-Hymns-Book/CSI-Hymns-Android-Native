// Offline tests for the page-index matcher, using real stored songs laid out as noisy fake book pages.
// Usage: node test-tools.mjs
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { buildBookIndex } from './tools/page-index.mjs';
import { DEFAULT_DATA_DIR, diff, loadSongs } from './tools/shared.mjs';

// Small seeded random generator so the "OCR noise" is the same on every run.
function random(seed) {
  let state = seed;
  return () => { state = (state * 1664525 + 1013904223) % 4294967296; return state / 4294967296; };
}

// Lays songs out one after another and cuts the text into pages of ~`wordsPerPage` words, mid-song like a real book.
// Applies OCR-like noise: punctuation and stanza numbers vanish (the matcher ignores them anyway), a few words are
// misread, a few dropped, and a few stray words from the back of the page appear.
function makeBook(songs, { wordsPerPage = 230, misread = 0.03, dropped = 0.01, strays = 0.01, seed = 7 } = {}) {
  const next = random(seed);
  const all = [];
  songs.forEach((song) => diff.wordsOf(song.text).forEach((word) => all.push({ text: word.text, id: song.id })));
  const pages = [];
  const truth = new Map(); // song id -> Set of page numbers (1-based)
  for (let from = 0; from < all.length; from += wordsPerPage) {
    const chunk = all.slice(from, from + wordsPerPage);
    const file = `page${pages.length + 1}.jpg`;
    const out = [];
    chunk.forEach((word) => {
      const roll = next();
      if (!truth.has(word.id)) truth.set(word.id, new Map());
      truth.get(word.id).set(file, (truth.get(word.id).get(file) ?? 0) + 1);
      if (roll < dropped) return;
      out.push(roll < dropped + misread ? `${word.text}ಕ` : word.text);
      if (next() < strays) out.push(all[Math.floor(next() * all.length)].text);
    });
    pages.push({ file, text: out.join(' ') });
  }
  return { pages, truth };
}

const tests = [];
const test = (name, fn) => tests.push([name, fn]);
const hymns = loadSongs(DEFAULT_DATA_DIR, 'Hymn').sort((a, b) => Number(a.id) - Number(b.id)); // the book is in number order

const expectedPages = (truth, song, wordCount) => {
  const needed = Math.max(3, Math.ceil(wordCount * 0.08));
  return [...truth.get(song.id).entries()].filter(([, count]) => count >= needed).map(([file]) => file).sort();
};

test('a whole book section is placed correctly, including songs that span pages', async () => {
  const section = hymns.slice(0, 90);
  const { pages, truth } = makeBook(section);
  const result = buildBookIndex({ songs: section, pages });
  let wrong = 0;
  let spanning = 0;
  section.forEach((song) => {
    const found = result.songs[song.id];
    const expected = expectedPages(truth, song, diff.wordsOf(song.text).length);
    if (expected.length > 1) spanning += 1;
    if (!found || JSON.stringify([...found.pages].sort()) !== JSON.stringify(expected)) wrong += 1;
  });
  assert.ok(spanning >= 10, `only ${spanning} songs span pages in this fake book`);
  assert.ok(wrong <= 1, `${wrong} of ${section.length} songs were placed on the wrong page(s)`);
  assert.equal(result.unmatched.length <= 1, true);
});

test('with only the first pages photographed, other songs are not falsely placed', async () => {
  const { pages } = makeBook(hymns.slice(0, 7), { wordsPerPage: 110 });
  const result = buildBookIndex({ songs: hymns, pages: pages.slice(0, 3) });
  const placed = Object.keys(result.songs);
  ['1', '2', '3', '4', '5', '6'].forEach((id) => assert.ok(result.songs[id], `hymn ${id} should be placed`));
  const falsePositives = placed.filter((id) => Number(id) > 7);
  assert.deepEqual(falsePositives, [], `falsely placed: ${falsePositives.join(', ')}`);
});

test('a song that runs past the last photographed page is reported, not guessed', async () => {
  const { pages } = makeBook(hymns.slice(0, 7), { wordsPerPage: 110 });
  const result = buildBookIndex({ songs: hymns.slice(0, 7), pages: pages.slice(0, 3) });
  assert.ok(result.songs['1']);
  assert.ok(result.unmatched.some((item) => item.id === '7') || result.songs['7'], 'hymn 7 is either unmatched or placed');
});

test('works for a book of a different shape (Order of Service pages)', async () => {
  const songs = loadSongs(DEFAULT_DATA_DIR, 'Order of Service Regular');
  const { pages, truth } = makeBook(songs, { wordsPerPage: 400 });
  const result = buildBookIndex({ songs, pages });
  songs.forEach((song) => {
    const expected = expectedPages(truth, song, diff.wordsOf(song.text).length);
    assert.deepEqual([...(result.songs[song.id]?.pages ?? [])].sort(), expected, `page ${song.id}`);
  });
});

test('noise-free pages give full coverage and no warnings about overlaps', async () => {
  const section = hymns.slice(0, 30);
  const { pages } = makeBook(section, { misread: 0, dropped: 0, strays: 0 });
  const result = buildBookIndex({ songs: section, pages });
  section.forEach((song) => assert.ok(result.songs[song.id].coverage >= 0.98, `hymn ${song.id}`));
  assert.deepEqual(result.warnings.filter((warning) => warning.includes('overlap') || warning.includes('before song')), []);
});

test('unreadable pages and empty input do not crash', async () => {
  assert.deepEqual(Object.keys(buildBookIndex({ songs: hymns.slice(0, 5), pages: [{ file: 'page1.jpg', text: '' }] }).songs), []);
  assert.deepEqual(Object.keys(buildBookIndex({ songs: [], pages: [{ file: 'page1.jpg', text: 'ಅ ಆ ಇ' }] }).songs), []);
});

test('locateWindow finds a song in the middle of other songs and counts only exact words', async () => {
  const [a, b, c] = hymns.slice(10, 13).map((song) => diff.wordsOf(song.text));
  const stream = [...a, ...b, ...c];
  const hit = diff.locateWindow(b, stream);
  assert.equal(hit.start, a.length);
  assert.equal(hit.end, a.length + b.length);
  assert.equal(hit.edits, 0);
  assert.equal(hit.matches, b.length);
});

// ---- letter-level comparison (diff.compareSong) ----
const limits = { maxHunks: 6, maxHunkChars: 12 };
const hymn = (id) => hymns.find((song) => song.id === String(id)).text;
const noisy = (text) => `L.M. 250\n${text.replace(/^\s*\d+\.\s*/gm, '').replace(/[.,;:!?]/g, '').replace(/ +/g, ' ')}\nC 9 KANNADA HYMNS`;

test('compare: spacing, punctuation, stanza numbers, digits and Latin noise are not differences', async () => {
  const stored = hymn(206);
  const joined = noisy(stored).replace(/(\p{L}) (\p{L})/gu, (all, a, b, offset) => (offset % 3 === 0 ? `${a}${b}` : all));
  const result = diff.compareSong(stored, joined, limits);
  assert.equal(result.found, true);
  assert.equal(result.hunks.length, 0, result.notes);
  assert.equal(result.similarity, 1);
});

test('compare: a real letter typo is found and patched without touching anything else', async () => {
  const stored = `${hymn(206)}\n4. ಕ್ರುಪಾಪೂರ್ಣನು ಹಾಡಿರಿ ಸ್ತೋತ್ರ ಮಾಡಿರಿ ಕರ್ತನ ನಾಮವ ಹೊಗಳಿರಿ`;
  const page = noisy(stored.replace('ಕ್ರುಪಾಪೂರ್ಣನು', 'ಕೃಪಾಪೂರ್ಣನು'));
  const result = diff.compareSong(stored, page, limits);
  assert.equal(result.fixable, true);
  assert.equal(result.applied.length, 1);
  assert.equal(result.corrected, stored.replace('ಕ್ರುಪಾಪೂರ್ಣನು', 'ಕೃಪಾಪೂರ್ಣನು'));
  assert.match(result.notes, /ಕ್ರುಪಾಪೂರ್ಣನು" -> "ಕೃಪಾಪೂರ್ಣನು/);
});

test('compare: an OCR-dropped sign is reported but never applied; real fixes in the same song still are', async () => {
  const stored = `${hymn(206)}\n4. ಕ್ರುಪಾಪೂರ್ಣನು ಯಾವಾಗ್ಯೂ ಹಾಡಿರಿ ಸ್ತೋತ್ರ ಮಾಡಿರಿ ಕರ್ತನ ನಾಮವ ಹೊಗಳಿರಿ`;
  const page = noisy(stored.replace('ಕ್ರುಪಾಪೂರ್ಣನು', 'ಕೃಪಾಪೂರ್ಣನು').replace('ಯಾವಾಗ್ಯೂ', 'ಯಾವಾಗೂ'));
  const result = diff.compareSong(stored, page, limits);
  assert.equal(result.hunks.length, 2);
  assert.equal(result.applied.length, 1);
  assert.equal(result.skipped.length, 1);
  assert.equal(result.corrected, stored.replace('ಕ್ರುಪಾಪೂರ್ಣನು', 'ಕೃಪಾಪೂರ್ಣನು'));
  assert.match(result.notes, /not applied, check by hand/);
});

test('compare: an extra word near the very start does not turn into a bogus substitution', async () => {
  const stored = hymn(206);
  const page = noisy(stored.replace(/^(\s*1\.\s*\S+\s+\S+\s+)/, '$1ಹೆಚ್ಚುವರಿ '));
  const result = diff.compareSong(stored, page, limits);
  assert.equal(result.fixable, false, result.notes);
  assert.equal(result.corrected, null);
});

test('compare: a song that is not on the page is not found', async () => {
  const result = diff.compareSong(hymn(206), noisy(hymn(100)), limits);
  assert.equal(result.found && result.coverage >= 0.6, false);
});

test('compare: too many differences are left for a person', async () => {
  const words = [...hymn(206).matchAll(/[\p{Script=Kannada}]+/gu)];
  let page = hymn(206);
  words.slice(0, 16).filter((_, i) => i % 2 === 0).reverse().forEach((word, i) => {
    page = page.slice(0, word.index) + `${word[0].slice(0, -1)}ಕ` + page.slice(word.index + word[0].length);
  });
  const result = diff.compareSong(hymn(206), noisy(page), { maxHunks: 3, maxHunkChars: 12 });
  assert.equal(result.fixable, false);
});

test('a real OCR read of a printed page (hymns page 1) finds hymns 1 and 2 despite noise', async () => {
  const ocrText = readFileSync(new URL('./fixtures/hymns-page1-ocr.txt', import.meta.url), 'utf8');
  const index = buildBookIndex({ songs: hymns, pages: [{ file: 'page1.jpg', text: ocrText }], minCoverage: 0.45 });
  assert.deepEqual(Object.keys(index.songs).sort(), ['1', '2']);
  ['1', '2'].forEach((id) => {
    const result = diff.compareSong(hymn(id), ocrText, limits);
    assert.ok(result.coverage >= 0.9 && result.similarity >= 0.9, `hymn ${id}: ${result.coverage} / ${result.similarity}`);
    assert.ok(result.hunks.every((h) => !/^[A-Za-z0-9 .]+$/.test(h.page)), 'noise tokens must not appear as differences');
    assert.ok(result.hunks.length <= 6);
  });
});

// ---- layout-aware reading order (diff.pageTextOf) ----
// Builds a Cloud Vision-style annotation: paragraphs with boxes, listed in the interleaved order Vision uses on
// two-column pages, plus the "plain text" Vision would give in that same scrambled order.
function annotationOf(paragraphs) {
  const make = (item) => {
    const words = item.text.split(/\s+/).filter(Boolean);
    return {
      boundingBox: { vertices: [{ x: item.x0, y: item.y0 }, { x: item.x1, y: item.y0 }, { x: item.x1, y: item.y1 }, { x: item.x0, y: item.y1 }] },
      words: words.map((word, i) => ({ symbols: [{ text: word, property: { detectedBreak: { type: i === words.length - 1 ? 'LINE_BREAK' : 'SPACE' } } }] })),
    };
  };
  return { text: paragraphs.map((item) => item.text).join('\n'), pages: [{ blocks: paragraphs.map((item) => ({ paragraphs: [make(item)] })) }] };
}
const stanzasOf = (text) => text.split(/\n(?=\s*\d+\.)/).map((part) => part.trim()).filter(Boolean);

// A page like the real hymns page 2: header, hymn 3 (4 stanzas) in the left column, then hymn 4 stanza 1 below it
// and hymn 4 stanzas 2-5 in the right column.
function twoColumnPage() {
  const three = stanzasOf(hymn(3));
  const four = stanzasOf(hymn(4));
  const left = [...three, four[0]].map((text, i) => ({ text, x0: 100, x1: 460, y0: 220 + i * 180, y1: 380 + i * 180 }));
  const right = four.slice(1).map((text, i) => ({ text, x0: 520, x1: 880, y0: 200 + i * 210, y1: 380 + i * 210 }));
  const header = { text: 'ದೇವಸ್ತೋತ್ರ', x0: 430, x1: 540, y0: 60, y1: 100 };
  const interleaved = [header];
  for (let i = 0; i < Math.max(left.length, right.length); i += 1) {
    if (left[i]) interleaved.push(left[i]);
    if (right[i]) interleaved.push(right[i]);
  }
  const expected = [header, ...left, ...right].map((item) => item.text.replace(/\s+/g, ' ').trim());
  return { page: annotationOf(interleaved), expected };
}

test('layout: Vision text order on a two-column page is scrambled, the rebuilt order is not', async () => {
  const { page } = twoColumnPage();
  const scrambled = diff.compareSong(hymn(4), page.text, limits);
  const rebuilt = diff.compareSong(hymn(4), diff.pageTextOf(page), limits);
  assert.ok(scrambled.hunks.length > 0, 'the plain text should be broken for a song split across columns');
  assert.equal(rebuilt.hunks.length, 0, rebuilt.notes);
  assert.equal(rebuilt.similarity, 1);
  assert.equal(diff.compareSong(hymn(3), diff.pageTextOf(page), limits).hunks.length, 0);
});

test('layout: reading order is header, whole left column, whole right column', async () => {
  const { page, expected } = twoColumnPage();
  const got = diff.pageTextOf(page).split('\n').map((line) => line.replace(/\s+/g, ' ').trim());
  assert.deepEqual(got, expected);
});

test('layout: single-column pages and pages without layout data keep their order', async () => {
  const lines = stanzasOf(hymn(6)).map((text, i) => ({ text, x0: 150, x1: 800, y0: 100 + i * 200, y1: 280 + i * 200 }));
  const single = diff.pageTextOf(annotationOf([lines[2], lines[0], lines[1], lines[3]]));
  assert.equal(diff.compareSong(hymn(6), single, limits).hunks.length, 0);
  assert.equal(diff.pageTextOf({ text: 'plain text only' }), 'plain text only');
  assert.equal(diff.pageTextOf(undefined), '');
});

let failed = 0;
for (const [name, fn] of tests) {
  try { await fn(); console.log(`ok   ${name}`); } catch (error) { failed += 1; console.log(`FAIL ${name}\n     ${error.message}`); }
}
console.log(`\n${tests.length - failed}/${tests.length} passed`);
process.exit(failed ? 1 : 0);
