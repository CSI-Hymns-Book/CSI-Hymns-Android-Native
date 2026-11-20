import { workflow, node, trigger, newCredential, ifElse, switchCase, expr } from '@n8n/workflow-sdk';

const hook = trigger({
  type: 'n8n-nodes-base.webhook',
  version: 2.2,
  config: { name: 'Webhook', parameters: { httpMethod: 'POST', path: 'lyrics-ticket', responseMode: 'onReceived' } },
  output: [{ body: { issue: { key: 'CSI-12', fields: { summary: 'Hymn 206 Lyrics Issue', labels: ['lyrics-issue', 'app-reported'], description: { type: 'doc', version: 1, content: [] } } } } }],
});

const config = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "Config", parameters: { mode: 'runOnceForAllItems', jsCode: "// Edit these values, then run `node build.mjs` to regenerate the workflow code.\nreturn [{\n  json: {\n    vaultRepo: 'Reynold29/csi-hymns-vault',\n    vaultBranch: 'main',\n    jiraBase: 'https://YOUR-SITE.atlassian.net',\n    // Reference page photos: {imageBaseUrl}/{imageFolder}/{page file named in page-index.json}\n    imageBaseUrl: 'https://YOUR-HOST/lyrics-images',\n    // One entry per book. `songType` is what the app writes in the Jira summary:\n    // \"<songType> <id> Lyrics Issue\".\n    books: {\n      'Hymn': { file: 'hymns_data.json', collection: '', idField: 'number', textField: 'kannadaLyrics', imageFolder: 'hymns' },\n      'Keerthane': { file: 'keerthane_data.json', collection: '', idField: 'number', textField: 'kannadaLyrics', imageFolder: 'keerthanes' },\n      'M.T. Hymn': { file: 'mangalore_hymns_data.json', collection: '', idField: 'number', textField: 'kannadaLyrics', imageFolder: 'mangalore-tunes' },\n      'Order of Service Regular': { file: 'order-of-service_data.json', collection: 'regular', idField: 'page_no', textField: 'content', imageFolder: 'order-of-service/regular' },\n      'Order of Service Festival': { file: 'order-of-service_data.json', collection: 'festival', idField: 'page_no', textField: 'content', imageFolder: 'order-of-service/festival' },\n    },\n    whitelistFile: 'verified_lyrics.json',\n    // Built by tools/build-page-index.mjs: which page photo(s) each song is on.\n    pageIndexFile: 'page-index.json',\n    // The song's text must be found on the page photo(s) with at least this share of its letters.\n    minWindowCoverage: 0.6,\n    // OCR language hints for Google Cloud Vision (kn = Kannada).\n    ocrLanguageHints: ['kn'],\n    // A correction PR is proposed only when at most this many places differ, each a letter-for-letter\n    // substitution of at most this many letters inside one word. Anything else goes to a person.\n    maxAutoEditHunks: 6,\n    maxHunkChars: 12,\n    // Find these ids in Jira: GET /rest/api/3/issue/KEY/transitions\n    jiraDoneTransitionId: '31',\n    jiraReviewTransitionId: '21',\n    humanReviewLabel: 'needs-human-review',\n    supabaseUrl: 'https://YOUR-PROJECT.supabase.co',\n    fcmProjectId: 'YOUR-FIREBASE-PROJECT-ID',\n  },\n}];\n" } },
  output: [{ vaultRepo: 'Reynold29/csi-hymns-vault', vaultBranch: 'main', jiraBase: 'https://example.atlassian.net' }],
});

const parse = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "Parse ticket", parameters: { mode: 'runOnceForAllItems', jsCode: "// Reads the Jira \"issue created\" webhook and extracts what the app put in the ticket.\n// Summary format: \"<Hymn|Keerthane|M.T. Hymn|Order of Service Regular|Order of Service Festival> <id> Lyrics Issue\".\nconst BOOK_TYPES = ['Hymn', 'Keerthane', 'M.T. Hymn', 'Order of Service Regular', 'Order of Service Festival'];\n\nconst body = $('Webhook').first().json.body ?? {};\nconst fields = body.issue?.fields ?? {};\nconst labels = fields.labels ?? [];\nconst summary = fields.summary ?? '';\n\nfunction adfText(node) {\n  if (!node) return '';\n  if (typeof node === 'string') return node;\n  if (node.type === 'text') return node.text ?? '';\n  return (node.content ?? []).map(adfText).join(node.type === 'doc' ? '\\n' : '');\n}\n\nconst match = summary.match(/^(.*?)\\s+(\\d+)\\s+Lyrics Issue$/i);\nconst songType = match ? BOOK_TYPES.find((type) => type.toLowerCase() === match[1].trim().toLowerCase()) ?? '' : '';\nconst isLyrics = labels.includes('lyrics-issue') && !labels.includes('audio-contribution') && songType !== '';\n\nconst text = adfText(fields.description);\nconst titleLine = text.match(/\\*\\s*Title:\\s*(.+)/);\nconst described = text.match(/\\*Issue Description:\\*\\s*([\\s\\S]*?)\\s*\\*Reported via:\\*/);\nconst userDescription = (described?.[1] ?? '').trim();\nconst hasUserDescription = userDescription !== '' && userDescription !== 'No description provided';\n\nreturn [{\n  json: {\n    issueKey: body.issue?.key ?? '',\n    isLyrics,\n    songType,\n    songNumber: match ? Number(match[2]) : 0,\n    songTitle: titleLine ? titleLine[1].trim() : '',\n    userDescription: hasUserDescription ? userDescription : '',\n    hasUserDescription,\n  },\n}];\n" } },
  output: [{ issueKey: 'CSI-12', isLyrics: true, songType: 'Hymn', songNumber: 206, userDescription: '', hasUserDescription: false }],
});

const isLyrics = ifElse({
  version: 2.4,
  config: { name: "Is lyrics ticket", parameters: { conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ $json.isLyrics }}"), operator: { type: 'boolean', operation: 'true', singleValue: true }, rightValue: '' }], combinator: 'and' } } },
});

const ignore = node({ type: 'n8n-nodes-base.noOp', version: 1, config: { name: 'Ignore other tickets' }, output: [{}] });

const getWhitelist = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Get whitelist", parameters: { method: "GET", url: expr("https://raw.githubusercontent.com/{{ $(\"Config\").first().json.vaultRepo }}/{{ $(\"Config\").first().json.vaultBranch }}/{{ $(\"Config\").first().json.whitelistFile }}"), options: { response: { response: { neverError: true, responseFormat: "text" } } } } },
  output: [{ data: '{}' }],
});

