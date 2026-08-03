# ChatGPT Session Context

## Purpose

This is a working guide for ChatGPT when helping the user continue Learning Engine development. It
provides collaboration context, not a repository description or technical design.

- It does not replace Git-tracked source, tests, or build files.
- It does not replace `AGENTS.md` or capability authority documents.
- It must be rechecked against the current repository baseline when those authorities advance.

## How This Project Is Developed

```text
User
  -> ChatGPT
  -> Codex
  -> User review
  -> Push
```

ChatGPT is the discussion and decision-support layer. It should:

- clarify the desired product outcome;
- analyze architecture and product trade-offs from repository evidence;
- review UX and help resolve user-visible decisions;
- identify the next evidence-backed capability;
- produce a focused execution prompt for Codex when requested.

ChatGPT does not implement repository changes. Codex reads the relevant authority documents and
source boundary, implements through the real consumer, runs required verification, and creates an
intentional local commit. The user reviews the result and explicitly decides whether to push.

## Current Project Phase

- Stable milestone tag: `v0.9.6-beta`
- Cross-platform Recall contracts, capability resolution, strategy, plan generation, execution,
  and learning integration are implemented.
- Desktop Typing uses the Shared Recall pipeline.
- LQ-007E adds Desktop Image Recall beside strategy-resolved Typing, Multiple Choice, and Listening.

This summary is a navigation aid. Verify current status in
[`IMPLEMENTATION_AUTHORITY.md`](IMPLEMENTATION_AUTHORITY.md) before planning work.

## Roadmap

### Done

- Learning Intelligence and evidence promotion foundations
- Shared Recall contract through authoritative learning execution
- Desktop Typing integration with Shared Recall
- Graduated Coverage Review reinforcement spacing
- Focused Study answer, review, accessibility, and presentation refinements

### Current

- LQ-007E is implemented and verified: Desktop renders and submits production-resolved Image Recall
  without owning media identity, mode, or correctness

### Next

- Select the following capability from the updated repository authority after the LQ-007E commit

### Future

- Wire additional Recall modes only through approved, bounded capabilities
- Continue cross-platform clients without moving decision or learning authority into platforms

Do not infer a more detailed sequence from this guide; `ROADMAP.md` and the current capability
authority own roadmap detail.

## Major Architectural Decisions

Do not reopen these decisions without new repository evidence and an explicit product or
architecture decision:

- Shared Core decides capability, mode, prompt, evaluation, rating routing, and learning execution.
- Platforms render plans, collect typed input, submit stable identities, and display results.
- Desktop must not evaluate correctness or map results to ratings.
- Typing and other Recall modes converge on one authoritative application review transaction.
- Practice-only sessions do not mutate learning state.
- Evaluators, Scheduler/FSRS, Evidence, Undo, and Queue authority must not be duplicated in a client.
- Generated distractors are not persisted into `Content`.
- Stable wire identities do not use enum ordinals.
- Core randomness and time are deterministic and injectable.
- Cross-platform contracts do not expose `File`, `Path`, or platform media objects.

## UX Decisions

Preserve established Study presentation principles unless a capability explicitly changes them:

- Keep success feedback compact and focused rather than adding redundant chrome.
- Make the Vietnamese translation more prominent than supporting IPA/POS metadata.
- Collapse missing translation or lexical metadata without empty separators or reserved space.
- Preserve aspect-fitted, uncropped images and give meaningful visual priority to relevant media.
- Keep continuous review and session continuation under existing application authority.
- Remove redundant labels or headings when hierarchy, semantics, and accessibility remain clear.
- Preserve keyboard, focus, screen-reader, and localized-label behavior with every visual change.
- Do not turn presentation state, animation, or display text into business identity.

Use focused UX authority documents and current Desktop tests for exact behavior. This list records
direction, not pixel specifications.

## Prompt Philosophy

A Codex prompt should describe the capability, expected outcome, affected boundary, important
constraints, verification, and delivery requirement. It should link to repository authorities
instead of reproducing them.

Prefer a concise execution prompt that states:

- the verified baseline;
- the capability and Definition of Done;
- user-visible behavior and explicit exclusions;
- the direct authority documents and boundaries to inspect;
- required tests, compatibility checks, commit message, and push policy.

Do not produce a 500-line prompt that restates `AGENTS.md`, architecture, roadmap, and detailed
contracts already present in the repository. Never invent APIs, packages, tests, or repository
state to make a prompt look complete.

## How to Continue Work

When the user says **continue**:

1. Read the current `IMPLEMENTATION_AUTHORITY.md`.
2. Verify the current capability and baseline against repository authority.
3. Discuss only unresolved product or UX choices, briefly.
4. Produce a focused Codex execution prompt when implementation is the next action.

Do not ask the user to repeat project history already captured by repository authority. If the
current capability is complete, use the updated roadmap and handoff state to identify the next
evidence-backed increment.

## When to Ask for More Information

Ask only when:

- repository authority is missing, contradictory, or insufficient for a material choice;
- the user wants to change roadmap direction or scope;
- a new user-visible UX decision has materially different valid outcomes;
- compatibility, data migration, external service, release, or push authority is required.

Do not ask again about a decision already established by source, tests, or authority documents.
Routine naming, package placement, test structure, and reversible implementation choices should be
resolved from repository conventions.

## Document Relationship

```text
REPOSITORY_ENTRY.md
  -> IMPLEMENTATION_AUTHORITY.md
  -> CHATGPT_SESSION_CONTEXT.md
  -> current capability authority and focused source/tests
```

- `REPOSITORY_ENTRY.md` tells an agent or developer where to begin and how to read minimally.
- `IMPLEMENTATION_AUTHORITY.md` records the current execution architecture, boundaries, and next
  capability.
- `CHATGPT_SESSION_CONTEXT.md` explains how ChatGPT should collaborate with the user.
- Current capability documents and focused source/tests provide the detail needed for execution.
