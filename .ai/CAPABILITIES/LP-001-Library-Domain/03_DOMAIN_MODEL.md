# LP-001: Library Domain — Domain Model

## High-Level Domain Model Diagram

```text
+-----------------------------------------------------------------------------------+
|                            LIBRARY PLATFORM DOMAIN                                |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                             Library Aggregate Root                          |  |
|  |  LibraryId (VO)                                                             |  |
|  |  Name: String                                                               |  |
|  |  Entries: List<LibraryEntry>                                                |  |
|  +-------------------------------------+---------------------------------------+  |
|                                        |                                          |
|                                        | (Owns & References)                      |
|                                        v                                          |
|  +-------------------------------------+---------------------------------------+  |
|  |                        InstalledPackage Aggregate Root                      |  |
|  |  InstalledPackageId (VO)                                                    |  |
|  |  PackageId (VO)                                                             |  |
|  |  TopicId (VO)                                                               |  |
|  |  Name: PackageName (VO)                                                     |  |
|  |  Version: PackageVersion (VO)                                               |  |
|  |  State: PackageState (ACTIVE | ARCHIVED | REMOVED)                           |  |
|  |  InstalledAt: Instant                                                       |  |
|  |  ContentCount: Int                                                          |  |
|  |  LearningItemCount: Int                                                     |  |
|  +-------------------------------------+---------------------------------------+  |
|                                        ^                                          |
|                                        | (Assigned To)                            |
|                                        |                                          |
|  +-------------------------------------+---------------------------------------+  |
|  |                            Collection Aggregate Root                        |  |
|  |  CollectionId (VO)                                                          |  |
|  |  LibraryId (VO)                                                             |  |
|  |  Name: CollectionName (VO)                                                  |  |
|  |  Description: String                                                        |  |
|  |  AssignedPackageIds: Set<InstalledPackageId>                                |  |
|  +-----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

## Bounded Context Position
The Library Domain context receives canonical packages exported by Package Platform and registers them as `InstalledPackage` aggregates. It provides topic cataloging and collection groupings to downstream consumers (Workspace Platform and Learning Sessions).
