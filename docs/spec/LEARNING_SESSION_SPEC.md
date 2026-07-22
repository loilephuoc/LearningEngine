# Learning Session Product Specification

## Purpose

A Learning Session turns an intentional content choice into a calm sequence of retrieval,
feedback, and completion. This specification defines learner-visible behavior across Desktop,
Android, iOS, and Web. Learning Engine remains authoritative for selection, scheduling, rating,
history, progress, persistence, undo, and recovery; platforms present and collect interaction.

## Experience principles

- The next meaningful action is obvious.
- Thinking is protected from accidental reveal or autoplay.
- Rich media supports recall; it never fabricates recall evidence.
- A rating describes recall quality, not whether the learner likes the content.
- Every automatic behavior is visible, optional, interruptible, and recoverable.
- The learner may stop without punishment and resume without duplicate work.
- Advanced choices appear progressively; safe defaults require no setup.

## Journey overview

```text
Entry → Scope → Setup → Prepare → Think → Optional help → Reveal → Rate
                                      ↑                    │
                                      └──── next item ─────┘
                         → Complete → Summary → Leave / Continue
```

## 1. Entry

The learner enters from Home, Library, a topic/lesson, Recents, or Resume. The entry surface
shows one primary action:

- **Resume session** when an active recoverable session exists;
- **Start learning** when no active session exists;
- **Continue lesson/topic** when scope is already known.

Starting from a different scope while a session is active never silently replaces it. The
learner resumes, explicitly ends it, or returns. “Pause” means leaving an active session; it is
not a separate durable learning state.

## 2. Topic and lesson selection

The learner navigates Library → Package → Topic → Section → Lesson. A node communicates title,
short description when available, item count, due/new availability, and recent/pinned state.
Missing hierarchy gracefully collapses: a package may contain lessons directly, and a single
lesson may start without intermediate screens.

Single-lesson learning is the initial mandatory behavior. Multi-lesson selection is a later
explicit plan: selected lessons are summarized before start, duplicates are removed, missing
content blocks start before mutation, and deterministic engine planning decides item order.

## 3. Session setup

The default setup is concise:

- scope summary;
- estimated item budget, distinguishing new and due review when known;
- learning preset: Standard by default; Listening and Typed Recall only when supported;
- optional session limits inherited from validated learner preferences;
- media availability warning that does not block text-capable study.

The platform submits an immutable setup request. It does not construct or reorder the durable
queue. If setup fails, no session is created and the learner’s selections remain visible.

## 4. Preparation

After Start, the product immediately shows a preparing state and allows cancellation. The first
card appears only after the engine confirms an active session and current item. Slow loading
shows honest progress or a phase label. Errors retain context and offer Retry or Back; repeated
actions cannot create duplicate sessions.

## 5. Learning card lifecycle

Each card has these presentation phases:

1. **Thinking** — prompt available; rating unavailable.
2. **Assisted thinking** — optional hint, prompt audio, or image requested by the learner.
3. **Reveal** — authoritative answer and examples become visible; reveal is idempotent.
4. **Rating** — Again, Hard, Good, Easy become available in stable order.
5. **Feedback/transition** — committed rating and scheduling feedback are acknowledged once.
6. **Next or Completed** — engine projection selects the outcome.

Platforms may animate between phases, but animation never owns or delays durable state.

## 6. Thinking phase

The prompt is the visual and semantic focus. Elapsed response time may be measured by the
engine contract but is not shown as pressure by default. Answer, examples, and answer audio are
hidden. The learner may:

- reveal;
- play explicitly designated prompt audio;
- show an available hint;
- show an available prompt image if hidden by preference;
- pause/leave.

The learner cannot rate before reveal unless a future exercise contract explicitly produces
equivalent answer evidence; that is not assumed by this specification.

## 7. Hint phase

Hints are progressive and optional. A hint never changes scheduler state and never counts as a
review. Product copy should distinguish a hint from the answer. If content has no authored
hint, the control is absent rather than disabled. Hint usage may be retained only if a future
learning-evidence contract explicitly requires it; current UI state is transient.

