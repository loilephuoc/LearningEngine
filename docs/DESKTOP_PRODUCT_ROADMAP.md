# Desktop Product Roadmap

This roadmap begins after the current Desktop 1.0 external/manual gates. Capability IDs map to
the Android behavior matrix but are ordered by learner outcome, not feature parity. Each is an
independently buildable commit-sized capability; Product Owner decisions gate those marked
`Decision`.

## DP-01 — Hierarchical learning scope (M / Medium)

- **Learner problem:** packages with many lessons are hard to navigate as one flat list.
- **Journey:** Library → package hierarchy → section/lesson → inspect scope.
- **Evidence:** TOPIC-001/002; current Lesson Browser is flat.
- **Scope:** query projection for package/section/lesson hierarchy and lazy Desktop navigation.
- **Excluded:** queue changes, authoring, multi-select.
- **Owner:** application query + Desktop.
- **Dependency:** current Content Library metadata.
- **Compatibility:** no persisted mutation.
- **Failure:** missing hierarchy falls back to current lesson list.
- **Automated acceptance:** deterministic grouping/order, Unicode/search, large-data lazy list,
  real composition.
- **Manual UAT:** navigate a representative large package without losing context.
- **Release impact:** minor; no migration.
- **Order:** 1.

## DP-02 — Multi-lesson session plan (L / High / Decision)

- **Learner problem:** a learner cannot intentionally combine a small set of lessons.
- **Journey:** select lessons → review deterministic summary → start → resume same scope.
- **Evidence:** TOPIC-003/004; engine has immutable content scope but no multi-lesson UI contract.
- **Scope:** typed application request, duplicate/missing validation, deterministic plan,
  persisted session scope, Desktop selection summary.
- **Excluded:** ad-hoc queue editing during a session.
- **Owner:** application session planning + Desktop.
- **Dependency:** DP-01 and Product Owner ordering decision.
- **Compatibility:** retain single-lesson/global APIs and legacy records.
- **Failure:** reject before session mutation; active session remains unchanged.
- **Automated acceptance:** scope/order, restart, completion, sibling isolation, rollback.
- **Manual UAT:** select several lessons and confirm exact resumed sequence.
- **Release impact:** schema review may be required only if content scope is insufficient.
- **Order:** 2.

## DP-03 — Listening session preset (L / High / Decision)

- **Learner problem:** listening practice requires repeated manual configuration.
- **Journey:** choose Listening → preview playback behavior → start same learning session.
- **Evidence:** MODE-001, AUTO-002; current generic Study mode.
- **Scope:** typed preset with explicit defaults; projection chooses content emphasis and media
  plan while engine remains authority.
- **Excluded:** auto-rating and scheduler changes.
- **Owner:** Desktop preference/projection; application only if selection differs.
- **Dependency:** product decision on vocabulary/listening semantics.
- **Compatibility:** old config/session defaults to current Study.
- **Failure:** unavailable audio falls back to visible content and manual review.
- **Automated acceptance:** preset projection, recovery default, no scheduler/queue drift.
- **Manual UAT:** switch presets and finish/resume a session.
- **Release impact:** opt-in.
- **Order:** 3.

## DP-09 — Semantic audio roles (M / Medium / Decision)

- **Learner problem:** generic audio cannot express prompt, translation, example, or voice.
- **Journey:** content declares roles; learner selects available voice/channel.
- **Evidence:** AUDIO-002/003; current generic audio reference.
- **Scope:** backward-compatible optional role projection and UI labels.
- **Excluded:** timed automation.
- **Owner:** content/application model + package mapping + Desktop.
- **Dependency:** role vocabulary decision.
- **Compatibility:** unlabelled audio remains generic.
- **Failure:** unknown roles degrade to generic audio.
- **Automated acceptance:** legacy package, round-trip, missing/unsafe asset, renderer semantics.
- **Manual UAT:** bilingual/voice package presents correct labels.
- **Release impact:** package extension, no forced migration.
- **Order:** 4.

## DP-10 — Deterministic media session (L / High)

- **Learner problem:** repeated listening lacks reliable loop, delay, cancellation, and feedback.
- **Journey:** play/stop → choose loop/delay → transition/pause stops safely.
- **Evidence:** AUDIO-001/004; current view-local manual player.
- **Scope:** injected media coordinator, fake clock/player, one active playback identity, loop and
  delay preference, lifecycle cancellation.
