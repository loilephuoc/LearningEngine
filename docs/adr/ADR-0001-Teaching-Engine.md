# ADR-0001: Building an Adaptive Teaching Engine Instead of a Flashcard App

- **Status**: Accepted
- **Date**: 2026-07-23
- **Deciders**: Architecture Team, Product Architect

## Context

Traditional spaced-repetition software (such as Anki) functions primarily as a passive flashcard container and scheduling calculator. These applications require learners to manually manage deck settings, interval multipliers, learning steps, and presentation modes. This administrative overhead consumes significant cognitive energy and creates friction for non-technical learners.

## Decision

We decide that **Learning Engine 2.0 is an adaptive Teaching Engine, NOT a flashcard application or Anki clone.**

- The learner's role is restricted to initiating study (**"Start Learning"**) and engaging with presented Learning Scenes.
- The engine autonomously determines what to teach, how to teach, when to teach, and how to structure presentation.
- Flashcards (retrieval practice) are treated as one specific Learning Scene type within a broader pedagogical repertoire.

## Consequences

### Positive
- Minimizes cognitive overload for the learner.
- Delivers a unified, friction-free learner experience.
- Enables rich, multi-modal teaching flows (listening, prompt, typing, synthesis) impossible in static flashcard apps.

### Negative / Trade-offs
- Power users who desire low-level manual algorithm configuration cannot customize scheduling variables.
- Requires a sophisticated Product Brain layer in the application architecture.