const getVault = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Get vault file", parameters: { method: "GET", url: expr("https://raw.githubusercontent.com/{{ $(\"Config\").first().json.vaultRepo }}/{{ $(\"Config\").first().json.vaultBranch }}/{{ $(\"Config\").first().json.books[$(\"Parse ticket\").first().json.songType].file }}"), options: { response: { response: { neverError: false, responseFormat: "text" } } } } },
  output: [{ data: '[]' }],
});

const getPageIndex = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Get page index", parameters: { method: "GET", url: expr("{{ $(\"Config\").first().json.imageBaseUrl }}/{{ $(\"Config\").first().json.pageIndexFile }}"), options: { response: { response: { neverError: true, responseFormat: "text" } } } } },
  output: [{ data: '{}' }],
});

const route = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "Route", parameters: { mode: 'runOnceForAllItems', jsCode: "// Looks the entry up in the book's vault JSON, checks the whitelist, and picks a route:\n//   whitelisted-blank -> reply \"already verified\" and close\n//   check             -> fetch reference image and ask the model\n//   human             -> leave for a person (reason says why)\nconst config = $('Config').first().json;\nlet ticket = $('Parse ticket').first().json;\n\nconst normalizeTitle = (title) => String(title ?? '').toLowerCase().replace(/[^a-z0-9ಀ-೿]+/g, ' ').trim();\n\nfunction sameTitle(first, second) {\n  const a = normalizeTitle(first);\n  const b = normalizeTitle(second);\n  if (a === '' || b === '') return false;\n  return a === b || (Math.min(a.length, b.length) >= 8 && (a.includes(b) || b.includes(a)));\n}\n\nfunction parseJson(value, fallback) {\n  if (typeof value !== 'string') return value ?? fallback;\n  try { return JSON.parse(value); } catch { return fallback; }\n}\n\nconst lookup = (bookData, bookConfig, number) => {\n  const list = bookConfig.collection ? bookData?.[bookConfig.collection] : bookData;\n  return Array.isArray(list) ? list.find((item) => String(item[bookConfig.idField]) === String(number)) : undefined;\n};\n\n// Which book was it really reported from? Trust the ticket's title over its book name.\nlet book = config.books[ticket.songType];\nlet data = parseJson($('Get vault file').first().json.data, []);\nlet titleProblem = '';\nif (ticket.songTitle) {\n  const declared = lookup(data, book, ticket.songNumber);\n  if (declared && !sameTitle(declared.title, ticket.songTitle)) {\n    const mtBook = config.books['M.T. Hymn'];\n    const mtData = parseJson($('Get M.T. vault file').first().json.data, []);\n    const mtEntry = ticket.songType === 'Hymn' ? lookup(mtData, mtBook, ticket.songNumber) : undefined;\n    if (mtEntry && sameTitle(mtEntry.title, ticket.songTitle)) {\n      ticket = { ...ticket, songType: 'M.T. Hymn' };\n      book = mtBook;\n      data = mtData;\n    } else {\n      titleProblem = `The ticket says \"${ticket.songTitle}\" but ${ticket.songType} ${ticket.songNumber} is \"${declared.title}\", and no other book matches. It may have been renamed since.`;\n    }\n  }\n}\n\nconst whitelist = parseJson($('Get whitelist').first().json.data, {});\nconst pageIndex = parseJson($('Get page index').first().json.data, {});\nconst collection = book.collection ? data?.[book.collection] : data;\nconst found = Array.isArray(collection)\n  ? collection.find((item) => String(item[book.idField]) === String(ticket.songNumber))\n  : undefined;\nconst text = found?.[book.textField];\nconst entry = typeof text === 'string' && text.trim() !== ''\n  ? { id: String(found[book.idField]), title: found.title ?? '', text }\n  : null;\nconst verified = whitelist?.[ticket.songType]?.[String(ticket.songNumber)];\nconst placed = pageIndex?.books?.[ticket.songType]?.[String(ticket.songNumber)];\nconst pages = (Array.isArray(placed?.pages) ? placed.pages : [])\n  .map((file) => ({ file, url: `${config.imageBaseUrl}/${book.imageFolder}/${encodeURIComponent(file)}` }));\n\nlet route = 'check';\nlet reason = '';\nif (!entry) {\n  route = 'human';\n  reason = `${ticket.songType} ${ticket.songNumber} was not found in ${book.file}.`;\n} else if (titleProblem) {\n  route = 'human';\n  reason = titleProblem;\n} else if (verified && !ticket.hasUserDescription) {\n  route = 'whitelisted-blank';\n} else if (verified) {\n  route = 'human';\n  reason = 'These lyrics were verified earlier, but the reporter described a specific problem.';\n} else if (pages.length === 0) {\n  route = 'human';\n  reason = `No page photo is indexed for ${ticket.songType} ${ticket.songNumber} yet.`;\n}\n\nreturn [{\n  json: {\n    ...ticket,\n    route,\n    reason,\n    verifiedAt: verified?.verifiedAt ?? '',\n    pages,\n    entry,\n  },\n}];\n" } },
  output: [{ issueKey: 'CSI-12', songType: 'Hymn', songNumber: 206, route: 'check', pages: [{ file: 'page2.jpg', url: 'https://example.com/hymns/page2.jpg' }], hasUserDescription: false, entry: { id: '206', title: 'Aghora Manakleshavu', text: 'sample' } }],
});

const routeSwitch = switchCase({
  version: 3.5,
  config: { name: "Route switch", parameters: { mode: 'rules', rules: { values: [{ outputKey: "check", renameOutput: true, conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ $json.route }}"), operator: { type: 'string', operation: 'equals' }, rightValue: "check" }], combinator: 'and' } }, { outputKey: "whitelisted-blank", renameOutput: true, conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ $json.route }}"), operator: { type: 'string', operation: 'equals' }, rightValue: "whitelisted-blank" }], combinator: 'and' } }] }, options: { fallbackOutput: 'extra', renameFallbackOutput: "human" } } },
});

const listPages = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "List pages", parameters: { mode: 'runOnceForAllItems', jsCode: "// One item per page photo the song is on, in reading order; the HTTP node downloads each.\nconst route = $('Route').first().json;\nreturn route.pages.map((page, order) => ({ json: { order, pageFile: page.file, imageUrl: page.url } }));\n" } },
  output: [{ order: 0, pageFile: 'page1.jpg', imageUrl: 'https://example.com/hymns/page1.jpg' }],
});

