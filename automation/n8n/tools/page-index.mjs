// Works out which page file(s) each song is on, from OCR text of the book pages and the stored songs.
// Pure functions (no network), so it can be tested offline.
import { diff } from './shared.mjs';

const BUCKET = 250; // words per search bucket
const COMMON_WORD = 60; // words occurring this often in the book are too common to point at a place
const MIN_PAGE_WORDS = 3;

export function buildBookIndex({ songs, pages, minCoverage = 0.55 }) {
  const stream = [];
  pages.forEach((page, pageIndex) => {
    diff.wordsOf(page.text).forEach((word) => stream.push({ key: word.key, pageIndex }));
  });

  const positions = new Map();
  stream.forEach((word, at) => {
    if (!positions.has(word.key)) positions.set(word.key, []);
    positions.get(word.key).push(at);
  });

  const found = {};
  const unmatched = [];
  const windows = [];

  songs.forEach((song) => {
    const words = diff.wordsOf(song.text);
    if (words.length < 3 || stream.length === 0) { unmatched.push({ id: song.id, title: song.title, reason: 'too short or no pages' }); return; }

    const votes = new Map();
    new Set(words.map((w) => w.key)).forEach((key) => {
      const where = positions.get(key) ?? [];
      if (where.length > COMMON_WORD) return;
      where.forEach((at) => votes.set(Math.floor(at / BUCKET), (votes.get(Math.floor(at / BUCKET)) ?? 0) + 1));
    });
    const buckets = [...votes.entries()].filter(([, count]) => count >= 3).sort((a, b) => b[1] - a[1]).slice(0, 3);

    let best = null;
    buckets.forEach(([bucket]) => {
      const from = Math.max(0, bucket * BUCKET - words.length - 20);
      const to = Math.min(stream.length, (bucket + 1) * BUCKET + words.length + 20);
      const hit = diff.locateWindow(words, stream.slice(from, to));
      if (hit && (!best || hit.matches > best.matches || (hit.matches === best.matches && hit.edits < best.edits))) {
        best = { ...hit, start: hit.start + from, end: hit.end + from, matchedAt: hit.matchedAt.map((at) => at + from) };
      }
    });

    const coverage = best ? best.matches / words.length : 0;
    if (!best || coverage < minCoverage) {
      unmatched.push({ id: song.id, title: song.title, reason: `best match covers only ${(coverage * 100).toFixed(0)}% of the words` });
      return;
    }

    const perPage = new Map();
    best.matchedAt.forEach((at) => perPage.set(stream[at].pageIndex, (perPage.get(stream[at].pageIndex) ?? 0) + 1));
    const needed = Math.max(MIN_PAGE_WORDS, Math.ceil(words.length * 0.08));
    let pageIndexes = [...perPage.entries()].filter(([, count]) => count >= needed).map(([index]) => index).sort((a, b) => a - b);
    if (pageIndexes.length === 0) pageIndexes = [stream[best.start].pageIndex];

    found[song.id] = { pages: pageIndexes.map((index) => pages[index].file), coverage: Number(coverage.toFixed(3)) };
    windows.push({ id: song.id, start: best.start, end: best.end });
  });

  const warnings = [];
  const covered = new Set(Object.values(found).flatMap((entry) => entry.pages));
  pages.forEach((page) => { if (!covered.has(page.file)) warnings.push(`${page.file}: no song was matched on this page (title page, or unreadable?)`); });

  const ordered = [...windows].sort((a, b) => a.start - b.start);
  ordered.forEach((window, i) => {
    const next = ordered[i + 1];
    if (next && window.end - next.start > Math.min(window.end - window.start, next.end - next.start) * 0.5) {
      warnings.push(`songs ${window.id} and ${next.id} overlap on the pages; they may be duplicates or one is a wrong match`);
    }
  });
  const byId = windows.filter((w) => Number.isFinite(Number(w.id))).sort((a, b) => Number(a.id) - Number(b.id));
  byId.forEach((window, i) => {
    const next = byId[i + 1];
    if (next && next.start < window.start) warnings.push(`song ${next.id} was found before song ${window.id}; check these two`);
  });

  return { songs: found, unmatched, warnings, windows };
}
