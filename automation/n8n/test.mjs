// Runs each n8n Code node against mocked n8n globals and the real vault JSON. Usage: node test.mjs
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { generate, loadCode } from './build.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const contentDir = join(here, '../../app/src/main/assets/content');
const AsyncFunction = Object.getPrototypeOf(async function noop() {}).constructor;
const config = new Function(loadCode('00-config.js'))()[0].json;

async function run(file, { nodes = {}, input = [{ json: {} }], binary } = {}) {
  const $ = (name) => ({ isExecuted: name in nodes, first: () => nodes[name][0], all: () => nodes[name] });
  const fn = new AsyncFunction('$', '$input', loadCode(file));
  const context = { helpers: { getBinaryDataBuffer: async () => binary ?? Buffer.from('img') } };
  return (await fn.call(context, $, { first: () => input[0], all: () => input })).map((item) => item.json);
}

const adfIssue = ({ summary, labels, description }) => ({
  json: {
    body: {
      issue: {
        key: 'CSI-12',
        fields: {
          summary,
          labels,
          description: {
            type: 'doc',
            version: 1,
            content: [{ type: 'paragraph', content: [{ type: 'text', text: `*Song Information:*\n* Type: x\n\n*Issue Description:*\n${description}\n\n*Reported via:* CSI Hymns App Android` }] }],
          },
        },
      },
    },
  },
});

const rawFile = (name) => readFileSync(join(contentDir, name), 'utf8');
const files = Object.fromEntries(['hymns_data.json', 'keerthane_data.json', 'mangalore_hymns_data.json', 'order-of-service_data.json'].map((name) => [name, rawFile(name)]));
const b64 = (value) => Buffer.from(value, 'utf8').toString('base64');

// One real sample per book: [songType, id, expected title field, whitelist-free ticket]
const SAMPLES = [
  ['Hymn', 206],
  ['Keerthane', 12],
  ['M.T. Hymn', 3],
  ['Order of Service Regular', 183],
  ['Order of Service Festival', 1],
];

const tests = [];
const test = (name, fn) => tests.push([name, fn]);

for (const [songType, id] of SAMPLES) {
  test(`parse: "${songType} ${id} Lyrics Issue"`, async () => {
    const [out] = await run('01-parse-ticket.js', {
      nodes: { Webhook: [adfIssue({ summary: `${songType} ${id} Lyrics Issue`, labels: ['lyrics-issue', 'app-reported'], description: 'No description provided' })] },
    });
    assert.deepEqual(out, { issueKey: 'CSI-12', isLyrics: true, songType, songNumber: id, songTitle: '', userDescription: '', hasUserDescription: false });
  });
}

test('parse: keeps the reporter description; ignores audio and unrelated tickets', async () => {
  const [described] = await run('01-parse-ticket.js', {
    nodes: { Webhook: [adfIssue({ summary: 'Keerthane 12 Lyrics Issue', labels: ['lyrics-issue'], description: 'Line 3 is missing a word' })] },
  });
  assert.equal(described.userDescription, 'Line 3 is missing a word');
  assert.equal(described.hasUserDescription, true);
  for (const [summary, labels] of [['Hymn 5 Audio Contribution', ['audio-contribution']], ['Something else', ['lyrics-issue']], ['Psalm 5 Lyrics Issue', ['lyrics-issue']]]) {
    const [out] = await run('01-parse-ticket.js', { nodes: { Webhook: [adfIssue({ summary, labels, description: 'x' })] } });
    assert.equal(out.isLyrics, false, summary);
  }
});

const routeNodes = (songType, id, whitelist = {}, extra = {}, pages = ['page2.jpg']) => ({
  Config: [{ json: config }],
  'Parse ticket': [{ json: { issueKey: 'CSI-12', isLyrics: true, songType, songNumber: id, userDescription: '', hasUserDescription: false, ...extra } }],
  'Get whitelist': [{ json: { data: JSON.stringify(whitelist) } }],
  'Get vault file': [{ json: { data: files[config.books[songType].file] } }],
  'Get M.T. vault file': [{ json: { data: files['mangalore_hymns_data.json'] } }],
  'Get page index': [{ json: { data: JSON.stringify({ books: { [songType]: { [String(id)]: { pages, coverage: 1 } } } }) } }],
});