const getImage = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Get reference image", parameters: { method: "GET", url: expr('{{ $json.imageUrl }}'), options: { response: { response: { neverError: true, responseFormat: "file" } } } } },
  output: [{}],
});

const buildAi = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "Build OCR request", parameters: { mode: 'runOnceForAllItems', jsCode: "// Builds ONE Cloud Vision request for all the song's page photos (kept in reading order).\n// A download that did not return an image (404 page, wrong file) is listed in missingPages.\nconst config = $('Config').first().json;\nconst route = $('Route').first().json;\nconst items = $input.all();\n\nconst requests = [];\nconst missingPages = [];\nfor (let i = 0; i < items.length; i += 1) {\n  const file = route.pages[i]?.file ?? `page ${i + 1}`;\n  if (!(items[i].binary?.data?.mimeType ?? '').startsWith('image/')) {\n    missingPages.push(file);\n  } else {\n    const buffer = await this.helpers.getBinaryDataBuffer(i, 'data');\n    requests.push({\n      image: { content: buffer.toString('base64') },\n      features: [{ type: 'DOCUMENT_TEXT_DETECTION' }],\n      imageContext: { languageHints: config.ocrLanguageHints },\n    });\n  }\n}\n\nreturn [{ json: { body: { requests }, missingPages, pageFiles: route.pages.map((page) => page.file) } }];\n" } },
  output: [{ body: { requests: [{ image: { content: '' }, features: [{ type: 'DOCUMENT_TEXT_DETECTION' }], imageContext: { languageHints: ['kn'] } }] }, missingPages: [], pageFiles: ['page1.jpg'] }],
});

const askModel = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Read page (Cloud Vision)", parameters: { method: "POST", url: 'https://vision.googleapis.com/v1/images:annotate', authentication: 'genericCredentialType', genericAuthType: "httpHeaderAuth", sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr('{{ JSON.stringify($json.body) }}'), options: { timeout: 60000, response: { response: { neverError: true, responseFormat: "json" } } } }, credentials: { httpHeaderAuth: newCredential("Google Vision API key") } },
  output: [{ responses: [{ fullTextAnnotation: { text: 'sample page text' } }] }],
});

