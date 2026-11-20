/*ADF*/
// Builds the Jira comment (and transition/label payloads) for the path this item came through.
// The app shows comments that do not contain "[via CSI Android App]" as replies from the team.
const config = $('Config').first().json;
const decided = $('Decide').isExecuted ? $('Decide').first().json : $('Route').first().json;
const patch = $('Patch lyrics').isExecuted ? $('Patch lyrics').first().json : null;
// A correction that could not be applied safely is handled like any other uncertain case.
const v = patch && patch.patchOk === false ? { ...decided, verdict: 'human', reason: patch.reason } : decided;
const label = v.songType.startsWith('Order of Service ')
  ? `Order of Service (${v.songType.slice('Order of Service '.length)}) page ${v.songNumber}`
  : `${v.songType} ${v.songNumber}`;

// a correction PR opened just now, or one that an earlier ticket for the same song already opened
const pullRequestUrl = $('Open PR').isExecuted
  ? ($('Open PR').first().json.html_url ?? '')
  : ($('Find open PR').isExecuted ? ($('Find open PR').first().json.html_url ?? '') : '');

const comments = {
  'whitelisted-blank':
    `Automated check: the lyrics for ${label} were already verified against the printed book on ${v.verifiedAt || 'an earlier date'}. ` +
    'They are correct and up to date, so this ticket is being closed. If you still see a problem, please send a new report ' +
    'and describe exactly what is wrong in the description box.',
  verified:
    `Automated check: the lyrics for ${label} match the printed book. They are correct and up to date, so this ticket is being closed. ` +
    'If you still see a problem, please send a new report and describe exactly what is wrong in the description box.',
  fix:
    `Automated check: we found a difference in ${label} and prepared a correction (${pullRequestUrl}). ` +
    'It will be reviewed and published in the next content update. Thank you for reporting it.',
  human:
    `Thanks for the report on ${label}. It has been flagged for a manual check. ` +
    `(${v.reason || 'No reference image was available for an automatic check.'})`,
};

const kind = v.route === 'whitelisted-blank' ? 'whitelisted-blank' : v.verdict ?? 'human';
const transitionId = ['whitelisted-blank', 'verified'].includes(kind)
  ? config.jiraDoneTransitionId
  : kind === 'fix' ? config.jiraReviewTransitionId : '';

// Internal note for the maintainer (hidden from the reporter): what was compared and where to look.
const outcomes = {
  'whitelisted-blank': 'closed: hymn is on the verified list',
  verified: 'closed: the page matches the stored text exactly',
  fix: 'draft correction PR opened; ticket moved to review',
  human: 'left open: needs a person to check',
};
const lines = [
  `Automated check for ${label}: ${outcomes[kind] ?? outcomes.human}.`,
  ...(v.reason ? [`Reason: ${v.reason}`] : []),
  ...(typeof v.confidence === 'number' && v.confidence > 0 ? [`Letters identical to the page: ${Math.round(v.confidence * 1000) / 10}%.`] : []),
  ...(v.notes ? [`Differences (stored -> page; OCR can misread, compare with the photo): ${v.notes}`] : []),
  ...((v.pages ?? []).length > 0 ? [`Page photo(s): ${v.pages.map((page) => page.url).join(' , ')}`] : []),
  ...(pullRequestUrl ? [`Correction PR: ${pullRequestUrl}`] : []),
];
const internalBody = {
  body: { type: 'doc', version: 1, content: lines.map((text) => ({ type: 'paragraph', content: [{ type: 'text', text }] })) },
  properties: [{ key: 'sd.public.comment', value: { internal: true } }],
};

return [{
  json: {
    issueKey: v.issueKey,
    kind,
    commentBody: adf(comments[kind] ?? comments.human),
    internalBody,
    transitionId,
    labelBody: { update: { labels: [{ add: config.humanReviewLabel }] } },
    internalNote: v.notes ?? '',
  },
}];