for (const [songType, id] of SAMPLES) {
  test(`route: ${songType} ${id} is found and sent to the page check`, async () => {
    const [out] = await run('02-route.js', { nodes: routeNodes(songType, id) });
    assert.equal(out.route, 'check');
    assert.equal(out.entry.id, String(id));
    assert.ok(out.entry.text.length > 20);
    assert.deepEqual(out.pages, [{ file: 'page2.jpg', url: `${config.imageBaseUrl}/${config.books[songType].imageFolder}/page2.jpg` }]);
  });
}

test('route: whitelist rules, missing entries and songs without a page photo', async () => {
  const [blank] = await run('02-route.js', { nodes: routeNodes('Hymn', 206, { Hymn: { 206: { verifiedAt: '2026-11-01' } } }, {}, []) });
  assert.equal(blank.route, 'whitelisted-blank');
  assert.equal(blank.verifiedAt, '2026-11-01');
  const [described] = await run('02-route.js', { nodes: routeNodes('Hymn', 206, { Hymn: { 206: {} } }, { hasUserDescription: true, userDescription: 'typo' }) });
  assert.equal(described.route, 'human');
  const [missing] = await run('02-route.js', { nodes: routeNodes('Hymn', 99999) });
  assert.equal(missing.route, 'human');
  assert.match(missing.reason, /not found in hymns_data\.json/);
  const [noPage] = await run('02-route.js', { nodes: routeNodes('Hymn', 206, {}, {}, []) });
  assert.equal(noPage.route, 'human');
  assert.match(noPage.reason, /No page photo is indexed for Hymn 206/);
  const noIndex = routeNodes('Hymn', 206);
  noIndex['Get page index'] = [{ json: { data: '404: Not Found' } }];
  assert.equal((await run('02-route.js', { nodes: noIndex }))[0].route, 'human');
  const noWhitelist = routeNodes('Hymn', 206);
  noWhitelist['Get whitelist'] = [{ json: { data: '404: Not Found' } }];
  assert.equal((await run('02-route.js', { nodes: noWhitelist }))[0].route, 'check');
  const [spaced] = await run('02-route.js', { nodes: routeNodes('Hymn', 206, {}, {}, ['page 2.jpg', 'page3.png']) });
  assert.equal(spaced.pages[0].url.endsWith('/page%202.jpg'), true);
  assert.deepEqual(spaced.pages.map((page) => page.file), ['page 2.jpg', 'page3.png']);
});

const hymns = JSON.parse(files['hymns_data.json']);
const titleOf = (data, number) => data.find((item) => String(item.number) === String(number)).title;

test('parse: reads the song title the app wrote into the ticket', async () => {
  const issue = adfIssue({ summary: 'Hymn 3 Lyrics Issue', labels: ['lyrics-issue'], description: 'No description provided' });
  issue.json.body.issue.fields.description.content[0].content[0].text = '*Song Information:*\n* Type: Hymn\n* Number: 3\n* Title: Shuddha, shuddha, shuddha\n\n*Issue Description:*\nNo description provided\n\n*Reported via:* CSI Hymns App Android';
  const [out] = await run('01-parse-ticket.js', { nodes: { Webhook: [issue] } });
  assert.equal(out.songTitle, 'Shuddha, shuddha, shuddha');
});

test('route: a "Hymn N" ticket whose title is an M.T. hymn is treated as M.T. Hymn N', async () => {
  const mt = JSON.parse(files['mangalore_hymns_data.json']);
  const title = titleOf(mt, 3);
  const [out] = await run('02-route.js', { nodes: routeNodes('Hymn', 3, {}, { songTitle: title }, ['page9.jpg']) });
  assert.equal(out.songType, 'M.T. Hymn');
  assert.equal(out.entry.title, title);
  assert.equal(out.entry.text, mt.find((item) => String(item.number) === '3').kannadaLyrics);
});

