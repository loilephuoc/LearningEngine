# LearningEngine Agent Guide

## Branch

- Develop only
- Never work directly on main

---

## Workflow

Code

↓

BUILD SUCCESSFUL

↓

Commit

↓

Push develop

---

## Delivery

- Full replacement file only.
- No patch.
- Keep every batch buildable.

---

## Project

- Kotlin
- Gradle
- DDD
- FSRS

---

## Rules

- Prefer Value Objects.
- Preserve compatibility during migrations.
- Small buildable batches.
- Stop only when a design decision requires user input.