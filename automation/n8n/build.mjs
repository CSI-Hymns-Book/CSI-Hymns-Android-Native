// Generates lyrics-ticket-triage.sdk.ts (n8n Workflow SDK code) from code/*.js. Usage: node build.mjs
// The SDK file is what gets validated and created in n8n; the Code nodes embed code/*.js verbatim.
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));

export const ADF_HELPER = `const adf = (text) => ({
  body: { type: 'doc', version: 1, content: [{ type: 'paragraph', content: [{ type: 'text', text }] }] },
});`;

export const DIFF_LIB = readFileSync(join(here, 'lib', 'diff-lib.js'), 'utf8').replace(/^\/\/.*\n/gm, '').trim();

export const TITLE_LIB = readFileSync(join(here, 'lib', 'title-lib.js'), 'utf8').replace(/^\/\/.*\n/gm, '').trim();

export function loadCode(file) {
  return readFileSync(join(here, 'code', file), 'utf8')
    .replace('/*ADF*/', () => ADF_HELPER)
    .replace('/*DIFF_LIB*/', () => DIFF_LIB)
    .replace('/*TITLE_LIB*/', () => TITLE_LIB);
}

const q = (value) => JSON.stringify(value);
const cfg = (key) => `$("Config").first().json.${key}`;
const out = [];
const emit = (text) => out.push(text);

const SAMPLE = {
  hook: `[{ body: { issue: { key: 'CSI-12', fields: { summary: 'Hymn 206 Lyrics Issue', labels: ['lyrics-issue', 'app-reported'], description: { type: 'doc', version: 1, content: [] } } } } }]`,
  route: `[{ issueKey: 'CSI-12', songType: 'Hymn', songNumber: 206, route: 'check', pages: [{ file: 'page2.jpg', url: 'https://example.com/hymns/page2.jpg' }], hasUserDescription: false, entry: { id: '206', title: 'Aghora Manakleshavu', text: 'sample' } }]`,
  decided: `[{ issueKey: 'CSI-12', songType: 'Hymn', songNumber: 206, verdict: 'fix', branch: 'fix/lyrics-hymn-206-csi-12', corrected: 'sample', confidence: 0.95, notes: '', entry: { id: '206', title: 'T', text: 'sample' } }]`,
};

function code(variable, name, file, sample) {
  emit(`const ${variable} = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: ${q(name)}, parameters: { mode: 'runOnceForAllItems', jsCode: ${q(loadCode(file))} } },
  output: ${sample},
});`);
}

const AUTH = {
  vision: { type: 'httpHeaderAuth', credential: 'Google Vision API key', generic: true },
  github: { type: 'githubApi', credential: 'GitHub vault token' },
  anthropic: { type: 'anthropicApi', credential: 'Anthropic API key' },
  jira: { type: 'jiraSoftwareCloudApi', credential: 'Jira Cloud' },
  supabase: { type: 'supabaseApi', credential: 'Supabase (service role)' },
  google: { type: 'googleApi', credential: 'Firebase service account' },
};

function http(variable, name, p) {
  const auth = p.auth ? AUTH[p.auth] : null;
  const lines = [
    `method: ${q(p.method ?? 'GET')}`,
    `url: ${p.url}`,
  ];
  if (auth?.generic) {
    lines.push(`authentication: 'genericCredentialType'`, `genericAuthType: ${q(auth.type)}`);
  } else if (auth) {
    lines.push(`authentication: 'predefinedCredentialType'`, `nodeCredentialType: ${q(auth.type)}`);
  }
  if (p.headers) {
    lines.push(`sendHeaders: true`, `headerParameters: { parameters: [${p.headers.map(([n, v]) => `{ name: ${q(n)}, value: ${q(v)} }`).join(', ')}] }`);
  }
  if (p.body) {
    lines.push(`sendBody: true`, `contentType: 'json'`, `specifyBody: 'json'`, `jsonBody: ${p.body}`);
  }
  lines.push(`options: { ${p.timeout ? `timeout: ${p.timeout}, ` : ''}response: { response: { neverError: ${Boolean(p.neverError)}, responseFormat: ${q(p.format ?? 'json')} } } }`);
  const extra = [
    auth ? `credentials: { ${auth.type}: newCredential(${q(auth.credential)}) }` : '',
    p.onError ? `onError: ${q(p.onError)}` : '',
    p.disabled ? `disabled: true` : '',
    p.alwaysOutput ? `alwaysOutputData: true` : '',
  ].filter(Boolean);
  emit(`const ${variable} = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: ${q(name)}, parameters: { ${lines.join(', ')} }${extra.length ? `, ${extra.join(', ')}` : ''} },
  output: ${p.sample ?? '[{}]'},
});`);
}

