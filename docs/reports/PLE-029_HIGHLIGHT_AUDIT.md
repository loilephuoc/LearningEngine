# PLE-029 Highlight Audit

Audit context: clean `develop` baseline
`6133abf6f8d9367e00e11522480a0a4619de5144`, scanned 2026-07-28.
Only aggregate metadata is recorded; private package text, paths, media names and learner state
are excluded.

## Sources and method

- Active installed `contents.json`: 3,417 records.
- Git-tracked import, OPD3, legacy, sample and test fixtures.
- Embedded two-line translation is recognized only when translation audio supplies authoritative
  language evidence, matching the presentation contract.
- Baseline is case-insensitive literal containment. The normalized prescreen applies NFC,
  apostrophe/hyphen canonicalization and flexible whitespace. Rendering additionally validates
  lexical boundaries and maps matches to original indices.

| Metric | Count |
|---|---:|
| Records with English example | 3,415 |
| Baseline English literal-compatible | 2,996 |
| Normalized English candidate-compatible | 2,996 |
| English unresolved | 419 |
| Records with Vietnamese example | 3,401 |
| Baseline Vietnamese literal-compatible | 1,612 |
| Normalized Vietnamese candidate-compatible | 1,613 |
| Vietnamese unresolved | 1,788 |

Of unresolved candidates, 393 English keys and 1,677 Vietnamese meanings are multi-word; 12
unresolved English keys contain an apostrophe variant. These groups do not imply that matching
should be forced.

Failure classes include semantic paraphrase/inflection, changed multi-word surface or word order,
insufficient evidence to split bilingual text, punctuation/metadata outside the example surface,
and absent fields. Safe regression representatives are `I don't mind`, `traffic warden`,
`mother-in-law`, `take off`, and composed/decomposed `tiếng Việt`.

The matcher intentionally does not infer grammar (`write` → `wrote`, `go` → `went`), remove
Vietnamese diacritics, mutate package content, or claim every unresolved record is recoverable.
