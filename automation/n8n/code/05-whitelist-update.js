// Adds the hymn to verified_lyrics.json via the GitHub contents API (PUT body built here).
// Shape: { "Hymn": { "206": { "verifiedAt": "2026-11-19", "ticket": "CSI-12" } }, "Keerthane": { ... } }
const config = $('Config').first().json;
const verdict = $('Decide').first().json;
const existing = $input.first().json;

let whitelist = {};
if (existing.sha && existing.content) {
  try {
    whitelist = JSON.parse(Buffer.from(existing.content, 'base64').toString('utf8'));
  } catch {
    throw new Error('verified_lyrics.json exists but is not valid JSON; fix it before re-running.');
  }
}

const typeKey = verdict.songType;
const next = {
  ...whitelist,
  [typeKey]: {
    ...(whitelist[typeKey] ?? {}),
    [String(verdict.songNumber)]: {
      verifiedAt: new Date().toISOString().slice(0, 10),
      ticket: verdict.issueKey,
    },
  },
};

const content = Buffer.from(`${JSON.stringify(next, null, 2)}\n`, 'utf8').toString('base64');
return [{
  json: {
    putUrl: `https://api.github.com/repos/${config.vaultRepo}/contents/${config.whitelistFile}`,
    putBody: {
      message: `chore: mark ${typeKey} ${verdict.songNumber} as verified (${verdict.issueKey})`,
      content,
      branch: config.vaultBranch,
      ...(existing.sha ? { sha: existing.sha } : {}),
    },
  },
}];