const decide = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "Decide", parameters: { mode: 'runOnceForAllItems', jsCode: "// Finds the song inside the text Cloud Vision read from its page photo(s), then compares it letter by letter.\n//   verified -> no differing words (and the reporter gave no specific complaint)\n//   fix      -> only a few small letter-for-letter differences inside words; the stored text is patched with the page's letters\n//   human    -> anything else (song not found on the page, inserted/missing letters, many differences, unreadable photo)\n// Spaces, punctuation, digits and Latin letters are ignored; OCR noise is why only exact matches are auto-closed.\nconst config = $('Config').first().json;\nconst route = $('Route').first().json;\nconst build = $('Build OCR request').first().json;\nconst body = $input.first().json;\n\nconst ZERO_WIDTH = String.fromCharCode(0x200c, 0x200d);\nconst WORD_PATTERN = new RegExp('[\\\\p{Script=Kannada}' + ZERO_WIDTH + ']+', 'gu'); // Kannada script only\nconst NUMBER_ONLY = new RegExp('^\\\\p{N}+$', 'u');\nconst ZERO_WIDTH_PATTERN = new RegExp('[' + ZERO_WIDTH + ']', 'g');\nconst keyOf = (word) => word.replace(ZERO_WIDTH_PATTERN, '').normalize('NFC');\nconst wordsOf = (text) => [...String(text).matchAll(WORD_PATTERN)]\n  .filter((m) => !NUMBER_ONLY.test(m[0]))\n  .map((m) => ({ text: m[0], key: keyOf(m[0]), start: m.index, end: m.index + m[0].length }));\n\nfunction hunksOf(stored, page) {\n  const n = stored.length;\n  const m = page.length;\n  const table = Array.from({ length: n + 1 }, () => new Int32Array(m + 1));\n  for (let i = n - 1; i >= 0; i -= 1) {\n    for (let j = m - 1; j >= 0; j -= 1) {\n      table[i][j] = stored[i].key === page[j].key ? table[i + 1][j + 1] + 1 : Math.max(table[i + 1][j], table[i][j + 1]);\n    }\n  }\n  const hunks = [];\n  let current = null;\n  let i = 0;\n  let j = 0;\n  const flush = () => { if (current) { hunks.push(current); current = null; } };\n  while (i < n || j < m) {\n    if (i < n && j < m && stored[i].key === page[j].key) {\n      flush(); i += 1; j += 1;\n    } else {\n      current ??= { del: [], ins: [] };\n      if (j >= m || (i < n && table[i + 1][j] >= table[i][j + 1])) { current.del.push(i); i += 1; } else { current.ins.push(j); j += 1; }\n    }\n  }\n  flush();\n  return hunks;\n}\n\nfunction locateWindow(stored, stream) {\n  const n = stored.length;\n  const m = stream.length;\n  if (n === 0 || m === 0) return null;\n  const table = Array.from({ length: n + 1 }, () => new Int32Array(m + 1));\n  for (let i = 1; i <= n; i += 1) {\n    table[i][0] = i;\n    for (let j = 1; j <= m; j += 1) {\n      const diagonal = table[i - 1][j - 1] + (stored[i - 1].key === stream[j - 1].key ? 0 : 1);\n      table[i][j] = Math.min(diagonal, table[i - 1][j] + 1, table[i][j - 1] + 1);\n    }\n  }\n  let end = 0;\n  for (let j = 1; j <= m; j += 1) if (table[n][j] < table[n][end]) end = j;\n  let i = n;\n  let j = end;\n  const matchedAt = [];\n  while (i > 0) {\n    if (j > 0 && stored[i - 1].key === stream[j - 1].key && table[i][j] === table[i - 1][j - 1]) {\n      matchedAt.push(j - 1); i -= 1; j -= 1;\n    } else if (j > 0 && table[i][j] === table[i - 1][j - 1] + 1) {\n      i -= 1; j -= 1;\n    } else if (table[i][j] === table[i - 1][j] + 1) {\n      i -= 1;\n    } else {\n      j -= 1;\n    }\n  }\n  matchedAt.reverse();\n  return { start: j, end, edits: table[n][end], matches: matchedAt.length, matchedAt };\n}\n\nconst KANNADA_SIGN = new RegExp('\\\\p{Script=Kannada}', 'u');\nconst ANY_NUMBER = new RegExp('\\\\p{N}', 'u');\n\nfunction compactOf(text) {\n  const chars = [];\n  const at = [];\n  let offset = 0;\n  for (const ch of String(text)) {\n    if (KANNADA_SIGN.test(ch) && !ANY_NUMBER.test(ch)) { chars.push(ch); at.push(offset); }\n    offset += ch.length;\n  }\n  return { chars, at };\n}\n\nfunction alignChars(stored, page) {\n  const n = stored.length;\n  const m = page.length;\n  if (n === 0 || m === 0) return null;\n  const table = Array.from({ length: n + 1 }, () => new Uint16Array(m + 1));\n  for (let j = 1; j <= m; j += 1) table[0][j] = j;\n  for (let i = 1; i <= n; i += 1) {\n    table[i][0] = i;\n    for (let j = 1; j <= m; j += 1) {\n      const diagonal = table[i - 1][j - 1] + (stored[i - 1] === page[j - 1] ? 0 : 1);\n      table[i][j] = Math.min(diagonal, table[i - 1][j] + 1, table[i][j - 1] + 1);\n    }\n  }\n  const ops = [];\n  let i = n;\n  let j = m;\n  while (i > 0 || j > 0) {\n    if (i === 0) { ops.push('i'); j -= 1; continue; }\n    if (j > 0 && stored[i - 1] === page[j - 1] && table[i][j] === table[i - 1][j - 1]) {\n      ops.push('m'); i -= 1; j -= 1;\n    } else if (j > 0 && table[i][j] === table[i - 1][j - 1] + 1) {\n      ops.push('s'); i -= 1; j -= 1;\n    } else if (table[i][j] === table[i - 1][j] + 1) {\n      ops.push('d'); i -= 1;\n    } else {\n      ops.push('i'); j -= 1;\n    }\n  }\n  ops.reverse();\n\n  const hunks = [];\n  let s = 0;\n  let p = 0;\n  let current = null;\n  let matches = 0;\n  ops.forEach((op) => {\n    if (op === 'm') {\n      if (current) { current.sj = s; current.pj = p; hunks.push(current); current = null; }\n      matches += 1; s += 1; p += 1;\n      return;\n    }\n    current ??= { si: s, pi: p, sj: s, pj: p };\n    if (op !== 'i') s += 1;\n    if (op !== 'd') p += 1;\n  });\n  if (current) { current.sj = s; current.pj = p; hunks.push(current); }\n  return { edits: table[n][m], matches, hunks };\n}\n\nconst wordAround = (text, from, to) => {\n  let a = from;\n  while (a > 0 && !/\\s/.test(text[a - 1])) a -= 1;\n  let b = to;\n  while (b < text.length && !/\\s/.test(text[b])) b += 1;\n  return text.slice(a, b).trim();\n};\n\nfunction compareSong(storedText, pageText, { maxHunks = 6, maxHunkChars = 12 } = {}) {\n  const empty = { found: false, coverage: 0, similarity: 0, hunks: [], applied: [], skipped: [], fixable: false, corrected: null, notes: '' };\n  // 1. Find the song on the page by words (robust to stray text and to words OCR split or joined).\n  const storedWords = wordsOf(storedText);\n  const pageWords = wordsOf(pageText);\n  const located = locateWindow(storedWords, pageWords);\n  if (!located || located.end <= located.start || located.matches === 0) return empty;\n  const windowFrom = pageWords[located.start].start;\n  const windowTo = pageWords[located.end - 1].end;\n  const windowText = pageText.slice(windowFrom, windowTo);\n\n  // 2. Compare that stretch with the stored text letter by letter, end to end.\n  const stored = compactOf(storedText);\n  const page = compactOf(windowText);\n  const hit = alignChars(stored.chars, page.chars);\n  if (!hit) return empty;\n\n  const offsetOf = (side, text, index) => (index < side.chars.length ? side.at[index] : text.length);\n  const endOffsetOf = (side, from, to, startOffset) => (to > from ? side.at[to - 1] + side.chars[to - 1].length : startOffset);\n\n  const hunks = hit.hunks.map((h) => {\n    const storedFrom = offsetOf(stored, storedText, h.si);\n    const storedTo = endOffsetOf(stored, h.si, h.sj, storedFrom);\n    const pageFrom = windowFrom + offsetOf(page, windowText, h.pi);\n    const pageTo = pageFrom + (h.pj > h.pi ? page.at[h.pj - 1] + page.chars[h.pj - 1].length - page.at[h.pi] : 0);\n    // the stretch must be Kannada letters only (no space, punctuation or zero-width sign in the middle)\n    const cleanSlice = storedText.slice(storedFrom, storedTo) === stored.chars.slice(h.si, h.sj).join('');\n    return {\n      ...h,\n      storedFrom,\n      storedTo,\n      replacement: page.chars.slice(h.pi, h.pj).join(''),\n      stored: wordAround(storedText, storedFrom, storedTo),\n      page: wordAround(pageText, pageFrom, pageTo),\n      // a small letter-for-letter change inside one word\n      substitution: h.sj > h.si && h.pj > h.pi && h.sj - h.si <= maxHunkChars && h.pj - h.pi <= maxHunkChars && cleanSlice,\n    };\n  });\n\n  // Substitutions are applied; insertions and deletions are more likely OCR dropping or adding a sign than a real\n  // typo, so they are only reported, for a person to check against the book.\n  const applied = hunks.filter((h) => h.substitution);\n  const skipped = hunks.filter((h) => !h.substitution);\n  const fixable = applied.length > 0 && applied.length <= maxHunks;\n  let corrected = null;\n  if (fixable) {\n    corrected = storedText;\n    [...applied].sort((a, b) => b.storedFrom - a.storedFrom).forEach((h) => {\n      corrected = corrected.slice(0, h.storedFrom) + h.replacement + corrected.slice(h.storedTo);\n    });\n    if (corrected === storedText) corrected = null;\n  }\n  const show = (list) => list.slice(0, 8).map((h) => `\"${h.stored}\" -> \"${h.page}\"`).join('; ');\n  return {\n    found: true,\n    coverage: hit.matches / stored.chars.length,\n    similarity: Math.max(0, 1 - hit.edits / stored.chars.length),\n    hunks,\n    applied,\n    skipped,\n    fixable: fixable && corrected !== null,\n    corrected,\n    notes: skipped.length > 0 && applied.length > 0\n      ? `${show(applied)} | not applied, check by hand: ${show(skipped)}`\n      : show(hunks),\n  };\n}\n\nfunction paragraphText(paragraph) {\n  return (paragraph.words ?? []).map((word) => {\n    const symbols = word.symbols ?? [];\n    const text = symbols.map((symbol) => symbol.text ?? '').join('');\n    const kind = symbols.length > 0 ? symbols[symbols.length - 1].property?.detectedBreak?.type : undefined;\n    return text + (kind === 'LINE_BREAK' || kind === 'EOL_SURE_SPACE' ? '\\n' : kind === 'SPACE' || kind === 'SURE_SPACE' ? ' ' : '');\n  }).join('');\n}\n\nfunction pageTextOf(annotation) {\n  const items = [];\n  (annotation?.pages?.[0]?.blocks ?? []).forEach((block) => (block.paragraphs ?? []).forEach((paragraph) => {\n    const vertices = paragraph.boundingBox?.vertices ?? [];\n    if (vertices.length === 0) return;\n    const xs = vertices.map((vertex) => vertex.x ?? 0);\n    const ys = vertices.map((vertex) => vertex.y ?? 0);\n    items.push({ text: paragraphText(paragraph), x0: Math.min(...xs), x1: Math.max(...xs), y0: Math.min(...ys), y1: Math.max(...ys) });\n  }));\n  if (items.length < 4) return annotation?.text ?? items.map((item) => item.text).join('\\n');\n\n  const width = Math.max(...items.map((item) => item.x1));\n  const height = Math.max(...items.map((item) => item.y1));\n  const centre = (item) => (item.x0 + item.x1) / 2;\n  const average = (values) => values.reduce((sum, value) => sum + value, 0) / values.length;\n  const byTop = (a, b) => a.y0 - b.y0 || a.x0 - b.x0;\n\n  // two-means on the horizontal centres: are there two columns?\n  let low = Math.min(...items.map(centre));\n  let high = Math.max(...items.map(centre));\n  for (let round = 0; round < 12; round += 1) {\n    const left = items.filter((item) => Math.abs(centre(item) - low) <= Math.abs(centre(item) - high));\n    const right = items.filter((item) => !left.includes(item));\n    if (left.length === 0 || right.length === 0) break;\n    low = average(left.map(centre));\n    high = average(right.map(centre));\n  }\n  const middle = (low + high) / 2;\n  const leftCount = items.filter((item) => centre(item) <= middle).length;\n  const twoColumns = high - low > width * 0.3 && leftCount >= 2 && items.length - leftCount >= 2;\n  if (!twoColumns) return [...items].sort(byTop).map((item) => item.text.trim()).join('\\n');\n\n  const isHeader = (item) => item.y0 < height * 0.12 && Math.abs(centre(item) - middle) < width * 0.12;\n  const headers = items.filter(isHeader).sort(byTop);\n  const rest = items.filter((item) => !isHeader(item));\n  const leftColumn = rest.filter((item) => centre(item) <= middle).sort(byTop);\n  const rightColumn = rest.filter((item) => centre(item) > middle).sort(byTop);\n  return [...headers, ...leftColumn, ...rightColumn].map((item) => item.text.trim()).join('\\n');\n}\n\nconst responses = body.responses ?? [];\nconst failure = body.error ?? responses.find((item) => item.error)?.error;\nconst pageText = responses.map((item) => pageTextOf(item.fullTextAnnotation)).join('\\n');\nconst original = route.entry.text ?? '';\n\nlet verdict = 'human';\nlet reason = '';\nlet corrected = null;\nlet similarity = 0;\nlet notes = '';\n\nif (build.missingPages.length > 0) {\n  reason = `The page photo(s) ${build.missingPages.join(', ')} could not be downloaded.`;\n} else if (failure || pageText.trim() === '') {\n  reason = `Cloud Vision could not read the page photo(s)${failure?.message ? `: ${failure.message}` : '.'}`;\n} else {\n  const result = compareSong(original, pageText, { maxHunks: config.maxAutoEditHunks, maxHunkChars: config.maxHunkChars });\n  similarity = result.similarity;\n  notes = result.notes;\n\n  if (!result.found || result.coverage < config.minWindowCoverage) {\n    reason = `Could not find this song's text on the page photo(s): only ${Math.round(result.coverage * 100)}% of its letters matched. Is the page index right, or is the photo unreadable?`;\n  } else if (result.hunks.length === 0) {\n    if (route.hasUserDescription) reason = 'The page text matches, but the reporter described a specific problem.';\n    else verdict = 'verified';\n  } else if (result.fixable) {\n    verdict = 'fix';\n    corrected = result.corrected;\n  } else {\n    reason = `The page differs from the stored text in ${result.hunks.length} place(s) in a way that needs a person to check.`;\n  }\n}\n\nconst slug = route.songType.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');\n// one branch per song: later reports of the same song find the correction that is already open\nconst branch = `fix/lyrics-${slug}-${route.songNumber}`;\n\nreturn [{ json: { ...route, verdict, reason, corrected, confidence: Number(similarity.toFixed(3)), notes, branch, pageFiles: build.pageFiles } }];\n" } },
  output: [{ issueKey: 'CSI-12', songType: 'Hymn', songNumber: 206, verdict: 'fix', branch: 'fix/lyrics-hymn-206-csi-12', corrected: 'sample', confidence: 0.95, notes: '', entry: { id: '206', title: 'T', text: 'sample' } }],
});

