# HEAD

d13708b

---

## Completed

- MemoryState exposes Difficulty Value Object.
- MemoryState exposes Stability Value Object.
- BUILD SUCCESSFUL.
- Pushed to develop.

---

## Current Work

Primitive → Value Object migration.

---

## Next Batch

Replace production usages of

- difficulty
- stabilityDays

with

- difficultyValue
- stability

---

## Constraints

- Do not change persistence.
- Keep public constructor compatible.
- Every batch must build.