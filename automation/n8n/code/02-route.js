// Looks the entry up in the book's vault JSON, checks the whitelist, and picks a route:
//   whitelisted-blank -> reply "already verified" and close
//   check             -> fetch reference image and ask the model
//   human             -> leave for a person (reason says why)
const config = $('Config').first().json;
let ticket = $('Parse ticket').first().json;

/*TITLE_LIB*/

function parseJson(value, fallback) {
  if (typeof value !== 'string') return value ?? fallback;
  try { return JSON.parse(value); } catch { return fallback; }
}

const lookup = (bookData, bookConfig, number) => {
  const list = bookConfig.collection ? bookData?.[bookConfig.collection] : bookData;
  return Array.isArray(list) ? list.find((item) => String(item[bookConfig.idField]) === String(number)) : undefined;
};

// Which book was it really reported from? Trust the ticket's title over its book name.
let book = config.books[ticket.songType];
let data = parseJson($('Get vault file').first().json.data, []);
let titleProblem = '';
if (ticket.songTitle) {
  const declared = lookup(data, book, ticket.songNumber);
  if (declared && !sameTitle(declared.title, ticket.songTitle)) {
    const mtBook = config.books['M.T. Hymn'];
    const mtData = parseJson($('Get M.T. vault file').first().json.data, []);
    const mtEntry = ticket.songType === 'Hymn' ? lookup(mtData, mtBook, ticket.songNumber) : undefined;
    if (mtEntry && sameTitle(mtEntry.title, ticket.songTitle)) {
      ticket = { ...ticket, songType: 'M.T. Hymn' };
      book = mtBook;
      data = mtData;
    } else {
      titleProblem = `The ticket says "${ticket.songTitle}" but ${ticket.songType} ${ticket.songNumber} is "${declared.title}", and no other book matches. It may have been renamed since.`;
    }
  }
}

const whitelist = parseJson($('Get whitelist').first().json.data, {});
const pageIndex = parseJson($('Get page index').first().json.data, {});
const collection = book.collection ? data?.[book.collection] : data;
const found = Array.isArray(collection)
  ? collection.find((item) => String(item[book.idField]) === String(ticket.songNumber))
  : undefined;
const text = found?.[book.textField];
const entry = typeof text === 'string' && text.trim() !== ''
  ? { id: String(found[book.idField]), title: found.title ?? '', text }
  : null;
const verified = whitelist?.[ticket.songType]?.[String(ticket.songNumber)];
const placed = pageIndex?.books?.[ticket.songType]?.[String(ticket.songNumber)];
const pages = (Array.isArray(placed?.pages) ? placed.pages : [])
  .map((file) => ({ file, url: `${config.imageBaseUrl}/${book.imageFolder}/${encodeURIComponent(file)}` }));

let route = 'check';
let reason = '';
if (!entry) {
  route = 'human';
  reason = `${ticket.songType} ${ticket.songNumber} was not found in ${book.file}.`;
} else if (titleProblem) {
  route = 'human';
  reason = titleProblem;
} else if (verified && !ticket.hasUserDescription) {
  route = 'whitelisted-blank';
} else if (verified) {
  route = 'human';
  reason = 'These lyrics were verified earlier, but the reporter described a specific problem.';
} else if (pages.length === 0) {
  route = 'human';
  reason = `No page photo is indexed for ${ticket.songType} ${ticket.songNumber} yet.`;
}

return [{
  json: {
    ...ticket,
    route,
    reason,
    verifiedAt: verified?.verifiedAt ?? '',
    pages,
    entry,
  },
}];