test('route: a genuine Hymn ticket keeps its book; mismatched titles go to a person', async () => {
  const csiTitle = titleOf(hymns, 3);
  const [kept] = await run('02-route.js', { nodes: routeNodes('Hymn', 3, {}, { songTitle: csiTitle }) });
  assert.equal(kept.songType, 'Hymn');
  assert.equal(kept.entry.title, csiTitle);
  const [odd] = await run('02-route.js', { nodes: routeNodes('Hymn', 3, {}, { songTitle: 'A completely different title' }) });
  assert.equal(odd.route, 'human');
  assert.match(odd.reason, /may have been renamed/);
  const [keerthane] = await run('02-route.js', { nodes: routeNodes('Keerthane', 12, {}, { songTitle: 'Not the real title' }) });
  assert.equal(keerthane.route, 'human');
  const [spaced] = await run('02-route.js', { nodes: routeNodes('Hymn', 3, {}, { songTitle: `  ${csiTitle.toUpperCase()}  ` }) });
  assert.equal(spaced.songType, 'Hymn');
});

test('list pages: one item per page photo, in reading order', async () => {
  const [route] = await run('02-route.js', { nodes: routeNodes('Hymn', 7, {}, {}, ['page3.jpg', 'page4.jpg']) });
  const items = await run('02b-list-pages.js', { nodes: { Route: [{ json: route }] } });
  assert.deepEqual(items.map((item) => [item.order, item.pageFile]), [[0, 'page3.jpg'], [1, 'page4.jpg']]);
  assert.ok(items[0].imageUrl.endsWith('/hymns/page3.jpg'));
});

test('ocr request: one Vision request per page photo; non-images are reported as missing', async () => {
  const route = { pages: [{ file: 'page3.jpg' }, { file: 'page4.jpg' }, { file: 'page5.jpg' }] };
  const [out] = await run('03-build-ocr-request.js', {
    nodes: { Config: [{ json: config }], Route: [{ json: route }] },
    input: [
      { json: {}, binary: { data: { mimeType: 'image/jpeg' } } },
      { json: {}, binary: { data: { mimeType: 'text/plain' } } },
      { json: {}, binary: { data: { mimeType: 'image/png' } } },
    ],
    binary: Buffer.from('hello'),
  });
  assert.equal(out.body.requests.length, 2);
  assert.equal(out.body.requests[0].image.content, Buffer.from('hello').toString('base64'));
  assert.deepEqual(out.body.requests[0].features, [{ type: 'DOCUMENT_TEXT_DETECTION' }]);
  assert.deepEqual(out.body.requests[0].imageContext.languageHints, ['kn']);
  assert.deepEqual(out.missingPages, ['page4.jpg']);
  assert.deepEqual(out.pageFiles, ['page3.jpg', 'page4.jpg', 'page5.jpg']);
});

const mtHymns = JSON.parse(files['mangalore_hymns_data.json']);
const textOf = (number) => hymns.find((h) => h.number === number).kannadaLyrics;
const realText = textOf(206);
const mtText = mtHymns.find((h) => String(h.number) === '3').kannadaLyrics;
const orderPages = JSON.parse(files['order-of-service_data.json']);
const orderText = orderPages.regular.find((p) => String(p.page_no) === '183').content;
const decideRoute = (text, extra = {}) => ({ issueKey: 'CSI-12', songType: 'Hymn', songNumber: 206, route: 'check', hasUserDescription: false, userDescription: '', entry: { id: '206', title: 'T', text }, ...extra });
const decide = (text, response, extra = {}, build = { missingPages: [], pageFiles: ['page2.jpg'] }) =>
  run('04-decide.js', {
    nodes: { Config: [{ json: config }], Route: [{ json: decideRoute(text, extra) }], 'Build OCR request': [{ json: build }] },
    input: [{ json: response }],
  }).then(([out]) => out);
