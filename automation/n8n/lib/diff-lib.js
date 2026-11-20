// Shared word matching. Injected into the workflow's Decide node (/*DIFF_LIB*/) and loaded by the page-index
// tools, so both always behave the same. Keep it dependency-free and declarations-only.
const ZERO_WIDTH = String.fromCharCode(0x200c, 0x200d);
const WORD_PATTERN = new RegExp('[\\p{Script=Kannada}' + ZERO_WIDTH + ']+', 'gu'); // Kannada script only
const NUMBER_ONLY = new RegExp('^\\p{N}+$', 'u');
const ZERO_WIDTH_PATTERN = new RegExp('[' + ZERO_WIDTH + ']', 'g');
const keyOf = (word) => word.replace(ZERO_WIDTH_PATTERN, '').normalize('NFC');
const wordsOf = (text) => [...String(text).matchAll(WORD_PATTERN)]
  .filter((m) => !NUMBER_ONLY.test(m[0]))
  .map((m) => ({ text: m[0], key: keyOf(m[0]), start: m.index, end: m.index + m[0].length }));

// Longest-common-subsequence alignment of two word lists -> change hunks ({ del: [...], ins: [...] } indexes).
function hunksOf(stored, page) {
  const n = stored.length;
  const m = page.length;
  const table = Array.from({ length: n + 1 }, () => new Int32Array(m + 1));
  for (let i = n - 1; i >= 0; i -= 1) {
    for (let j = m - 1; j >= 0; j -= 1) {
      table[i][j] = stored[i].key === page[j].key ? table[i + 1][j + 1] + 1 : Math.max(table[i + 1][j], table[i][j + 1]);
    }
  }
  const hunks = [];
  let current = null;
  let i = 0;
  let j = 0;
  const flush = () => { if (current) { hunks.push(current); current = null; } };
  while (i < n || j < m) {
    if (i < n && j < m && stored[i].key === page[j].key) {
      flush(); i += 1; j += 1;
    } else {
      current ??= { del: [], ins: [] };
      if (j >= m || (i < n && table[i + 1][j] >= table[i][j + 1])) { current.del.push(i); i += 1; } else { current.ins.push(j); j += 1; }
    }
  }
  flush();
  return hunks;
}

// Approximate substring match: the stretch of `stream` words that best matches the `stored` words
// (word edit distance, free start and end in the stream). Returns null when either list is empty.
//   { start, end }        stream indexes of the stretch (end exclusive)
//   { matches, edits }    exactly matching words, and edit distance
//   { matchedAt }         stream indexes of the matching words
function locateWindow(stored, stream) {
  const n = stored.length;
  const m = stream.length;
  if (n === 0 || m === 0) return null;
  const table = Array.from({ length: n + 1 }, () => new Int32Array(m + 1));
  for (let i = 1; i <= n; i += 1) {
    table[i][0] = i;
    for (let j = 1; j <= m; j += 1) {
      const diagonal = table[i - 1][j - 1] + (stored[i - 1].key === stream[j - 1].key ? 0 : 1);
      table[i][j] = Math.min(diagonal, table[i - 1][j] + 1, table[i][j - 1] + 1);
    }
  }
  let end = 0;
  for (let j = 1; j <= m; j += 1) if (table[n][j] < table[n][end]) end = j;
  let i = n;
  let j = end;
  const matchedAt = [];
  while (i > 0) {
    if (j > 0 && stored[i - 1].key === stream[j - 1].key && table[i][j] === table[i - 1][j - 1]) {
      matchedAt.push(j - 1); i -= 1; j -= 1;
    } else if (j > 0 && table[i][j] === table[i - 1][j - 1] + 1) {
      i -= 1; j -= 1;
    } else if (table[i][j] === table[i - 1][j] + 1) {
      i -= 1;
    } else {
      j -= 1;
    }
  }
  matchedAt.reverse();
  return { start: j, end, edits: table[n][end], matches: matchedAt.length, matchedAt };
}

// ---- character-level comparison -------------------------------------------------------------------------
// Only Kannada letters and signs are compared: spaces, line breaks, punctuation, digits, stanza numbers and
// Latin letters are ignored, so an OCR word join/split or a stray "C" is not a difference, but a changed
// letter or sign (for example ಕ್ರು versus ಕೃ) is.
const KANNADA_SIGN = new RegExp('\\p{Script=Kannada}', 'u');
const ANY_NUMBER = new RegExp('\\p{N}', 'u');

function compactOf(text) {
  const chars = [];
  const at = [];
  let offset = 0;
  for (const ch of String(text)) {
    if (KANNADA_SIGN.test(ch) && !ANY_NUMBER.test(ch)) { chars.push(ch); at.push(offset); }
    offset += ch.length;
  }
  return { chars, at };
}

