// Google Cloud Vision OCR for one image file. The key comes from GOOGLE_VISION_API_KEY and is never printed.
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { diff } from './shared.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const CACHE_DIR = join(here, '.ocr-cache');
const ENDPOINT = 'https://vision.googleapis.com/v1/images:annotate';
const MAX_ATTEMPTS = 3;

export class VisionError extends Error {}

export async function ocrImage(path, { languageHints = ['kn'], useCache = true } = {}) {
  const bytes = readFileSync(path);
  // the cache holds Vision's full layout (not just text) so the reading order can be rebuilt
  const cacheFile = join(CACHE_DIR, `${createHash('sha1').update(bytes).update(languageHints.join()).digest('hex')}.layout.json`);
  if (useCache && existsSync(cacheFile)) return diff.pageTextOf(JSON.parse(readFileSync(cacheFile, 'utf8')));

  const apiKey = process.env.GOOGLE_VISION_API_KEY;
  if (!apiKey) throw new VisionError('Set GOOGLE_VISION_API_KEY in your terminal first (never paste the key into a file or chat).');

  const body = JSON.stringify({
    requests: [{
      image: { content: bytes.toString('base64') },
      features: [{ type: 'DOCUMENT_TEXT_DETECTION' }],
      imageContext: { languageHints },
    }],
  });

  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt += 1) {
    const response = await fetch(ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Goog-Api-Key': apiKey },
      body,
    });
    const retryable = response.status === 429 || response.status >= 500;
    if (retryable && attempt < MAX_ATTEMPTS) {
      await new Promise((resolve) => setTimeout(resolve, 1500 * attempt));
      continue;
    }
    const payload = await response.json().catch(() => ({}));
    const failure = payload.error ?? payload.responses?.[0]?.error;
    if (!response.ok || failure) throw new VisionError(`Cloud Vision rejected ${path}: ${failure?.message ?? response.status}`);
    const annotation = payload.responses?.[0]?.fullTextAnnotation ?? {};
    mkdirSync(CACHE_DIR, { recursive: true });
    writeFileSync(cacheFile, JSON.stringify(annotation));
    return diff.pageTextOf(annotation);
  }
  throw new VisionError(`Cloud Vision did not answer for ${path}`);
}
