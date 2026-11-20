# Lyrics ticket triage (n8n)

When the app creates a `lyrics-issue` Jira ticket, this workflow compares the entry with a photo of the printed
page with Google Cloud Vision OCR, then closes the ticket, opens a correction PR in the lyrics vault, or leaves it for a person.
No AI model is involved: the OCR text is compared word by word with the stored text in a Code node.
It covers all four books: Hymns, Keerthanes, M.T. Hymns and the Order of Service (regular and festival).

Live copy: workflow `JhPzwadwQtyI0bHg` on your n8n (inactive until you publish it). The live webhook path is a random
string (`/webhook/lyrics-ticket-<random>`), set directly in n8n; the path in the generated SDK file is only a placeholder,
so do not re-create the workflow from it without setting a new random path.

```
Jira webhook -> parse ticket -> whitelist + vault + page-index lookup -> route
  whitelisted, no description ........ reply "already verified", close
  no page photo indexed for the song . reply, label needs-human-review
  otherwise .......................... download its page photo(s) -> Cloud Vision OCR -> find the song inside the page text -> word-by-word diff
      no differing words ............. add to verified_lyrics.json, reply, close, (push)
      a few one-for-one word diffs ... read file, patch ONLY those words, branch + PR, reply with PR link, move to review
      anything else .................. reply, label needs-human-review
  whitelisted but reporter wrote details -> human
```

The comparison works on Kannada letters only: spacing, punctuation, stanza numbers, digits and Latin text (`L.M.`,
`KANNADA HYMNS`, stray `C`) are ignored, so an OCR word join/split is not a difference but a changed letter or sign is
(for example `ಕ್ರು` vs `ಕೃ`). Only an exact letter match is auto-closed. Differences that are small letter-for-letter
substitutions inside a word (at most `maxHunkChars` letters, at most `maxAutoEditHunks` of them) go into a **draft** PR;
insertions and deletions are probably OCR dropping or adding a sign, so they are only listed ("not applied, check by hand")
in the PR body and the Jira comment. The PR also lists the page photo(s) used.

## Page photos and the page index

Photos are whole book pages, named `page1.jpg`, `page2.png`, ... inside the book's folder of the images repo
(`hymns/`, `keerthanes/`, `mangalore-tunes/`, `order-of-service/regular/`, `order-of-service/festival/`). Extensions
`.jpg`, `.jpeg` and `.png` are all fine. A page may hold several songs and a song may run over several pages.

`tools/build-page-index.mjs` reads every page with Cloud Vision, matches the text against the stored songs, and writes
`page-index.json` (song -> page files) into the images repo. The workflow reads that file, downloads the song's pages,
reads them with Vision in one request, finds the song's words inside the page text (`lib/diff-lib.js`) and compares only that.

```
export GOOGLE_VISION_API_KEY='...'        # type it in your terminal; never put it in a file or chat
cd automation/n8n
node tools/ocr-check.mjs "<images repo>/hymns/page1.jpg" --book Hymn --data "<vault repo folder>"
node tools/build-page-index.mjs --images "<images repo folder>" --data "<vault repo folder>"
```

`ocr-check` shows what Vision reads from one page and how well it matches your stored songs (do this first).
`build-page-index` writes `page-index.json`; re-run it whenever you add pages. OCR results are cached in
`tools/.ocr-cache/` (git-ignored) so a re-run only reads new or changed photos. Without `--data` the tools use the song
data bundled in the app, which can be older than the vault.

## The ticket contract (what the app must send)

Summary `"<songType> <id> Lyrics Issue"` with label `lyrics-issue`:

| Book | songType | id | Vault file | Field checked |
|---|---|---|---|---|
| Hymns | `Hymn` | `number` | `hymns_data.json` | `kannadaLyrics` |
| Keerthanes | `Keerthane` | `number` | `keerthane_data.json` | `kannadaLyrics` |
| M.T. Hymns | `M.T. Hymn` | `number` | `mangalore_hymns_data.json` | `kannadaLyrics` |
| Order of Service | `Order of Service Regular` / `Order of Service Festival` | `page_no` | `order-of-service_data.json` (`regular` / `festival`) | `content` |

The app sends these values from `LyricsTicketType` (`data/LyricsTicketType.kt`); keep it in sync with the table above.
M.T. hymns report as `M.T. Hymn`, and the Order of Service reader has a report button on each page. Builds released
before this change report M.T. hymns as `Hymn`, so those tickets are checked against the wrong book.

## Files

| File | Purpose |
|---|---|
| `code/*.js` | Logic of each Code node (edit these) |
| `lib/diff-lib.js` | Word matching shared by the Decide node and the page-index tools |
| `tools/` | `ocr-check.mjs`, `build-page-index.mjs` (run on your Mac with your Vision key) |
| `build.mjs` | Generates `lyrics-ticket-triage.sdk.ts` (n8n Workflow SDK code) |
| `test.mjs`, `test-tools.mjs` | Code nodes against sample tickets and real data; page-index matcher on fake noisy book pages |

After a change: `node test.mjs && node test-tools.mjs && node build.mjs`, then update the live workflow in n8n (the SDK file is a reference; the live workflow is the source of truth).

## Safety choices

- Corrections go to a branch and PR, never straight to `main` (the app syncs `main`).
- The patch replaces one entry's text by exact substitution, so file formatting is untouched; it refuses unless the
  old text appears exactly once and the patched file equals the original with only that entry changed.
- Auto-close needs an exact word match and no reporter description. Anything else goes to a person.
- Whitelist commits go to `main`; a wrong entry hides real reports, hence the threshold.

## Companion workflows (live in n8n only)

Two smaller workflows sit next to the triage workflow. They are not generated from this folder; their webhook paths are
random strings set in n8n and are not stored here.

| Workflow | Trigger | What it does |
|---|---|---|
| Lyrics PR merged - close Jira ticket | GitHub webhook, pull request closed | When a `fix/lyrics-*` PR is merged into `main`, moves each `CHB-###` ticket named in the PR title or body to Done (through Work in progress when needed) and comments with the PR link. |
| Lyrics verified by person - whitelist and close tickets | Jira webhook, issue updated | When a ticket labelled `needs-human-review` is moved to Done by a person, adds that song to `verified_lyrics.json` and closes the other open blank reports for the same song (matched by number and title). Reports where the reporter wrote something stay open. |

Every ticket the triage workflow handles also gets an internal-only Jira note (not visible to the reporter) with the
differences found, the percentage of identical letters, the page photo links and any correction PR.

## Status and limits

- Run end to end on real tickets for Hymns 1-6 (the vault now has `verified_lyrics.json` for those six).
- The push branch (last four nodes of the triage workflow) is disabled until Supabase and Firebase credentials exist.
- OCR is only a reader: it cuts or joins words, so some differences it reports are not real. Only exact matches close
  a ticket on their own, and one-for-one letter substitutions go to a draft PR; everything else is listed for a person.
- Only the Hymns book has page photos so far. The other books go to human review until their pages are added and indexed.
- Builds up to 5.2.0 report M.T. hymns as `Hymn`; the workflow tells them apart by the song title in the ticket.
- The Jira API token is still compiled into the app.