// Aligns `stored` letters with `page` letters from end to end (every stored letter is accounted for, so nothing
// can be skipped for free). Returns the edit distance, the number of matching letters, and the places where the two
// differ: { si, sj, pi, pj } = stored letters [si, sj) became page letters [pi, pj).
function alignChars(stored, page) {
  const n = stored.length;
  const m = page.length;
  if (n === 0 || m === 0) return null;
  const table = Array.from({ length: n + 1 }, () => new Uint16Array(m + 1));
  for (let j = 1; j <= m; j += 1) table[0][j] = j;
  for (let i = 1; i <= n; i += 1) {
    table[i][0] = i;
    for (let j = 1; j <= m; j += 1) {
      const diagonal = table[i - 1][j - 1] + (stored[i - 1] === page[j - 1] ? 0 : 1);
      table[i][j] = Math.min(diagonal, table[i - 1][j] + 1, table[i][j - 1] + 1);
    }
  }
  const ops = [];
  let i = n;
  let j = m;
  while (i > 0 || j > 0) {
    if (i === 0) { ops.push('i'); j -= 1; continue; }
    if (j > 0 && stored[i - 1] === page[j - 1] && table[i][j] === table[i - 1][j - 1]) {
      ops.push('m'); i -= 1; j -= 1;
    } else if (j > 0 && table[i][j] === table[i - 1][j - 1] + 1) {
      ops.push('s'); i -= 1; j -= 1;
    } else if (table[i][j] === table[i - 1][j] + 1) {
      ops.push('d'); i -= 1;
    } else {
      ops.push('i'); j -= 1;
    }
  }
  ops.reverse();

  const hunks = [];
  let s = 0;
  let p = 0;
  let current = null;
  let matches = 0;
  ops.forEach((op) => {
    if (op === 'm') {
      if (current) { current.sj = s; current.pj = p; hunks.push(current); current = null; }
      matches += 1; s += 1; p += 1;
      return;
    }
    current ??= { si: s, pi: p, sj: s, pj: p };
    if (op !== 'i') s += 1;
    if (op !== 'd') p += 1;
  });
  if (current) { current.sj = s; current.pj = p; hunks.push(current); }
  return { edits: table[n][m], matches, hunks };
}

const wordAround = (text, from, to) => {
  let a = from;
  while (a > 0 && !/\s/.test(text[a - 1])) a -= 1;
  let b = to;
  while (b < text.length && !/\s/.test(text[b])) b += 1;
  return text.slice(a, b).trim();
};

// Compares the stored song text with the text OCR read from the page(s).
//   found      false when nothing resembling the song is on the page
//   coverage   share of the song's letters found (0..1);  similarity  1 - edits / letters
//   hunks      each place the texts differ, with the surrounding words for display
//   applied    differences that are small letter-for-letter substitutions inside one word; skipped = all the others
//   fixable    true when there is at least one applied difference and no more than maxHunks of them
//   corrected  the stored text with the applied substitutions only (when fixable)
function compareSong(storedText, pageText, { maxHunks = 6, maxHunkChars = 12 } = {}) {
  const empty = { found: false, coverage: 0, similarity: 0, hunks: [], applied: [], skipped: [], fixable: false, corrected: null, notes: '' };
  // 1. Find the song on the page by words (robust to stray text and to words OCR split or joined).
  const storedWords = wordsOf(storedText);
  const pageWords = wordsOf(pageText);
  const located = locateWindow(storedWords, pageWords);
  if (!located || located.end <= located.start || located.matches === 0) return empty;
  const windowFrom = pageWords[located.start].start;
  const windowTo = pageWords[located.end - 1].end;
  const windowText = pageText.slice(windowFrom, windowTo);

  // 2. Compare that stretch with the stored text letter by letter, end to end.
  const stored = compactOf(storedText);
  const page = compactOf(windowText);
  const hit = alignChars(stored.chars, page.chars);
  if (!hit) return empty;

  const offsetOf = (side, text, index) => (index < side.chars.length ? side.at[index] : text.length);
  const endOffsetOf = (side, from, to, startOffset) => (to > from ? side.at[to - 1] + side.chars[to - 1].length : startOffset);

  const hunks = hit.hunks.map((h) => {
    const storedFrom = offsetOf(stored, storedText, h.si);
    const storedTo = endOffsetOf(stored, h.si, h.sj, storedFrom);
    const pageFrom = windowFrom + offsetOf(page, windowText, h.pi);
    const pageTo = pageFrom + (h.pj > h.pi ? page.at[h.pj - 1] + page.chars[h.pj - 1].length - page.at[h.pi] : 0);
    // the stretch must be Kannada letters only (no space, punctuation or zero-width sign in the middle)
    const cleanSlice = storedText.slice(storedFrom, storedTo) === stored.chars.slice(h.si, h.sj).join('');
    return {
      ...h,
      storedFrom,
      storedTo,
      replacement: page.chars.slice(h.pi, h.pj).join(''),
      stored: wordAround(storedText, storedFrom, storedTo),
      page: wordAround(pageText, pageFrom, pageTo),
      // a small letter-for-letter change inside one word
      substitution: h.sj > h.si && h.pj > h.pi && h.sj - h.si <= maxHunkChars && h.pj - h.pi <= maxHunkChars && cleanSlice,
    };
  });

  // Substitutions are applied; insertions and deletions are more likely OCR dropping or adding a sign than a real
  // typo, so they are only reported, for a person to check against the book.
  const applied = hunks.filter((h) => h.substitution);
  const skipped = hunks.filter((h) => !h.substitution);
  const fixable = applied.length > 0 && applied.length <= maxHunks;
  let corrected = null;
  if (fixable) {
    corrected = storedText;
    [...applied].sort((a, b) => b.storedFrom - a.storedFrom).forEach((h) => {
      corrected = corrected.slice(0, h.storedFrom) + h.replacement + corrected.slice(h.storedTo);
    });
    if (corrected === storedText) corrected = null;
  }
  const show = (list) => list.slice(0, 8).map((h) => `"${h.stored}" -> "${h.page}"`).join('; ');
  return {
    found: true,
    coverage: hit.matches / stored.chars.length,
    similarity: Math.max(0, 1 - hit.edits / stored.chars.length),
    hunks,
    applied,
    skipped,
    fixable: fixable && corrected !== null,
    corrected,
    notes: skipped.length > 0 && applied.length > 0
      ? `${show(applied)} | not applied, check by hand: ${show(skipped)}`
      : show(hunks),
  };
}