const verdictSwitch = switchCase({
  version: 3.5,
  config: { name: "Verdict switch", parameters: { mode: 'rules', rules: { values: [{ outputKey: "verified", renameOutput: true, conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ $json.verdict }}"), operator: { type: 'string', operation: 'equals' }, rightValue: "verified" }], combinator: 'and' } }, { outputKey: "fix", renameOutput: true, conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ $json.verdict }}"), operator: { type: 'string', operation: 'equals' }, rightValue: "fix" }], combinator: 'and' } }] }, options: { fallbackOutput: 'extra', renameFallbackOutput: "human" } } },
});

const getWl = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Get whitelist file (API)", parameters: { method: "GET", url: expr("https://api.github.com/repos/{{ $(\"Config\").first().json.vaultRepo }}/contents/{{ $(\"Config\").first().json.whitelistFile }}?ref={{ $(\"Config\").first().json.vaultBranch }}"), authentication: 'predefinedCredentialType', nodeCredentialType: "githubApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Accept", value: "application/vnd.github+json" }] }, options: { response: { response: { neverError: true, responseFormat: "json" } } } }, credentials: { githubApi: newCredential("GitHub vault token") } },
  output: [{ sha: 'abc', content: '' }],
});

const buildWl = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "Build whitelist commit", parameters: { mode: 'runOnceForAllItems', jsCode: "// Adds the hymn to verified_lyrics.json via the GitHub contents API (PUT body built here).\n// Shape: { \"Hymn\": { \"206\": { \"verifiedAt\": \"2026-11-19\", \"ticket\": \"CSI-12\" } }, \"Keerthane\": { ... } }\nconst config = $('Config').first().json;\nconst verdict = $('Decide').first().json;\nconst existing = $input.first().json;\n\nlet whitelist = {};\nif (existing.sha && existing.content) {\n  try {\n    whitelist = JSON.parse(Buffer.from(existing.content, 'base64').toString('utf8'));\n  } catch {\n    throw new Error('verified_lyrics.json exists but is not valid JSON; fix it before re-running.');\n  }\n}\n\nconst typeKey = verdict.songType;\nconst next = {\n  ...whitelist,\n  [typeKey]: {\n    ...(whitelist[typeKey] ?? {}),\n    [String(verdict.songNumber)]: {\n      verifiedAt: new Date().toISOString().slice(0, 10),\n      ticket: verdict.issueKey,\n    },\n  },\n};\n\nconst content = Buffer.from(`${JSON.stringify(next, null, 2)}\\n`, 'utf8').toString('base64');\nreturn [{\n  json: {\n    putUrl: `https://api.github.com/repos/${config.vaultRepo}/contents/${config.whitelistFile}`,\n    putBody: {\n      message: `chore: mark ${typeKey} ${verdict.songNumber} as verified (${verdict.issueKey})`,\n      content,\n      branch: config.vaultBranch,\n      ...(existing.sha ? { sha: existing.sha } : {}),\n    },\n  },\n}];\n" } },
  output: [{ putUrl: 'https://api.github.com/repos/x/y/contents/verified_lyrics.json', putBody: { message: 'm', content: '', branch: 'main' } }],
});

