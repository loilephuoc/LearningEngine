# Desktop Recall Pipeline

Desktop is a renderer and interaction adapter for recall. Typing is the first integrated mode.

```text
Study session -> LearningEngine.createRecallPlan -> RecallPlan
RecallPlan -> Desktop rendering/input -> RecallSubmission
RecallSubmission -> LearningEngine.executeRecall -> RecallResult
RecallResult -> LearningEngine.executeRecallLearning -> existing review transaction
```

The Desktop layer owns rendering, input lifecycle, animation, accessibility, navigation, and
media playback. Shared Core owns the canonical answer contract, normalization, correctness,
evidence eligibility, and learning-state commit. The existing typing timing presentation remains
an input to the established automatic-rating compatibility policy; it does not bypass the recall
execution or learning bridge.

Only Typing is wired in LQ-007A. Other recall modes continue through their existing Desktop paths
until a later capability supplies their renderer/submission adapters.
