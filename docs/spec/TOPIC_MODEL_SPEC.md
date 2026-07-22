# Learner-Facing Topic Model Specification

## Purpose

The learner needs a stable mental model for finding, organizing, and starting learning. This
model is independent of package storage and platform widgets. Imported package structure is
projected into the hierarchy; it is not silently rewritten.

## Vocabulary

| Concept | Learner meaning | Ownership |
|---|---|---|
| Library | learner’s installed and organized learning content | application projection + durable collection data |
| Package | install/update/delete unit supplied by an author | package domain |
| Topic | meaningful subject or course inside a package/library | content metadata/projection |
| Section | optional grouping within a topic | content metadata/projection |
| Lesson | smallest named scope a learner intentionally starts | content/application projection |
| Session | engine-owned learning attempt over immutable content scope | session domain/application |
| Review | one committed rating and scheduler transition | review domain/application |

A package is not automatically a topic. A package may contain several topics; a topic may have
sections; sections may contain lessons. Missing levels collapse without fake “Default” nodes.

## Identity and ordering

Every node has a stable source-scoped identity, title, optional description, parent identity,
and authored deterministic order. UI sorting/filtering is a projection and does not change
authored order. Titles are not identities. Duplicate titles are allowed when parent/path makes
them distinct.

## Library behavior

Library shows installed packages and learner collections. It supports hierarchy browsing,
Unicode search, recent scopes, and pinned scopes. It communicates unavailable/missing content
without deleting learner organization. Empty Library guides import; failed loading preserves
the last known safe view and offers Retry.

## Import

Import validates package structure, compatibility, content, media, and conflicts before atomic
registration. Success exposes a package through Library projection. Partial directory import
reports each failed candidate while retaining successful candidates. Import never starts a
learning session automatically.

## Update

Update is package replacement under existing compatibility/dependency policy. Before mutation,
the product shows package identity, old/new version, affected learner organization, and any
blocking compatibility issue. Existing review history and memory state remain associated by
stable learning-item identity; a migration or orphan policy requires explicit approval.

## Delete

Delete/uninstall identifies package, dependent packages, attached collections, and durable
learner-data consequence before confirmation. It never silently deletes history or memory.
While an active session references package content, deletion is blocked or deferred; it does
not mutate the active queue beneath the learner.

## Duplicate policy

- Same package identity/version: deterministic already-installed or re-import result.
- Same package identity/new version: update workflow, not duplicate install.
- Different identity/same title: allowed and disambiguated by source/package metadata.
- Duplicate content identity inside one package: validation failure before mutation.
- Duplicate learner-facing title: allowed; path and package are shown when needed.
- Duplicate selection in a multi-lesson plan: collapse by identity before engine planning.

## Favorites, pinned, and recents

- **Favorite:** learner-content relationship for an item or lesson; durable only after a data
  compatibility contract is approved. It never modifies imported package content.
- **Pinned:** learner-chosen shortcut to a package/topic/section/lesson; durable, ordered by the
  learner, and resilient to display-title changes. Missing targets remain visible as unavailable
  until the learner removes them or an approved repair exists.
- **Recent:** derived from authoritative session/start activity, bounded and privacy-preserving;
  it is not a second history ledger.

## Daily study

Daily Study is a query/planning entry point, not a container. It asks the engine for eligible
new/due items under learner limits and selected Library scope. It displays the proposed scope
and budget before creating a session. Goals are informational and computed from authoritative
history; missing goals preserve ordinary Daily Study.

## Session entry behavior

Each eligible node exposes Start/Continue Learning. Starting produces an immutable scope request
of content identities; application planning produces the queue. A node with no eligible items
explains why and offers relevant navigation rather than creating an empty session.

## Search and discovery

Search spans visible titles, descriptions, paths, and approved metadata with Unicode-safe
multi-term semantics. Results disclose searchable scope and preserve hierarchy context. Search
never matches or exposes private review content beyond the current learner profile.

## Compatibility and fallback

Legacy flat packages project lessons directly beneath the package. Missing descriptions,
counts, or due summaries are omitted or marked unavailable; they are not invented. Existing
Library collections and package attachments remain valid. Introducing Topic/Section projection
must not require rewriting package or persistence files.

## Open decisions

- whether topics may span packages through learner-created collections;
- allowed favorite targets and their deletion/orphan behavior;
- pinned ordering and maximum count;
- multi-lesson authored versus learner-defined order;
- update behavior when stable content identities disappear.
