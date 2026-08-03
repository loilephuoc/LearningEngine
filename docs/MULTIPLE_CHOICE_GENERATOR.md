# Deterministic Multiple Choice Generator

LQ-006F evaluates generated choices solely by stable option ID from the validated plan. Platform
display text, position, and correctness claims are never authority.

LQ-006E implements the LQ-006D option-provider boundary in Shared Core. Candidate inventory is a
typed, caller-supplied projection from one active package/topic/scope; the generator never scans a
repository, reads platform state, or persists generated choices.

## Candidate authority and safety

Each candidate carries Content identity, scope, source/target answer sides, optional POS/topic
facts, lifecycle state, provenance, and explicit malformed/ambiguity facts. ContentId is the
authority: the target, repeated sibling projections, out-of-scope and inactive content, missing or
blank answer sides, malformed records, normalized duplicates, correct-answer collisions, accepted
alternative collisions, and ambiguous candidates are rejected with stable typed reasons.

Normalization keeps display text separate from comparison text and follows the configured Unicode
NFC, whitespace, case, and punctuation ambiguity policy. It does not strip accents, translate, or
invent semantic relationships. If unique correctness cannot be established, generation fails.

## Ranking and fallback

Validated policy owns option counts, scan bound, POS/topic/length weights, reduced-count behavior,
and ambiguity comparison. Ranking prefers same POS and topic, then same POS within scope, then any
safe in-scope candidate. A reduced option count is explicit and never falls below the minimum safe
count. Insufficient inventory returns a typed failure; no synthetic distractors are created.

Candidates are normalized once, deduplicated before ranking, bounded for large inventories, and
ranked with stable ContentId/seed tie-breaking. Option IDs derive from Content identity. A local
deterministic shuffle distributes the correct position while identical inputs and seeds reproduce
the same membership and order.

## Provider and platform boundary

`DeterministicMultipleChoiceOptionProvider` adapts the typed generator result to the plan factory.
Successful output records provenance, fallback tier, scanned/eligible counts, and typed rejected
candidates. Invalid or insufficient output remains a typed factory failure. The final LQ-006D
validation still enforces at least two choices and exactly one canonical correct answer.

The generator supports source-to-target, target-to-source, image-to-text, and audio-to-text answer
sides without loading media. It has no Desktop, Compose, Android, browser, filesystem, AI/network,
Scheduler, Evidence, trajectory, queue, or mutation dependency. This phase stops before UI and
submission/result execution.
