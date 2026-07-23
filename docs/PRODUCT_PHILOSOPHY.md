# Product Philosophy — Learning Engine 2.0

## 1. Why Learning Engine Exists

Learning Engine exists to transform self-directed learning from a manual administration burden into an adaptive, guided teaching experience. 

Traditional learning applications force learners to become their own instructional designers. Learners must manage deck configurations, adjust card interval multipliers, manually toggle learning modes, construct complex review filters, and decide when to study which item. This administrative overhead drains cognitive energy away from actual acquisition and retention.

Learning Engine eliminates this burden. It is built on a fundamental premise: **the learner's job is to learn; the engine's job is to teach.**

---

## 2. Teaching Engine vs. Flashcard App

Learning Engine is **NOT** a flashcard application, and it is **NOT** an Anki clone. 

| Dimension | Flashcard App / Anki Clone | Learning Engine 2.0 (Teaching Engine) |
|---|---|---|
| **Core Nature** | Passive card container & scheduling calculator | Active, adaptive Teaching Engine |
| **Learner Role** | System administrator & manual mode selector | Active learner pressing "Start Learning" |
| **Presentation** | Static front/back card flip | Dynamic, multi-stage Learning Scenes |
| **Decision Ownership** | Learner configures algorithms & review steps | Product Brain orchestrates objectives, strategies & scenes |
| **Adaptability** | Fixed repetition intervals based solely on rating | Multi-dimensional adaptation (scene, difficulty, engagement) |
| **Pedagogy** | Isolated retrieval practice only | Guided teaching loop (context, prompt, evaluation, feedback) |

Flashcards are merely one possible **Learning Scene** among many (e.g., listening practice, prompt recall, typing recall, contextual reading, active problem solving). Learning Engine treats retrieval practice as an ingredient of pedagogy, not the entire product identity.

---

## 3. The Product North Star

> **Every design decision must measurably improve the learner's ability to learn, remember, and stay motivated.**

All feature proposals, UI changes, and architectural decisions are evaluated strictly against this North Star. Features that do not contribute to learning efficiency, long-term retention, or learner motivation are rejected regardless of their popularity in other software.

---

## 4. Product Values

1. **Guided over Configured**: The learner should only need to press **Start Learning**. The Product Brain handles objective selection, strategy selection, scene rotation, and difficulty scaling.
2. **Pedagogy over Feature Count**: We prioritize scientifically backed teaching efficacy over a long checklist of settings or customization options.
3. **Cognitive Energy Conservation**: Interface friction, manual mode selection, and setup decisions are minimized to conserve cognitive capacity for acquisition.
4. **Encouragement and Mastery**: The system balances optimal spacing difficulty with motivational momentum, encouraging learners when challenged and rewarding progress.
5. **Decoupled Architecture**: Pedagogical intelligence lives entirely in the platform-neutral core, ensuring identical learning quality across Desktop, Mobile, and Web clients.

---

## 5. Success Metrics

- **Long-Term Retention Rate**: Measurement of recalled knowledge over extended retention intervals.
- **Learner Engagement & Consistency**: Session completion rates and daily study consistency without burnout.
- **Time-to-Mastery Efficiency**: Reduction in the total learner effort required to achieve durable recall.
- **Frictionless Onboarding**: Zero-friction transition from initial session entry to active learning.