## 8. Audio phase

Audio has semantic roles defined in `MEDIA_SPEC.md`. Prompt audio may play during thinking;
answer and example audio are unavailable until reveal unless Listening preset explicitly
defines a non-answer prompt sequence. Manual replay is always available for playable media.
Playback stops on item transition, pause, completion, or conflicting playback. Playback errors
show a local fallback and never fail or rate the card.

## 9. Reveal phase

Reveal displays the answer, explanation/translation, examples, and answer-side media in a
stable reading order. It does not advance the queue or commit a review. Repeated Reveal input
has no effect. Focus moves to the rating group without stealing focus from assistive technology
mid-announcement.

## 10. Image phase

Images are content blocks, not decorations when they carry meaning. Prompt images may appear
during thinking; answer images follow reveal. Each image has authored or safe fallback
description. The learner can enlarge an image without changing session state. Missing, unsafe,
or unreadable images render an explicit unavailable block and preserve the text flow.

## 11. Example phase

Examples follow the primary answer and are visually subordinate. Bilingual pairs remain
associated and readable in deterministic order. Example audio is manually playable after
reveal. Examples never become independent ratings unless authored as separate learning items.

## 12. Typing phase

Typed Recall is optional and requires a declared exercise/evaluator contract. The learner types
an answer, submits, receives a transparent comparison, then reveals/rates. Input, cursor,
selection, and partial text are transient and are not session recovery state. Hint and Show
answer remain escape paths. Evaluation failure must permit normal reveal; it cannot create a
partial review. Matching tolerance, accepted alternatives, punctuation, case, diacritics, and
locale remain Product Owner decisions until the exercise contract is approved.

## 13. Rating phase

Ratings are always presented as:

1. **Again** — recall failed or was materially incorrect;
2. **Hard** — recalled with substantial difficulty;
3. **Good** — recalled correctly with normal effort;
4. **Easy** — recalled correctly and effortlessly.

The platform sends exactly one rating intent. While it is pending, all rating inputs are
disabled. Feedback appears only after atomic commit. Failure keeps the revealed card and offers
Retry; it never optimistically advances. Undo reverses only the latest committed rating through
the engine boundary and restores that card’s recoverable projection.

## 14. Completion phase

Completion occurs only when the authoritative queue/session reports completion. The final card
does not disappear before its rating is committed. The surface acknowledges completion, stops
media, and offers Summary. If the last rating is undone, the session reopens deterministically.

## 15. Summary phase

The summary is neutral and useful:

- reviewed count and new/review split;
- completed scope and optional lesson progress;
- rating distribution and elapsed duration when authoritative;
- next due outlook when available;
- actions: Done, Continue with another scope, Review history.

No confetti, streak threat, artificial urgency, or unsupported learning claim is required.
Summary values come from engine projections, never screen counters.

## 16. Resume and interruption recovery

Resume restores the same active session, queue, current item, reveal state, pending scheduler
intent, and one-step undo checkpoint already owned by the engine. It does not create a new
session. Platforms may restore their own stable user preferences but not focus, scroll,
animation, expanded panels, playback position, partial typing, or Compose/view state.

After interruption:

- a committed rating is shown exactly once and is never recommitted;
- a staged pending rating completes/reconciles through the existing recovery contract;
- an uncommitted UI click is not inferred as a rating;
- missing/corrupt recovery data yields an explicit safe recovery outcome, not silent repair;
- media remains stopped until a new explicit or configured playback action.

## Accessibility and localization

Every phase exposes its name, current content, progress context, and available action through
semantic order. Visible and spoken rating order is identical. Keyboard, touch, pointer, and
assistive actions dispatch the same commands. English and Vietnamese are initial locales;
content language is independent of interface locale.

## Product uncertainties

- multi-lesson ordering and maximum selection;
- exact Listening preset reveal policy;
- typed-answer tolerance and whether authored content may require typing;
- whether hints affect future scheduling evidence;
- which summary trends are helpful without becoming distracting.
