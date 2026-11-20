// Edit these values, then run `node build.mjs` to regenerate the workflow code.
return [{
  json: {
    vaultRepo: 'Reynold29/csi-hymns-vault',
    vaultBranch: 'main',
    jiraBase: 'https://YOUR-SITE.atlassian.net',
    // Reference page photos: {imageBaseUrl}/{imageFolder}/{page file named in page-index.json}
    imageBaseUrl: 'https://YOUR-HOST/lyrics-images',
    // One entry per book. `songType` is what the app writes in the Jira summary:
    // "<songType> <id> Lyrics Issue".
    books: {
      'Hymn': { file: 'hymns_data.json', collection: '', idField: 'number', textField: 'kannadaLyrics', imageFolder: 'hymns' },
      'Keerthane': { file: 'keerthane_data.json', collection: '', idField: 'number', textField: 'kannadaLyrics', imageFolder: 'keerthanes' },
      'M.T. Hymn': { file: 'mangalore_hymns_data.json', collection: '', idField: 'number', textField: 'kannadaLyrics', imageFolder: 'mangalore-tunes' },
      'Order of Service Regular': { file: 'order-of-service_data.json', collection: 'regular', idField: 'page_no', textField: 'content', imageFolder: 'order-of-service/regular' },
      'Order of Service Festival': { file: 'order-of-service_data.json', collection: 'festival', idField: 'page_no', textField: 'content', imageFolder: 'order-of-service/festival' },
    },
    whitelistFile: 'verified_lyrics.json',
    // Built by tools/build-page-index.mjs: which page photo(s) each song is on.
    pageIndexFile: 'page-index.json',
    // The song's text must be found on the page photo(s) with at least this share of its letters.
    minWindowCoverage: 0.6,
    // OCR language hints for Google Cloud Vision (kn = Kannada).
    ocrLanguageHints: ['kn'],
    // A correction PR is proposed only when at most this many places differ, each a letter-for-letter
    // substitution of at most this many letters inside one word. Anything else goes to a person.
    maxAutoEditHunks: 6,
    maxHunkChars: 12,
    // Find these ids in Jira: GET /rest/api/3/issue/KEY/transitions
    jiraDoneTransitionId: '31',
    jiraReviewTransitionId: '21',
    humanReviewLabel: 'needs-human-review',
    supabaseUrl: 'https://YOUR-PROJECT.supabase.co',
    fcmProjectId: 'YOUR-FIREBASE-PROJECT-ID',
  },
}];
