// Replaces ONE entry's text inside the vault file by exact text substitution, so the file's own
// formatting is untouched and the PR diff shows only that entry. Never throws: when the patch
// cannot be applied safely it returns patchOk=false and the ticket goes to a human.
const config = $('Config').first().json;
const verdict = $('Decide').first().json;
const file = $input.first().json; // GitHub contents API response for the vault file

function fail(reason) {
  return [{ json: { patchOk: false, reason } }];
}

const book = config.books[verdict.songType];
const raw = Buffer.from(file.content ?? '', 'base64').toString('utf8');
const oldEncoded = JSON.stringify(verdict.entry.text);
const newEncoded = JSON.stringify(verdict.corrected);

if (!file.sha || raw === '') return fail(`Could not read ${book.file} from the vault.`);
if (raw.split(oldEncoded).length - 1 !== 1) {
  return fail(`The current text of ${verdict.songType} ${verdict.songNumber} no longer matches the vault file exactly once.`);
}

const patchedRaw = raw.split(oldEncoded).join(newEncoded);

// Safety net: the patched file must equal the original with just this one text changed.
const select = (data) => (book.collection ? data[book.collection] : data);
const expected = JSON.parse(raw);
const target = select(expected).find((item) => String(item[book.idField]) === verdict.entry.id);
if (!target) return fail('Entry vanished while patching.');
target[book.textField] = verdict.corrected;
if (JSON.stringify(expected) !== JSON.stringify(JSON.parse(patchedRaw))) {
  return fail('The patched file would differ from the original in more than the one entry.');
}

return [{
  json: {
    patchOk: true,
    branch: verdict.branch,
    putUrl: `https://api.github.com/repos/${config.vaultRepo}/contents/${book.file}`,
    putBody: {
      message: `fix: correct ${verdict.songType} ${verdict.songNumber} text (${verdict.issueKey})`,
      content: Buffer.from(patchedRaw, 'utf8').toString('base64'),
      sha: file.sha,
      branch: verdict.branch,
    },
  },
}];