const commitWl = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Commit whitelist", parameters: { method: "PUT", url: expr('{{ $json.putUrl }}'), authentication: 'predefinedCredentialType', nodeCredentialType: "githubApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Accept", value: "application/vnd.github+json" }] }, sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr('{{ JSON.stringify($json.putBody) }}'), options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { githubApi: newCredential("GitHub vault token") } },
  output: [{}],
});

const findPr = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Find open PR", parameters: { method: "GET", url: expr("https://api.github.com/repos/{{ $(\"Config\").first().json.vaultRepo }}/pulls?state=open&head={{ $(\"Config\").first().json.vaultRepo.split(\"/\")[0] }}:{{ $(\"Decide\").first().json.branch }}"), authentication: 'predefinedCredentialType', nodeCredentialType: "githubApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Accept", value: "application/vnd.github+json" }] }, options: { response: { response: { neverError: true, responseFormat: "json" } } } }, credentials: { githubApi: newCredential("GitHub vault token") }, alwaysOutputData: true },
  output: [{ html_url: 'https://github.com/x/y/pull/3' }],
});

const prOpen = ifElse({
  version: 2.4,
  config: { name: "Fix already open", parameters: { conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ Boolean($json.html_url) }}"), operator: { type: 'boolean', operation: 'true', singleValue: true }, rightValue: '' }], combinator: 'and' } } },
});

const getFile = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Get vault file (API)", parameters: { method: "GET", url: expr("https://api.github.com/repos/{{ $(\"Config\").first().json.vaultRepo }}/contents/{{ $(\"Config\").first().json.books[$(\"Decide\").first().json.songType].file }}?ref={{ $(\"Config\").first().json.vaultBranch }}"), authentication: 'predefinedCredentialType', nodeCredentialType: "githubApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Accept", value: "application/vnd.github+json" }] }, options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { githubApi: newCredential("GitHub vault token") } },
  output: [{ sha: 'abc', content: '' }],
});

const patch = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "Patch lyrics", parameters: { mode: 'runOnceForAllItems', jsCode: "// Replaces ONE entry's text inside the vault file by exact text substitution, so the file's own\n// formatting is untouched and the PR diff shows only that entry. Never throws: when the patch\n// cannot be applied safely it returns patchOk=false and the ticket goes to a human.\nconst config = $('Config').first().json;\nconst verdict = $('Decide').first().json;\nconst file = $input.first().json; // GitHub contents API response for the vault file\n\nfunction fail(reason) {\n  return [{ json: { patchOk: false, reason } }];\n}\n\nconst book = config.books[verdict.songType];\nconst raw = Buffer.from(file.content ?? '', 'base64').toString('utf8');\nconst oldEncoded = JSON.stringify(verdict.entry.text);\nconst newEncoded = JSON.stringify(verdict.corrected);\n\nif (!file.sha || raw === '') return fail(`Could not read ${book.file} from the vault.`);\nif (raw.split(oldEncoded).length - 1 !== 1) {\n  return fail(`The current text of ${verdict.songType} ${verdict.songNumber} no longer matches the vault file exactly once.`);\n}\n\nconst patchedRaw = raw.split(oldEncoded).join(newEncoded);\n\n// Safety net: the patched file must equal the original with just this one text changed.\nconst select = (data) => (book.collection ? data[book.collection] : data);\nconst expected = JSON.parse(raw);\nconst target = select(expected).find((item) => String(item[book.idField]) === verdict.entry.id);\nif (!target) return fail('Entry vanished while patching.');\ntarget[book.textField] = verdict.corrected;\nif (JSON.stringify(expected) !== JSON.stringify(JSON.parse(patchedRaw))) {\n  return fail('The patched file would differ from the original in more than the one entry.');\n}\n\nreturn [{\n  json: {\n    patchOk: true,\n    branch: verdict.branch,\n    putUrl: `https://api.github.com/repos/${config.vaultRepo}/contents/${book.file}`,\n    putBody: {\n      message: `fix: correct ${verdict.songType} ${verdict.songNumber} text (${verdict.issueKey})`,\n      content: Buffer.from(patchedRaw, 'utf8').toString('base64'),\n      sha: file.sha,\n      branch: verdict.branch,\n    },\n  },\n}];\n" } },
  output: [{ patchOk: true, branch: 'fix/lyrics-hymn-206-csi-12', putUrl: 'https://api.github.com/repos/x/y/contents/hymns_data.json', putBody: { message: 'm', content: '', sha: 'abc', branch: 'fix/lyrics-hymn-206-csi-12' } }],
});

const patched = ifElse({
  version: 2.4,
  config: { name: "Patch applied", parameters: { conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ $json.patchOk }}"), operator: { type: 'boolean', operation: 'true', singleValue: true }, rightValue: '' }], combinator: 'and' } } },
});