- **Excluded:** auto reveal/rating and global media service.
- **Owner:** Desktop coordinator + platform adapter.
- **Dependency:** DP-09.
- **Compatibility:** current manual action remains default.
- **Failure:** player/asset errors expose retry and never mutate learning state.
- **Automated acceptance:** stale callback, stop, loop timing, transition, pause, completion.
- **Manual UAT:** audio on real devices and package media.
- **Release impact:** platform/audio regression risk.
- **Order:** 5.

## DP-11 — Interruptible listening flow (XL / High / Decision)

- **Learner problem:** learners want hands-light listening without losing control.
- **Journey:** configure sequence → start → see current step → pause/skip/repeat → manually rate.
- **Evidence:** AUTO-001/002/003; video demonstrates auto and lesson loop.
- **Scope:** deterministic presentation-only playback plan over current item/session, explicit
  cancellation, visible next action.
- **Excluded:** auto-rating, background lock-screen service, hidden autoplay.
- **Owner:** application-facing Desktop orchestration.
- **Dependency:** DP-03, DP-09, DP-10; reveal policy decision.
- **Compatibility:** disabled by default.
- **Failure:** interruption resumes authoritative session, not a timer position.
- **Automated acceptance:** fake-clock sequence, interruption/restart, stale callback, completion.
- **Manual UAT:** extended listening with pause/resume and media failures.
- **Release impact:** opt-in beta.
- **Order:** 6.

## DP-08 — Optional typed-recall exercise (XL / High / Decision)

- **Learner problem:** recognition/reveal alone is insufficient for production practice.
- **Journey:** opt into typing → answer → receive deterministic comparison → reveal/rate.
- **Evidence:** TYPING-001/002/003; dedicated Android layout and comparison methods.
- **Scope:** exercise contract, evaluator policy, transient response, accessible feedback.
- **Excluded:** forced rating gates and broad language NLP.
- **Owner:** domain evaluation + application flow + Desktop input.
- **Dependency:** tolerance/locale and activation decisions.
- **Compatibility:** content without exercise metadata unchanged.
- **Failure:** evaluator failure permits reveal; no partial review.
- **Automated acceptance:** Unicode, punctuation, blank/hint/reveal, keyboard, restart.
- **Manual UAT:** English/Vietnamese representative answers.
- **Release impact:** experimental opt-in.
- **Order:** 7.

## DP-07 — Learner-visible session limits (M / Medium)

- **Learner problem:** existing limits are not understandable/configurable at session setup.
- **Journey:** see new/review budget → adjust within safe bounds → start.
- **Evidence:** QUEUE-001; `SessionPolicy` already owns limits.
- **Scope:** validated typed preferences and setup summary.
- **Excluded:** replacing queue strategies or Android counters.
- **Owner:** application policy factory + typed config + Desktop.
- **Dependency:** none after 1.0.
- **Compatibility:** established defaults preserved.
- **Failure:** invalid config is not overwritten and defaults safely.
- **Automated acceptance:** boundaries, config corruption, planning, localization.
- **Manual UAT:** confirm plan/progress match selected limits.
- **Release impact:** low.
- **Order:** 8.

## DP-14/15 — Calm session goals and feedback (L / Medium / Decision)

- **Learner problem:** progress is correct but lacks a learner-chosen daily intention.
- **Journey:** set informational goal → see calm progress → complete or stop without penalty.
- **Evidence:** PROGRESS-001/STATS-002; current progress/dashboard queries.
- **Scope:** typed goal preference and projection from review history.
- **Excluded:** streak loss, rewards economy, notifications, hidden engagement metrics.
- **Owner:** application query + Desktop.
- **Dependency:** ethical goal semantics decision.
- **Compatibility:** absent goal shows current progress.
- **Failure:** corrupt preference preserves file and hides goal.
- **Automated acceptance:** day boundaries, restart, timezone, no duplicate count.
- **Manual UAT:** language/tone and accessibility review.
- **Release impact:** optional.
- **Order:** 9.

## DP-05/06/12/13/16 — Later product options

Favorites, in-study quick search, configurable gestures, scratch notes, and expanded display
preferences remain separate capabilities. They must not be bundled into listening/typing work.
Favorites and durable notes require data/compatibility decisions; gestures require
discoverability and accessibility parity. Quick search can reuse existing query boundaries.

## Recommended next capability

After external Desktop 1.0 gates, implement **DP-01 — Hierarchical learning scope**. It is
read-only, improves a demonstrated large-package learner problem, exercises the verified
performance boundary, and creates the navigation foundation for the higher-risk multi-lesson
and listening outcomes without changing scheduler or persistence.
