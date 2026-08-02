# Question Transition Pipeline Contract

Application review and practice use cases remain the sole queue-advance authority. Desktop never
advances a queue. After a successful operation selects the destination exactly once, ViewModel
holds that committed destination as a non-visible pending state and keeps the source item as the
published presentation through `RESULT_SHOWN` and `EXITING_CURRENT`.

The typed lifecycle is linear and token-safe:

1. `RESULT_SHOWN`: the source item remains mounted under the result overlay.
2. `EXITING_CURRENT`: the source content exits; the pending item is still not published.
3. `ENTERING_NEXT`: ViewModel atomically publishes the pending destination once.
4. Completion of entry clears transient state and returns to ordinary presentation.

One in-progress guard spans the complete lifecycle, preventing click/keyboard races and duplicate
transactions. A competing rating cannot settle or advance the pipeline. A legitimate non-rating
lifecycle action may settle a pending destination before continuing. Final session completion has
no next item to swap and is published immediately.

Destination content, image loading, per-item typing/reveal state, timer keys, focus keys, and audio
binding do not receive the next item identity before `ENTERING_NEXT`. The former child item-arrival
animation keyed independently by item ID is removed; the transition host is the single visible
entry animation authority. Focus and bring-into-view occur only after the destination field is
mounted and enabled. Integrated Desktop no-flash UAT remains pending.