const ocr = (...pageTexts) => ({ responses: pageTexts.map((text) => ({ fullTextAnnotation: { text } })) });
// What OCR typically changes: layout, stanza numbers, punctuation and spacing — not the words.
const asOcr = (text) => text.replace(/^\s*\d+[.)]\s*/gm, '').replace(/[.,;:!?]/g, '').replace(/\n+/g, ' \n');
// A page photo shows neighbouring songs too.
const onPage = (text, before, after) => `${asOcr(before)}\n${asOcr(text)}\n${asOcr(after)}`;
const words = (text) => [...text.matchAll(/[\p{L}\p{M}‌‍]+/gu)];
const swapWord = (text, index, replacement) => {
  const word = words(text)[index];
  return text.slice(0, word.index) + replacement + text.slice(word.index + word[0].length);
};

test('decide: identical words (ignoring numbers, punctuation, layout, neighbouring songs) are verified', async () => {
  for (const [text, before, after] of [[realText, textOf(205), textOf(207)], [mtText, textOf(10), textOf(11)], [orderText, textOf(1), textOf(2)]]) {
    const out = await decide(text, ocr(onPage(text, before, after)));
    assert.equal(out.verdict, 'verified', out.reason);
    assert.equal(out.confidence, 1);
    assert.deepEqual(out.pageFiles, ['page2.jpg']);
  }
});

test('decide: one differing word becomes a fix that changes only that word', async () => {
  for (const [text, before, after] of [[realText, textOf(205), textOf(207)], [mtText, textOf(10), textOf(11)], [orderText, textOf(1), textOf(2)]]) {
    const changed = swapWord(text, 5, 'ಮಾರ್ಪಡು');
    const out = await decide(text, ocr(onPage(changed, before, after)));
    assert.equal(out.verdict, 'fix', out.reason);
    assert.equal(out.corrected, changed);
    assert.match(out.notes, /ಮಾರ್ಪಡು/);
    assert.equal(out.branch, 'fix/lyrics-hymn-206');
  }
});

test('decide: a song that runs over two page photos is read as one', async () => {
  const joined = onPage(textOf(7), textOf(6), textOf(8)).split(' ');
  const half = Math.floor(joined.length / 2);
  const out = await decide(textOf(7), ocr(joined.slice(0, half).join(' '), joined.slice(half).join(' ')), { songNumber: 7, entry: { id: '7', title: 'T', text: textOf(7) } });
  assert.equal(out.verdict, 'verified', out.reason);
});

test('decide: missing or extra words, many differences go to a human', async () => {
  const missing = (text) => { const word = words(text)[4]; return text.slice(0, word.index) + text.slice(word.index + word[0].length); };
  assert.equal((await decide(realText, ocr(onPage(missing(realText), textOf(205), textOf(207))))).verdict, 'human');
  const inserted = realText.replace(/(\S+\s+\S+\s+\S+\s+)/, '$1ಹೆಚ್ಚುವರಿ ');
  assert.equal((await decide(realText, ocr(onPage(inserted, textOf(205), textOf(207))))).verdict, 'human');
  let many = realText;
  for (let i = 0; i < 8; i += 1) many = swapWord(many, i * 3, `ತಪ್ಪು${i}`);
  const noisy = await decide(realText, ocr(onPage(many, textOf(205), textOf(207))));
  assert.equal(noisy.verdict, 'human');
  assert.match(noisy.reason, /needs a person|Could not find/);
});

