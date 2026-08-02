# Learning Insight & Learner Transparency Contract

`LearningInsight` is the cross-platform learner-transparency projection for Learning Engine. Shared
Application code composes the existing promotion decision, difficulty profile, and adaptive
recommendation authorities. Desktop and future clients render that typed result; they do not infer
learning policy from difficulty, trend, promotion reasons, or raw review events.

## Authority and inputs

- `LearningTrajectory` remains the learner-and-Content evidence source.
- `LearningDifficultyProfileCalculator` remains the difficulty authority.
- `AdaptiveLearningStrategy` remains the recommendation authority.
- `EvidencePromotionAuthority` remains the promotion authority; the insight layer consumes an
  already calculated `PromotionDecision` and never recalculates eligibility.
- `GetLearningInsightUseCase` reads the trajectory repository and performs no mutation.

The projection is identified by `LearnerId` and `ContentId`. Sibling learning items for the same
Content therefore share one source. Its generated time comes from an injected clock.

## Projection contract

An insight has typed category, severity, title, summary, difficulty/trend/confidence,
recommendation details, promotion details, supporting typed metrics, and `generatedAt`. The primary
ordering is deterministic: recovery/regression; blocked promotion; very difficult/high risk; focus
practice; positive promotion readiness; stable/mastered; insufficient data. Secondary insights are
deduplicated and bounded by `LearningInsightPresentationPolicy`.

Promotion reasons, remaining duration, and missing recall count stay typed. Practice results are
explicitly described as non-evaluative and never as promotion evidence. A manual rating is described
as recorded user input, never as automatic recall evidence. Low-confidence projections use
insufficient-data semantics rather than overstated guidance.

## Client presentation

Desktop renders the shared projection only after answer reveal, while practice-only mode may show
its non-evaluative transparency notice. The latest result is retained in the session-completion
summary. Localization and metric formatting are centralized in the presentation mapper. The card
uses text as well as visual emphasis, exposes one ordered accessibility description, bounds compact
metrics, and expands locally without changing learning state.

This capability performs no AI or network call and does not change scheduling, rating, promotion,
difficulty, recommendation, persistence, or undo semantics.