const getRef = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Get main ref", parameters: { method: "GET", url: expr("https://api.github.com/repos/{{ $(\"Config\").first().json.vaultRepo }}/git/ref/heads/{{ $(\"Config\").first().json.vaultBranch }}"), authentication: 'predefinedCredentialType', nodeCredentialType: "githubApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Accept", value: "application/vnd.github+json" }] }, options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { githubApi: newCredential("GitHub vault token") } },
  output: [{ object: { sha: 'abc123' } }],
});

const createBranch = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Create fix branch", parameters: { method: "POST", url: expr("https://api.github.com/repos/{{ $(\"Config\").first().json.vaultRepo }}/git/refs"), authentication: 'predefinedCredentialType', nodeCredentialType: "githubApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Accept", value: "application/vnd.github+json" }] }, sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr('{{ JSON.stringify({ ref: "refs/heads/" + $("Decide").first().json.branch, sha: $json.object.sha }) }}'), options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { githubApi: newCredential("GitHub vault token") } },
  output: [{}],
});

const commitFix = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Commit fix", parameters: { method: "PUT", url: expr('{{ $("Patch lyrics").first().json.putUrl }}'), authentication: 'predefinedCredentialType', nodeCredentialType: "githubApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Accept", value: "application/vnd.github+json" }] }, sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr('{{ JSON.stringify($("Patch lyrics").first().json.putBody) }}'), options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { githubApi: newCredential("GitHub vault token") } },
  output: [{}],
});

const openPr = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Open PR", parameters: { method: "POST", url: expr("https://api.github.com/repos/{{ $(\"Config\").first().json.vaultRepo }}/pulls"), authentication: 'predefinedCredentialType', nodeCredentialType: "githubApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Accept", value: "application/vnd.github+json" }] }, sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr("{{ JSON.stringify({ title: \"fix: correct \" + $(\"Decide\").first().json.songType + \" \" + $(\"Decide\").first().json.songNumber + \" text (\" + $(\"Decide\").first().json.issueKey + \")\", head: $(\"Decide\").first().json.branch, base: $(\"Config\").first().json.vaultBranch, draft: true, body: \"Proposed by the automatic lyrics check for \" + $(\"Decide\").first().json.issueKey + \".\\n\\nPage photo(s): \" + $(\"Decide\").first().json.pageFiles.join(\", \") + \"\\n\\nSimilarity: \" + $(\"Decide\").first().json.confidence + \"\\n\\nDifferences (stored -> page; anything marked not applied needs checking by hand): \" + $(\"Decide\").first().json.notes + \"\\n\\nOCR can misread Kannada, so compare each change with the printed page before merging.\" }) }}"), options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { githubApi: newCredential("GitHub vault token") } },
  output: [{ html_url: 'https://github.com/x/y/pull/7' }],
});

const buildComment = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: "Build comment", parameters: { mode: 'runOnceForAllItems', jsCode: "const adf = (text) => ({\n  body: { type: 'doc', version: 1, content: [{ type: 'paragraph', content: [{ type: 'text', text }] }] },\n});\n// Builds the Jira comment (and transition/label payloads) for the path this item came through.\n// The app shows comments that do not contain \"[via CSI Android App]\" as replies from the team.\nconst config = $('Config').first().json;\nconst decided = $('Decide').isExecuted ? $('Decide').first().json : $('Route').first().json;\nconst patch = $('Patch lyrics').isExecuted ? $('Patch lyrics').first().json : null;\n// A correction that could not be applied safely is handled like any other uncertain case.\nconst v = patch && patch.patchOk === false ? { ...decided, verdict: 'human', reason: patch.reason } : decided;\nconst label = v.songType.startsWith('Order of Service ')\n  ? `Order of Service (${v.songType.slice('Order of Service '.length)}) page ${v.songNumber}`\n  : `${v.songType} ${v.songNumber}`;\n\n// a correction PR opened just now, or one that an earlier ticket for the same song already opened\nconst pullRequestUrl = $('Open PR').isExecuted\n  ? ($('Open PR').first().json.html_url ?? '')\n  : ($('Find open PR').isExecuted ? ($('Find open PR').first().json.html_url ?? '') : '');\n\nconst comments = {\n  'whitelisted-blank':\n    `Automated check: the lyrics for ${label} were already verified against the printed book on ${v.verifiedAt || 'an earlier date'}. ` +\n    'They are correct and up to date, so this ticket is being closed. If you still see a problem, please send a new report ' +\n    'and describe exactly what is wrong in the description box.',\n  verified:\n    `Automated check: the lyrics for ${label} match the printed book. They are correct and up to date, so this ticket is being closed. ` +\n    'If you still see a problem, please send a new report and describe exactly what is wrong in the description box.',\n  fix:\n    `Automated check: we found a difference in ${label} and prepared a correction (${pullRequestUrl}). ` +\n    'It will be reviewed and published in the next content update. Thank you for reporting it.',\n  human:\n    `Thanks for the report on ${label}. It has been flagged for a manual check. ` +\n    `(${v.reason || 'No reference image was available for an automatic check.'})`,\n};\n\nconst kind = v.route === 'whitelisted-blank' ? 'whitelisted-blank' : v.verdict ?? 'human';\nconst transitionId = ['whitelisted-blank', 'verified'].includes(kind)\n  ? config.jiraDoneTransitionId\n  : kind === 'fix' ? config.jiraReviewTransitionId : '';\n\n// Internal note for the maintainer (hidden from the reporter): what was compared and where to look.\nconst outcomes = {\n  'whitelisted-blank': 'closed: hymn is on the verified list',\n  verified: 'closed: the page matches the stored text exactly',\n  fix: 'draft correction PR opened; ticket moved to review',\n  human: 'left open: needs a person to check',\n};\nconst lines = [\n  `Automated check for ${label}: ${outcomes[kind] ?? outcomes.human}.`,\n  ...(v.reason ? [`Reason: ${v.reason}`] : []),\n  ...(typeof v.confidence === 'number' && v.confidence > 0 ? [`Letters identical to the page: ${Math.round(v.confidence * 1000) / 10}%.`] : []),\n  ...(v.notes ? [`Differences (stored -> page; OCR can misread, compare with the photo): ${v.notes}`] : []),\n  ...((v.pages ?? []).length > 0 ? [`Page photo(s): ${v.pages.map((page) => page.url).join(' , ')}`] : []),\n  ...(pullRequestUrl ? [`Correction PR: ${pullRequestUrl}`] : []),\n];\nconst internalBody = {\n  body: { type: 'doc', version: 1, content: lines.map((text) => ({ type: 'paragraph', content: [{ type: 'text', text }] })) },\n  properties: [{ key: 'sd.public.comment', value: { internal: true } }],\n};\n\nreturn [{\n  json: {\n    issueKey: v.issueKey,\n    kind,\n    commentBody: adf(comments[kind] ?? comments.human),\n    internalBody,\n    transitionId,\n    labelBody: { update: { labels: [{ add: config.humanReviewLabel }] } },\n    internalNote: v.notes ?? '',\n  },\n}];\n" } },
  output: [{ issueKey: 'CSI-12', kind: 'verified', commentBody: { body: { type: 'doc', version: 1, content: [] } }, transitionId: '31', labelBody: { update: { labels: [{ add: 'needs-human-review' }] } } }],
});