test('decide: wrong page, unreadable photo, Vision errors and missing downloads go to a human with a clear reason', async () => {
  const wrongPage = await decide(realText, ocr(onPage(textOf(100), textOf(99), textOf(101))));
  assert.equal(wrongPage.verdict, 'human');
  assert.match(wrongPage.reason, /Could not find this song's text/);
  assert.match((await decide(realText, { responses: [{}] })).reason, /could not read/);
  const rejected = await decide(realText, { error: { message: 'API key not valid' } });
  assert.match(rejected.reason, /API key not valid/);
  const missing = await decide(realText, ocr(asOcr(realText)), {}, { missingPages: ['page4.jpg'], pageFiles: ['page3.jpg', 'page4.jpg'] });
  assert.equal(missing.verdict, 'human');
  assert.match(missing.reason, /page4\.jpg could not be downloaded/);
});

test('decide: real fixes are applied and doubtful differences are listed for a person', async () => {
  const stored = `${realText}\n4. ಕ್ರುಪಾಪೂರ್ಣನು ಯಾವಾಗ್ಯೂ ಹಾಡಿರಿ ಸ್ತೋತ್ರ ಮಾಡಿರಿ ಕರ್ತನ ನಾಮವ ಹೊಗಳಿರಿ`;
  const page = asOcr(stored.replace('ಕ್ರುಪಾಪೂರ್ಣನು', 'ಕೃಪಾಪೂರ್ಣನು').replace('ಯಾವಾಗ್ಯೂ', 'ಯಾವಾಗೂ'));
  const out = await decide(stored, ocr(`ENGLISH 250\n${page}\nC`));
  assert.equal(out.verdict, 'fix', out.reason);
  assert.equal(out.corrected, stored.replace('ಕ್ರುಪಾಪೂರ್ಣನು', 'ಕೃಪಾಪೂರ್ಣನು'));
  assert.match(out.notes, /not applied, check by hand/);
});

test('decide: a match is not auto-closed when the reporter described a problem', async () => {
  const out = await decide(realText, ocr(asOcr(realText)), { hasUserDescription: true, userDescription: 'stanza 2 wrong' });
  assert.equal(out.verdict, 'human');
  assert.match(out.reason, /described a specific problem/);
});

test('decide: branch names are safe for every book', async () => {
  const out = await decide(realText, ocr(asOcr(realText)), { songType: 'Order of Service Regular', songNumber: 183 });
  assert.equal(out.branch, 'fix/lyrics-order-of-service-regular-183');
});

test('whitelist: creates the file when missing and keeps existing entries', async () => {
  const nodes = { Config: [{ json: config }], Decide: [{ json: { songType: 'Order of Service Regular', songNumber: 183, issueKey: 'CSI-12' } }] };
  const [created] = await run('05-whitelist-update.js', { nodes, input: [{ json: { message: 'Not Found' } }] });
  assert.equal(created.putBody.sha, undefined);
  const existing = { Hymn: { 3: { verifiedAt: 'x' } } };
  const [updated] = await run('05-whitelist-update.js', { nodes, input: [{ json: { sha: 'abc', content: b64(JSON.stringify(existing)) } }] });
  const merged = JSON.parse(Buffer.from(updated.putBody.content, 'base64').toString());
  assert.equal(updated.putBody.sha, 'abc');
  assert.ok(merged.Hymn['3'] && merged['Order of Service Regular']['183'].ticket === 'CSI-12');
});

const patchNodes = (songType, id, corrected, entry) => ({
  Config: [{ json: config }],
  Decide: [{ json: { songType, songNumber: id, issueKey: 'CSI-12', corrected, branch: 'fix/test', entry } }],
});

for (const [songType, id] of SAMPLES) {
  test(`patch: ${songType} ${id} changes only that entry, byte-for-byte elsewhere`, async () => {
    const [routed] = await run('02-route.js', { nodes: routeNodes(songType, id) });
    const book = config.books[songType];
    const corrected = `${routed.entry.text.trimEnd()} (fixed)`;
    const text = files[book.file];
    const [out] = await run('06-patch-lyrics.js', {
      nodes: patchNodes(songType, id, corrected, routed.entry),
      input: [{ json: { sha: 'sha1', content: b64(text) } }],
    });
    assert.equal(out.patchOk, true);
    const patched = Buffer.from(out.putBody.content, 'base64').toString('utf8');
    assert.equal(patched.length - text.length, JSON.stringify(corrected).length - JSON.stringify(routed.entry.text).length);
    assert.equal(patched.split(JSON.stringify(routed.entry.text)).length, 1);
    assert.equal(out.putBody.sha, 'sha1');
    assert.equal(out.putBody.branch, 'fix/test');
    assert.ok(out.putUrl.endsWith(`/contents/${book.file}`));
    const before = JSON.parse(text);
    const after = JSON.parse(patched);
    const list = (data) => (book.collection ? data[book.collection] : data);
    const changed = list(after).filter((item, i) => JSON.stringify(item) !== JSON.stringify(list(before)[i]));
    assert.deepEqual(changed.map((item) => String(item[book.idField])), [String(id)]);
  });
}

test('patch: refuses (without throwing) when it cannot apply safely', async () => {
  const entry = { id: '206', title: 'T', text: 'text that is not in the file' };
  const [stale] = await run('06-patch-lyrics.js', {
    nodes: patchNodes('Hymn', 206, 'new', entry),
    input: [{ json: { sha: 's', content: b64(files['hymns_data.json']) } }],
  });
  assert.equal(stale.patchOk, false);
  const [unreadable] = await run('06-patch-lyrics.js', { nodes: patchNodes('Hymn', 206, 'new', entry), input: [{ json: { message: 'Not Found' } }] });
  assert.equal(unreadable.patchOk, false);
});

test('comment: each path produces the right reply, label and transition', async () => {
  const base = { issueKey: 'CSI-12', songType: 'Hymn', songNumber: 206 };
  const cases = [
    [{ ...base, route: 'whitelisted-blank', verifiedAt: '2026-11-01' }, null, null, 'whitelisted-blank', config.jiraDoneTransitionId, /already verified/],
    [{ ...base, route: 'check', verdict: 'verified' }, 'Decide', null, 'verified', config.jiraDoneTransitionId, /correct and up to date/],
    [{ ...base, route: 'check', verdict: 'fix' }, 'Decide', { patchOk: true }, 'fix', config.jiraReviewTransitionId, /pull\/7/],
    [{ ...base, route: 'human', reason: 'Because.' }, null, null, 'human', '', /Because\./],
    [{ ...base, route: 'check', verdict: 'fix' }, 'Decide', { patchOk: true }, 'fix', config.jiraReviewTransitionId, /pull\/3/, { 'Find open PR': 'https://github.com/x/y/pull/3' }],
    [{ ...base, route: 'check', verdict: 'fix' }, 'Decide', { patchOk: false, reason: 'Patch problem.' }, 'human', '', /Patch problem\./],
    [{ ...base, songType: 'Order of Service Festival', songNumber: 1, route: 'human', reason: 'x' }, null, null, 'human', '', /Order of Service \(Festival\) page 1/],
  ];
  for (const [json, decided, patch, kind, transition, pattern, existing] of cases) {
    const nodes = { Config: [{ json: config }], Route: [{ json }] };
    if (decided) nodes.Decide = [{ json }];
    if (patch) nodes['Patch lyrics'] = [{ json: patch }];
    if (existing) nodes['Find open PR'] = [{ json: { html_url: existing['Find open PR'] } }];
    else if (kind === 'fix') nodes['Open PR'] = [{ json: { html_url: 'https://github.com/x/y/pull/7' } }];
    const [out] = await run('07-comment.js', { nodes });
    assert.equal(out.kind, kind);
    assert.equal(out.transitionId, transition);
    assert.match(out.commentBody.body.content[0].content[0].text, pattern);
  }
});

test('push: builds the data message the app reads, or nothing for guests', async () => {
  const nodes = { 'Parse ticket': [{ json: { songType: 'Hymn', songNumber: 206 } }] };
  const [msg] = await run('08-push-payload.js', { nodes, input: [{ json: [{ fcm_token: 'tok' }] }] });
  assert.equal(msg.message.token, 'tok');
  assert.equal(msg.message.data.target_screen, 'tickets');
  assert.deepEqual(await run('08-push-payload.js', { nodes, input: [{ json: [] }] }), []);
});

test('sdk: generated workflow code embeds every code node and no secrets', async () => {
  const sdk = generate();
  for (const name of ['Webhook', 'Config', 'Parse ticket', 'Route', 'Build OCR request', 'Read page (Cloud Vision)', 'Decide', 'Patch lyrics', 'Build comment']) {
    assert.ok(sdk.includes(`name: "${name}"`) || sdk.includes(`name: '${name}'`), name);
  }
  assert.ok(!sdk.match(/ghp_|sk-ant|ATATT/));
});

let failed = 0;
for (const [name, fn] of tests) {
  try {
    await fn();
    console.log(`ok   ${name}`);
  } catch (error) {
    failed += 1;
    console.log(`FAIL ${name}\n     ${error.message}`);
  }
}
console.log(`\n${tests.length - failed}/${tests.length} passed`);
process.exit(failed ? 1 : 0);
