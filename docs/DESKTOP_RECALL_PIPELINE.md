# Desktop Recall Pipeline

Desktop is a renderer and interaction adapter for recall. Typing and Multiple Choice are integrated.

```text
Study session -> ProductionRecallPlanResolver -> capability + adaptive strategy
               -> ordered RecallPlanFactory attempts -> resolved RecallPlan
RecallPlan -> Desktop rendering/input -> RecallSubmission
RecallSubmission -> LearningEngine.executeRecall -> RecallResult
RecallResult -> LearningEngine.executeRecallLearning -> existing review transaction
```

The Desktop layer owns rendering, input lifecycle, animation, accessibility, navigation, and
media playback. Shared Core owns the canonical answer contract, normalization, correctness,
evidence eligibility, and learning-state commit. The existing typing timing presentation remains
an input to the established automatic-rating compatibility policy; it does not bypass the recall
execution or learning bridge.

LQ-007C routes `RecallPlan.mode` to a dedicated Multiple Choice renderer, preserves the plan's two
to four option identities and order, and submits only the selected option ID. Click and number-key
input share one attempt gate. Unsupported modes receive an explicit Desktop fallback and never
silently render as Typing. Listening, Image Recall, Reverse Translation, Example Completion, and
Dictation remain unwired.

LQ-007C.1 removes the production Typing decision previously fabricated by `StudyFacade`. Shared
Application now supplies strategy-selected mode authority and deterministic MCQ inventory;
unavailable MCQ attempts follow the strategy's ordered typed candidates with retained failure
provenance. Desktop adds no mode toggle, randomizer, eligibility rule, or silent fallback, and
reuses the resolved plan for the same session/item attempt across recomposition.