const postComment = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Post Jira comment", parameters: { method: "POST", url: expr("{{ $(\"Config\").first().json.jiraBase }}/rest/api/3/issue/{{ $json.issueKey }}/comment"), authentication: 'predefinedCredentialType', nodeCredentialType: "jiraSoftwareCloudApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Content-Type", value: "application/json" }] }, sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr('{{ JSON.stringify($json.commentBody) }}'), options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { jiraSoftwareCloudApi: newCredential("Jira Cloud") } },
  output: [{}],
});

const hasTransition = ifElse({
  version: 2.4,
  config: { name: "Has transition", parameters: { conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ $(\"Build comment\").first().json.transitionId !== \"\" }}"), operator: { type: 'boolean', operation: 'true', singleValue: true }, rightValue: '' }], combinator: 'and' } } },
});

const transition = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Transition issue", parameters: { method: "POST", url: expr("{{ $(\"Config\").first().json.jiraBase }}/rest/api/3/issue/{{ $(\"Build comment\").first().json.issueKey }}/transitions"), authentication: 'predefinedCredentialType', nodeCredentialType: "jiraSoftwareCloudApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Content-Type", value: "application/json" }] }, sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr('{{ JSON.stringify({ transition: { id: $("Build comment").first().json.transitionId } }) }}'), options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { jiraSoftwareCloudApi: newCredential("Jira Cloud") } },
  output: [{}],
});

const flag = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Flag for human review", parameters: { method: "PUT", url: expr("{{ $(\"Config\").first().json.jiraBase }}/rest/api/3/issue/{{ $(\"Build comment\").first().json.issueKey }}"), authentication: 'predefinedCredentialType', nodeCredentialType: "jiraSoftwareCloudApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Content-Type", value: "application/json" }] }, sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr('{{ JSON.stringify($("Build comment").first().json.labelBody) }}'), options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { jiraSoftwareCloudApi: newCredential("Jira Cloud") } },
  output: [{}],
});

const confirmed = ifElse({
  version: 2.4,
  config: { name: "Lyrics confirmed correct", parameters: { conditions: { options: { caseSensitive: true, leftValue: '', typeValidation: 'strict', version: 2 }, conditions: [{ leftValue: expr("{{ [\"verified\", \"whitelisted-blank\"].includes($(\"Build comment\").first().json.kind) }}"), operator: { type: 'boolean', operation: 'true', singleValue: true }, rightValue: '' }], combinator: 'and' } } },
});

const findOwner = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Find ticket owner", parameters: { method: "GET", url: expr("{{ $(\"Config\").first().json.supabaseUrl }}/rest/v1/jira_tickets?ticket_key=eq.{{ $(\"Build comment\").first().json.issueKey }}&select=user_id"), authentication: 'predefinedCredentialType', nodeCredentialType: "supabaseApi", options: { response: { response: { neverError: true, responseFormat: "json" } } } }, credentials: { supabaseApi: newCredential("Supabase (service role)") }, disabled: true },
  output: [{ user_id: '00000000-0000-0000-0000-000000000000' }],
});

const findToken = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Find push token", parameters: { method: "GET", url: expr("{{ $(\"Config\").first().json.supabaseUrl }}/rest/v1/users?auth_uid=eq.{{ $json.user_id ?? \"00000000-0000-0000-0000-000000000000\" }}&select=fcm_token"), authentication: 'predefinedCredentialType', nodeCredentialType: "supabaseApi", options: { response: { response: { neverError: true, responseFormat: "json" } } } }, credentials: { supabaseApi: newCredential("Supabase (service role)") }, disabled: true },
  output: [{ fcm_token: 'token' }],
});

const buildPush = node({
  type: 'n8n-nodes-base.code',
  version: 2,
  config: { name: 'Build push payload', disabled: true, parameters: { mode: 'runOnceForAllItems', jsCode: "// Builds the FCM v1 message. HymnsFirebaseMessagingService reads data keys title / message / target_screen.\nconst ticket = $('Parse ticket').first().json;\nconst tokenRows = $input.first().json;\nconst rows = Array.isArray(tokenRows) ? tokenRows : [tokenRows];\nconst token = rows.find((row) => row && row.fcm_token)?.fcm_token;\n\nif (!token) return []; // guest or no token stored: the Jira comment still reaches them in the app\n\nreturn [{\n  json: {\n    message: {\n      token,\n      data: {\n        title: `${ticket.songType} ${ticket.songNumber} lyrics`,\n        message: 'We checked your report: the lyrics are correct and up to date. Open the app to read our reply.',\n        target_screen: 'tickets',\n      },\n    },\n  },\n}];\n" } },
  output: [{ message: { token: 'token', data: { title: 't', message: 'm', target_screen: 'tickets' } } }],
});

const sendPush = node({
  type: 'n8n-nodes-base.httpRequest',
  version: 4.5,
  config: { name: "Send push (FCM)", parameters: { method: "POST", url: expr("https://fcm.googleapis.com/v1/projects/{{ $(\"Config\").first().json.fcmProjectId }}/messages:send"), authentication: 'predefinedCredentialType', nodeCredentialType: "googleApi", sendHeaders: true, headerParameters: { parameters: [{ name: "Content-Type", value: "application/json" }] }, sendBody: true, contentType: 'json', specifyBody: 'json', jsonBody: expr('{{ JSON.stringify($json) }}'), options: { response: { response: { neverError: false, responseFormat: "json" } } } }, credentials: { googleApi: newCredential("Firebase service account") }, disabled: true },
  output: [{}],
});

export default workflow('csi-lyrics-ticket-triage', 'CSI Hymns - lyrics ticket triage')
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
    .onFalse(flag));
