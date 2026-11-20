// Reads the Jira "issue created" webhook and extracts what the app put in the ticket.
// Summary format: "<Hymn|Keerthane|M.T. Hymn|Order of Service Regular|Order of Service Festival> <id> Lyrics Issue".
const BOOK_TYPES = ['Hymn', 'Keerthane', 'M.T. Hymn', 'Order of Service Regular', 'Order of Service Festival'];

const body = $('Webhook').first().json.body ?? {};
const fields = body.issue?.fields ?? {};
const labels = fields.labels ?? [];
const summary = fields.summary ?? '';

function adfText(node) {
  if (!node) return '';
  if (typeof node === 'string') return node;
  if (node.type === 'text') return node.text ?? '';
  return (node.content ?? []).map(adfText).join(node.type === 'doc' ? '\n' : '');
}

const match = summary.match(/^(.*?)\s+(\d+)\s+Lyrics Issue$/i);
const songType = match ? BOOK_TYPES.find((type) => type.toLowerCase() === match[1].trim().toLowerCase()) ?? '' : '';
const isLyrics = labels.includes('lyrics-issue') && !labels.includes('audio-contribution') && songType !== '';

const text = adfText(fields.description);
const titleLine = text.match(/\*\s*Title:\s*(.+)/);
const described = text.match(/\*Issue Description:\*\s*([\s\S]*?)\s*\*Reported via:\*/);
const userDescription = (described?.[1] ?? '').trim();
const hasUserDescription = userDescription !== '' && userDescription !== 'No description provided';

return [{
  json: {
    issueKey: body.issue?.key ?? '',
    isLyrics,
    songType,
    songNumber: match ? Number(match[2]) : 0,
    songTitle: titleLine ? titleLine[1].trim() : '',
    userDescription: hasUserDescription ? userDescription : '',
    hasUserDescription,
  },
}];
