# LP-001: Library Domain — Out of Scope

The following capabilities are explicitly **OUT OF SCOPE** for LP-001 and deferred to subsequent milestone capabilities:

1. **Infrastructure Persistence Implementations**:
   - JSON file storage adapters (`JsonLibraryRepository`), Room SQLite database schemas, and disk IO. (Deferred to Infrastructure capabilities).
2. **OPD3 File Parsing & Unpacking**:
   - Reading ZIP archives, uncompressing media files, or SHA-256 integrity verification. (Owned by Package Platform).
3. **Presentation & Desktop UI**:
   - Compose Desktop screens, Library composables, ViewModels, or Desktop window wiring. (Deferred to Presentation capabilities).
4. **Workspace & Study Sessions**:
   - Binding library topics to active learner workspaces, card queue planning, FSRS rating execution, or review history logging. (Owned by Workspace & Learning Session Platforms).
5. **Multi-Device Sync & Marketplace**:
   - Cloud synchronization, change-data-capture logs, peer-to-peer sharing, or marketplace discovery networking.
