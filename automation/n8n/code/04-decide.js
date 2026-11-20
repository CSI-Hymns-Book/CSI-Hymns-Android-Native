// Finds the song inside the text Cloud Vision read from its page photo(s), then compares it letter by letter.
//   verified -> no differing words (and the reporter gave no specific complaint)
//   fix      -> only a few small letter-for-letter differences inside words; the stored text is patched with the page's letters
//   human    -> anything else (song not found on the page, inserted/missing letters, many differences, unreadable photo)
// Spaces, punctuation, digits and Latin letters are ignored; OCR noise is why only exact matches are auto-closed.
const config = $('Config').first().json;
const route = $('Route').first().json;
const build = $('Build OCR request').first().json;
const body = $input.first().json;

/*DIFF_LIB*/

const responses = body.responses ?? [];
const failure = body.error ?? responses.find((item) => item.error)?.error;
const pageText = responses.map((item) => pageTextOf(item.fullTextAnnotation)).join('\n');
const original = route.entry.text ?? '';

let verdict = 'human';
let reason = '';
let corrected = null;
let similarity = 0;
let notes = '';

if (build.missingPages.length > 0) {
  reason = `The page photo(s) ${build.missingPages.join(', ')} could not be downloaded.`;
} else if (failure || pageText.trim() === '') {
  reason = `Cloud Vision could not read the page photo(s)${failure?.message ? `: ${failure.message}` : '.'}`;
} else {
  const result = compareSong(original, pageText, { maxHunks: config.maxAutoEditHunks, maxHunkChars: config.maxHunkChars });
  similarity = result.similarity;
  notes = result.notes;

  if (!result.found || result.coverage < config.minWindowCoverage) {
    reason = `Could not find this song's text on the page photo(s): only ${Math.round(result.coverage * 100)}% of its letters matched. Is the page index right, or is the photo unreadable?`;
  } else if (result.hunks.length === 0) {
    if (route.hasUserDescription) reason = 'The page text matches, but the reporter described a specific problem.';
    else verdict = 'verified';
  } else if (result.fixable) {
    verdict = 'fix';
    corrected = result.corrected;
  } else {
    reason = `The page differs from the stored text in ${result.hunks.length} place(s) in a way that needs a person to check.`;
  }
}

const slug = route.songType.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
// one branch per song: later reports of the same song find the correction that is already open
const branch = `fix/lyrics-${slug}-${route.songNumber}`;

return [{ json: { ...route, verdict, reason, corrected, confidence: Number(similarity.toFixed(3)), notes, branch, pageFiles: build.pageFiles } }];