// ---- reading order from Cloud Vision's layout data ---------------------------------------------------------
// Vision's plain text interleaves the paragraphs of a two-column page (the end of a song in the left column can
// appear after the start of the next song in the right column). The paragraphs carry their position, so rebuild
// the text the way a person reads the page: page headers, then the whole left column, then the whole right column.
function paragraphText(paragraph) {
  return (paragraph.words ?? []).map((word) => {
    const symbols = word.symbols ?? [];
    const text = symbols.map((symbol) => symbol.text ?? '').join('');
    const kind = symbols.length > 0 ? symbols[symbols.length - 1].property?.detectedBreak?.type : undefined;
    return text + (kind === 'LINE_BREAK' || kind === 'EOL_SURE_SPACE' ? '\n' : kind === 'SPACE' || kind === 'SURE_SPACE' ? ' ' : '');
  }).join('');
}

function pageTextOf(annotation) {
  const items = [];
  (annotation?.pages?.[0]?.blocks ?? []).forEach((block) => (block.paragraphs ?? []).forEach((paragraph) => {
    const vertices = paragraph.boundingBox?.vertices ?? [];
    if (vertices.length === 0) return;
    const xs = vertices.map((vertex) => vertex.x ?? 0);
    const ys = vertices.map((vertex) => vertex.y ?? 0);
    items.push({ text: paragraphText(paragraph), x0: Math.min(...xs), x1: Math.max(...xs), y0: Math.min(...ys), y1: Math.max(...ys) });
  }));
  if (items.length < 4) return annotation?.text ?? items.map((item) => item.text).join('\n');

  const width = Math.max(...items.map((item) => item.x1));
  const height = Math.max(...items.map((item) => item.y1));
  const centre = (item) => (item.x0 + item.x1) / 2;
  const average = (values) => values.reduce((sum, value) => sum + value, 0) / values.length;
  const byTop = (a, b) => a.y0 - b.y0 || a.x0 - b.x0;

  // two-means on the horizontal centres: are there two columns?
  let low = Math.min(...items.map(centre));
  let high = Math.max(...items.map(centre));
  for (let round = 0; round < 12; round += 1) {
    const left = items.filter((item) => Math.abs(centre(item) - low) <= Math.abs(centre(item) - high));
    const right = items.filter((item) => !left.includes(item));
    if (left.length === 0 || right.length === 0) break;
    low = average(left.map(centre));
    high = average(right.map(centre));
  }
  const middle = (low + high) / 2;
  const leftCount = items.filter((item) => centre(item) <= middle).length;
  const twoColumns = high - low > width * 0.3 && leftCount >= 2 && items.length - leftCount >= 2;
  if (!twoColumns) return [...items].sort(byTop).map((item) => item.text.trim()).join('\n');

  const isHeader = (item) => item.y0 < height * 0.12 && Math.abs(centre(item) - middle) < width * 0.12;
  const headers = items.filter(isHeader).sort(byTop);
  const rest = items.filter((item) => !isHeader(item));
  const leftColumn = rest.filter((item) => centre(item) <= middle).sort(byTop);
  const rightColumn = rest.filter((item) => centre(item) > middle).sort(byTop);
  return [...headers, ...leftColumn, ...rightColumn].map((item) => item.text.trim()).join('\n');
}
