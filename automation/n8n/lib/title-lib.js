// Song-title matching, shared by the workflow's Route node and the dry-run tool.
// App builds up to 5.2.0 reported M.T. hymns as "Hymn <n>", but the ticket still carries the song's title,
// so the title tells us which book the report really came from.
const normalizeTitle = (title) => String(title ?? '').toLowerCase().replace(/[^a-z0-9ಀ-೿]+/g, ' ').trim();

function sameTitle(first, second) {
  const a = normalizeTitle(first);
  const b = normalizeTitle(second);
  if (a === '' || b === '') return false;
  return a === b || (Math.min(a.length, b.length) >= 8 && (a.includes(b) || b.includes(a)));
}