const filterOptions = `{ caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }`;
function ifNode(variable, name, expression) {
  emit(`const ${variable} = ifElse({
  version: 2.4,
  config: { name: ${q(name)}, parameters: { conditions: { options: ${filterOptions}, conditions: [{ leftValue: expr(${q(`{{ ${expression} }}`)}), operator: { type: 'boolean', operation: 'true', singleValue: true }, rightValue: '' }], combinator: 'and' } } },
});`);
}

function switchNode(variable, name, field, cases, fallback) {
  const rules = cases.map((value) => `{ outputKey: ${q(value)}, renameOutput: true, conditions: { options: ${filterOptions}, conditions: [{ leftValue: expr(${q(`{{ ${field} }}`)}), operator: { type: 'string', operation: 'equals' }, rightValue: ${q(value)} }], combinator: 'and' } }`);
  emit(`const ${variable} = switchCase({
  version: 3.5,
  config: { name: ${q(name)}, parameters: { mode: 'rules', rules: { values: [${rules.join(', ')}] }, options: { fallbackOutput: 'extra', renameFallbackOutput: ${q(fallback)} } } },
});`);
}

const gh = (path) => `expr(${q(`https://api.github.com/repos/{{ ${cfg('vaultRepo')} }}${path}`)})`;
const raw = (file) => `expr(${q(`https://raw.githubusercontent.com/{{ ${cfg('vaultRepo')} }}/{{ ${cfg('vaultBranch')} }}/${file}`)})`;
const expr_raw = (text) => `expr(${q(text)})`;
const jira = (path) => `expr(${q(`{{ ${cfg('jiraBase')} }}${path}`)})`;
const GH_HEADERS = [['Accept', 'application/vnd.github+json']];
const JSON_HEADER = [['Content-Type', 'application/json']];
const decided = `$("Decide").first().json`;
const built = `$("Build comment").first().json`;

