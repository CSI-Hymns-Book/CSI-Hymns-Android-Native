// Builds ONE Cloud Vision request for all the song's page photos (kept in reading order).
// A download that did not return an image (404 page, wrong file) is listed in missingPages.
const config = $('Config').first().json;
const route = $('Route').first().json;
const items = $input.all();

const requests = [];
const missingPages = [];
for (let i = 0; i < items.length; i += 1) {
  const file = route.pages[i]?.file ?? `page ${i + 1}`;
  if (!(items[i].binary?.data?.mimeType ?? '').startsWith('image/')) {
    missingPages.push(file);
  } else {
    const buffer = await this.helpers.getBinaryDataBuffer(i, 'data');
    requests.push({
      image: { content: buffer.toString('base64') },
      features: [{ type: 'DOCUMENT_TEXT_DETECTION' }],
      imageContext: { languageHints: config.ocrLanguageHints },
    });
  }
}

return [{ json: { body: { requests }, missingPages, pageFiles: route.pages.map((page) => page.file) } }];
