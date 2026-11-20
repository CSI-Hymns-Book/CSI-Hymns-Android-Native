// One item per page photo the song is on, in reading order; the HTTP node downloads each.
const route = $('Route').first().json;
return route.pages.map((page, order) => ({ json: { order, pageFile: page.file, imageUrl: page.url } }));
