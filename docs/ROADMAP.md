# Roadmap

This roadmap is intentionally conservative. Items are marked complete only when represented in the current source and tests. Future batch scope must be confirmed by reading the relevant source at the current `develop` commit.

## Completed through Batch 16

- Core learning and review domain
- End-to-end review capability
- Study-session selection and sibling avoidance
- JSON persistence foundations
- Content-package import, registration, query, and uninstall workflows
- Dashboard query and desktop visualization foundations
- Study-queue planning and persistence
- Queue strategies, balancing, diversity policies, diagnostics, validation, and metrics
- Content-library collections and package attachment workflows
- Desktop content-library dialogs
- Repository cleanup and Git baseline migration

## Batch 17 — documentation and workflow baseline

- Restore living documentation from the current Git baseline
- Record the module and capability map
- Establish Git-only source-of-truth rules
- Define the batch application contract
- Add a conservative roadmap for subsequent source-driven planning

## Next capability selection

The next functional batch is deliberately not named in this document yet. Before Batch 18 is prepared, the relevant source, tests, factories, and desktop wiring must be inspected on the new `develop` HEAD. The selected increment must:

1. Deliver one complete user-visible or engine capability.
2. Include production code, tests, and wiring together.
3. Preserve existing contracts unless migration is included.
4. Pass `clean test`.
5. Be packaged with SHA verification, backup, and rollback.
