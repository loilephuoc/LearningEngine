# LP-001: Library Domain — Technical Debt Alignment

## Relevant Technical Debt Items

1. **`TD-02`: Vocabulary Alignment (`ContentCollection` vs `Collection`)**:
   - LP-001 specifies `Collection` as the canonical Aggregate Root name.
   - *Resolution Strategy*: When implementing LP-001 domain classes, introduce `Collection` in `vn.loi.learning.domain.library.model`, maintaining `ContentCollection` as a type alias if needed for backward compatibility until full Phase 3 expansion.
2. **`TD-01`: Application Port Isolation**:
   - LP-001 defines clean repository ports (`LibraryRepository`, `InstalledPackageRepository`, `CollectionRepository`) in `vn.loi.learning.domain.library.repository`. Use case orchestrators MUST depend exclusively on these interfaces.
