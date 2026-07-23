# Package Platform Blueprint

## Purpose
The **Package Platform** subsystem provides the platform-neutral foundation for building, validating, inspecting, verifying, and exporting OPD3 content packages (`.opd3`).

## Responsibilities
- Convert platform-neutral topic structures into canonical `CanonicalTopicPackage` models.
- Collect, deduplicate, and calculate SHA-256 checksums for media assets via `PackageMediaAssetCollector`.
- Export 100% byte-for-byte deterministic `.opd3` ZIP archives via `Opd3PackageExporter` and `DeterministicZipWriter`.
- Perform streaming inspection of archive contents, sizes, and diagnostics via `Opd3PackageInspector`.
- Perform cryptographic verification of package checksums, schema versions, and media cross-references via `Opd3PackageVerifier`.
- Enforce path safety boundaries via canonical `Opd3PathValidator`.

## Out of Scope
- Local database storage of installed packages (handled by Library Platform).
- Learner study session execution or SRS scheduling (handled by Learning Session Platform).

## Dependencies
- Pure Kotlin/JVM core build module.
- Standard `java.util.zip` compression tools.
- `kotlinx.serialization` JSON parser.

---

## Subsystem Architecture & Components

```text
+-----------------------------------------------------------------------------------+
|                                PACKAGE PLATFORM                                   |
|                                                                                   |
|  +------------------------------+             +--------------------------------+  |
|  |    PackageMediaAssetCollector|============>|      CanonicalMediaBundle      |  |
|  |    (Media Asset Collection)  |             |      (Deduplicated Media)     |  |
|  +--------------+---------------+             +---------------+----------------+  |
|                 |                                             |                   |
|                 v                                             v                   |
|  +------------------------------+             +--------------------------------+  |
|  |     Opd3PackageExporter      |============>|     DeterministicZipWriter     |  |
|  |     (OPD3 Package Exporter)  |             |     (Byte-Deterministic ZIP)   |  |
|  +--------------+---------------+             +--------------------------------+  |
|                 |                                                                 |
|                 v                                                                 |
|  +------------------------------+             +--------------------------------+  |
|  |     Opd3PackageInspector     |<===========>|     Opd3PathValidator          |  |
|  |     (Streaming Inspection)   |             |     (Canonical Path Security)  |  |
|  +--------------+---------------+             +--------------------------------+  |
|                 |                                                                 |
|                 v                                                                 |
|  +------------------------------+                                                 |
|  |     Opd3PackageVerifier      |                                                 |
|  |     (Integrity Verification) |                                                 |
|  +------------------------------+                                                 |
+-----------------------------------------------------------------------------------+
```

---

## Data Models & Schema Contracts

### OPD3 Package Layout Specification (Schema v1.0)
An `.opd3` package is a ZIP archive containing:
- `metadata.json`: Package ID, topic ID, logical topic name, schema version (`"1.0"`), format (`"OPD3"`), version (`"1"`), and tags.
- `contents.json`: List of educational content items (`ContentId`, `ContentType`, `ContentText`).
- `learning-items.json`: List of testable learning items (`LearningItemId`, `ContentId`, `LearningMode`).
- `media-manifest.json`: List of media assets (`logicalPath`, `mediaType`, `size`, `sha256`, `owningContentIds`).
- `media/*`: Raw media byte assets (audio MP3, image PNG/JPG).
- `manifest.json`: Map of all package files to their SHA-256 hex checksums.

### Deterministic Export Guarantee
Exporting a package produces identical byte outputs across executions:
- Fixed ZipEntry timestamp (`1577836800000L` / 2020-01-01T00:00:00Z UTC).
- Sorted keys by `ContentId` and `LearningItemId`.
- Standard UTF-8 JSON formatting.

---

## Security & Safety Limits

Package Platform enforces `PackageSafetyLimits`:
- `MAX_ENTRY_COUNT = 10_000` entries.
- `MAX_SINGLE_ENTRY_SIZE_BYTES = 500 MB`.
- `MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES = 2 GB`.
- `Opd3PathValidator`: Rejects `..`, `.`, empty segments, double slashes `//`, drive letters, and entries outside `metadata.json`, `contents.json`, `learning-items.json`, `media-manifest.json`, `manifest.json`, or `media/*`.

---

## Future Evolution
- Support for streaming package extraction during installation in Library Platform.
- Support for differential package delta updates.

---

## Architecture Notes
- All package platform capabilities are tested via `Opd3AdversarialTest.kt` (22+ adversarial test scenarios).