export function generate() {
  out.length = 0;
  emit(`import { workflow, node, trigger, newCredential, ifElse, switchCase, expr } from '@n8n/workflow-sdk';`);

  emit(`const hook = trigger({
  type: 'n8n-nodes-base.webhook',
  version: 2.2,
  config: { name: 'Webhook', parameters: { httpMethod: 'POST', path: 'lyrics-ticket', responseMode: 'onReceived' } },
  output: ${SAMPLE.hook},
});`);
  code('config', 'Config', '00-config.js', `[{ vaultRepo: 'Reynold29/csi-hymns-vault', vaultBranch: 'main', jiraBase: 'https://example.atlassian.net' }]`);
  code('parse', 'Parse ticket', '01-parse-ticket.js', `[{ issueKey: 'CSI-12', isLyrics: true, songType: 'Hymn', songNumber: 206, userDescription: '', hasUserDescription: false }]`);
  ifNode('isLyrics', 'Is lyrics ticket', '$json.isLyrics');
  emit(`const ignore = node({ type: 'n8n-nodes-base.noOp', version: 1, config: { name: 'Ignore other tickets' }, output: [{}] });`);

  http('getWhitelist', 'Get whitelist', { url: raw(`{{ ${cfg('whitelistFile')} }}`), format: 'text', neverError: true, sample: `[{ data: '{}' }]` });
  http('getVault', 'Get vault file', {
    url: raw(`{{ ${cfg('books')}[$("Parse ticket").first().json.songType].file }}`),
    format: 'text',
    sample: `[{ data: '[]' }]`,
  });
  http('getPageIndex', 'Get page index', { url: expr_raw(`{{ ${cfg('imageBaseUrl')} }}/{{ ${cfg('pageIndexFile')} }}`), format: 'text', neverError: true, sample: `[{ data: '{}' }]` });
  code('route', 'Route', '02-route.js', SAMPLE.route);
  switchNode('routeSwitch', 'Route switch', '$json.route', ['check', 'whitelisted-blank'], 'human');

  code('listPages', 'List pages', '02b-list-pages.js', `[{ order: 0, pageFile: 'page1.jpg', imageUrl: 'https://example.com/hymns/page1.jpg' }]`);
  http('getImage', 'Get reference image', { url: `expr('{{ $json.imageUrl }}')`, format: 'file', neverError: true });
  code('buildAi', 'Build OCR request', '03-build-ocr-request.js', `[{ body: { requests: [{ image: { content: '' }, features: [{ type: 'DOCUMENT_TEXT_DETECTION' }], imageContext: { languageHints: ['kn'] } }] }, missingPages: [], pageFiles: ['page1.jpg'] }]`);
  http('askModel', 'Read page (Cloud Vision)', {
    method: 'POST',
    url: `'https://vision.googleapis.com/v1/images:annotate'`,
    auth: 'vision',
    body: `expr('{{ JSON.stringify($json.body) }}')`,
    timeout: 60000,
    neverError: true,
    sample: `[{ responses: [{ fullTextAnnotation: { text: 'sample page text' } }] }]`,
  });
  code('decide', 'Decide', '04-decide.js', SAMPLE.decided);
  switchNode('verdictSwitch', 'Verdict switch', '$json.verdict', ['verified', 'fix'], 'human');

  // verified -> whitelist commit
  http('getWl', 'Get whitelist file (API)', {
    url: gh(`/contents/{{ ${cfg('whitelistFile')} }}?ref={{ ${cfg('vaultBranch')} }}`),
    auth: 'github', headers: GH_HEADERS, neverError: true, sample: `[{ sha: 'abc', content: '' }]`,
  });
  code('buildWl', 'Build whitelist commit', '05-whitelist-update.js', `[{ putUrl: 'https://api.github.com/repos/x/y/contents/verified_lyrics.json', putBody: { message: 'm', content: '', branch: 'main' } }]`);
  http('commitWl', 'Commit whitelist', {
    method: 'PUT', url: `expr('{{ $json.putUrl }}')`, auth: 'github', headers: GH_HEADERS, body: `expr('{{ JSON.stringify($json.putBody) }}')`,
  });

  // fix -> is a correction for this song already open? if not: read file, patch, branch, commit, PR
  http('findPr', 'Find open PR', {
    url: gh(`/pulls?state=open&head={{ ${cfg('vaultRepo')}.split("/")[0] }}:{{ ${decided}.branch }}`),
    auth: 'github', headers: GH_HEADERS, neverError: true, alwaysOutput: true, sample: `[{ html_url: 'https://github.com/x/y/pull/3' }]`,
  });
  ifNode('prOpen', 'Fix already open', 'Boolean($json.html_url)');
  http('getFile', 'Get vault file (API)', {
    url: gh(`/contents/{{ ${cfg('books')}[${decided}.songType].file }}?ref={{ ${cfg('vaultBranch')} }}`),
    auth: 'github', headers: GH_HEADERS, sample: `[{ sha: 'abc', content: '' }]`,
  });
  code('patch', 'Patch lyrics', '06-patch-lyrics.js', `[{ patchOk: true, branch: 'fix/lyrics-hymn-206-csi-12', putUrl: 'https://api.github.com/repos/x/y/contents/hymns_data.json', putBody: { message: 'm', content: '', sha: 'abc', branch: 'fix/lyrics-hymn-206-csi-12' } }]`);
  ifNode('patched', 'Patch applied', '$json.patchOk');
  http('getRef', 'Get main ref', {
    url: gh(`/git/ref/heads/{{ ${cfg('vaultBranch')} }}`), auth: 'github', headers: GH_HEADERS, sample: `[{ object: { sha: 'abc123' } }]`,
  });
  http('createBranch', 'Create fix branch', {
    method: 'POST', url: gh('/git/refs'), auth: 'github', headers: GH_HEADERS,
    body: `expr('{{ JSON.stringify({ ref: "refs/heads/" + ${decided}.branch, sha: $json.object.sha }) }}')`,
  });
  http('commitFix', 'Commit fix', {
    method: 'PUT',
    url: `expr('{{ $("Patch lyrics").first().json.putUrl }}')`,
    auth: 'github', headers: GH_HEADERS,
    body: `expr('{{ JSON.stringify($("Patch lyrics").first().json.putBody) }}')`,
  });
  http('openPr', 'Open PR', {
    method: 'POST', url: gh('/pulls'), auth: 'github', headers: GH_HEADERS,
    sample: `[{ html_url: 'https://github.com/x/y/pull/7' }]`,
    body: `expr(${q(`{{ JSON.stringify({ title: "fix: correct " + ${decided}.songType + " " + ${decided}.songNumber + " text (" + ${decided}.issueKey + ")", head: ${decided}.branch, base: ${cfg('vaultBranch')}, draft: true, body: "Proposed by the automatic lyrics check for " + ${decided}.issueKey + ".\\n\\nPage photo(s): " + ${decided}.pageFiles.join(", ") + "\\n\\nSimilarity: " + ${decided}.confidence + "\\n\\nDifferences (stored -> page; anything marked not applied needs checking by hand): " + ${decided}.notes + "\\n\\nOCR can misread Kannada, so compare each change with the printed page before merging." }) }}`)})`,
  });

  // Jira reply
  code('buildComment', 'Build comment', '07-comment.js', `[{ issueKey: 'CSI-12', kind: 'verified', commentBody: { body: { type: 'doc', version: 1, content: [] } }, transitionId: '31', labelBody: { update: { labels: [{ add: 'needs-human-review' }] } } }]`);
  http('postComment', 'Post Jira comment', {
    method: 'POST', url: jira(`/rest/api/3/issue/{{ $json.issueKey }}/comment`), auth: 'jira', headers: JSON_HEADER,
    body: `expr('{{ JSON.stringify($json.commentBody) }}')`,
  });
  ifNode('hasTransition', 'Has transition', `${built}.transitionId !== ""`);
  http('transition', 'Transition issue', {
    method: 'POST', url: jira(`/rest/api/3/issue/{{ ${built}.issueKey }}/transitions`), auth: 'jira', headers: JSON_HEADER,
    body: `expr('{{ JSON.stringify({ transition: { id: ${built}.transitionId } }) }}')`,
  });
  http('flag', 'Flag for human review', {
    method: 'PUT', url: jira(`/rest/api/3/issue/{{ ${built}.issueKey }}`), auth: 'jira', headers: JSON_HEADER,
    body: `expr('{{ JSON.stringify(${built}.labelBody) }}')`,
  });

  // optional push (disabled until Supabase and Firebase credentials exist)
  ifNode('confirmed', 'Lyrics confirmed correct', `["verified", "whitelisted-blank"].includes(${built}.kind)`);
  http('findOwner', 'Find ticket owner', {
    url: `expr(${q(`{{ ${cfg('supabaseUrl')} }}/rest/v1/jira_tickets?ticket_key=eq.{{ ${built}.issueKey }}&select=user_id`)})`,
    auth: 'supabase', neverError: true, disabled: true, sample: `[{ user_id: '00000000-0000-0000-0000-000000000000' }]`,
  });
  http('findToken', 'Find push token', {
    url: `expr(${q(`{{ ${cfg('supabaseUrl')} }}/rest/v1/users?auth_uid=eq.{{ $json.user_id ?? "00000000-0000-0000-0000-000000000000" }}&select=fcm_token`)})`,
    auth: 'supabase', neverError: true, disabled: true, sample: `[{ fcm_token: 'token' }]`,
  });
  emit(`const buildPush = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: 'Build push payload', disabled: true, parameters: { mode: 'runOnceForAllItems', jsCode: ${q(loadCode('08-push-payload.js'))} } },
  output: [{ message: { token: 'token', data: { title: 't', message: 'm', target_screen: 'tickets' } } }],
});`);
  http('sendPush', 'Send push (FCM)', {
    method: 'POST', url: `expr(${q(`https://fcm.googleapis.com/v1/projects/{{ ${cfg('fcmProjectId')} }}/messages:send`)})`,
    auth: 'google', headers: JSON_HEADER, body: `expr('{{ JSON.stringify($json) }}')`, disabled: true,
  });

  emit(`export default workflow('csi-lyrics-ticket-triage', 'CSI Hymns - lyrics ticket triage')
  .add(hook)
  .to(config)
  .to(parse)
  .to(isLyrics
    .onTrue(getWhitelist
      .to(getVault)
      .to(getPageIndex)
      .to(route)
      .to(routeSwitch
        .onCase(0, listPages.to(getImage).to(buildAi).to(askModel).to(decide).to(verdictSwitch
          .onCase(0, getWl.to(buildWl).to(commitWl).to(buildComment))
          .onCase(1, findPr.to(prOpen
            .onTrue(buildComment)
            .onFalse(getFile.to(patch).to(patched
              .onTrue(getRef.to(createBranch).to(commitFix).to(openPr).to(buildComment))
              .onFalse(buildComment)))))
          .onCase(2, buildComment)))
        .onCase(1, buildComment)
        .onCase(2, buildComment)))
    .onFalse(ignore))
  .add(buildComment)
  .to(postComment)
  .to(hasTransition
    .onTrue(transition.to(confirmed.onTrue(findOwner.to(findToken).to(buildPush).to(sendPush))))
    .onFalse(flag));`);
  return `${out.join('\n\n')}\n`;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  writeFileSync(join(here, 'lyrics-ticket-triage.sdk.ts'), generate());
  console.log('wrote lyrics-ticket-triage.sdk.ts');
}
