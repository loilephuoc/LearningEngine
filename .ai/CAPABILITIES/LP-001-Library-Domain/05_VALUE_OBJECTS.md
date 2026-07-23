# LP-001: Library Domain — Value Objects Specification

## 1. `LibraryId`
- **Meaning**: Strongly-typed identity value object for a `Library` aggregate root.
- **Validation Rules**: Must not be blank, must contain only alphanumeric characters, hyphens, or underscores. Length between 3 and 64 characters.
- **Equality Rules**: Structural equality based on string `value`.
- **Examples**: `LibraryId("lib-main")`, `LibraryId("lib-user-101")`
- **Invalid Examples**: `LibraryId("")`, `LibraryId("   ")`, `LibraryId("lib/invalid")`

## 2. `InstalledPackageId`
- **Meaning**: Unique identity value object for an `InstalledPackage` instance.
- **Validation Rules**: Must not be blank, valid UUID or deterministic string identifier.
- **Equality Rules**: Structural equality.
- **Examples**: `InstalledPackageId("inst-pkg-001")`, `InstalledPackageId("c8f921a4-8b12-4211-9a10-123456789abc")`
- **Invalid Examples**: `InstalledPackageId("")`, `InstalledPackageId("   ")`

## 3. `CollectionId`
- **Meaning**: Unique identity value object for a `Collection` aggregate root.
- **Validation Rules**: Must not be blank.
- **Equality Rules**: Structural equality.
- **Examples**: `CollectionId("col-jlpt-n3")`, `CollectionId("col-grammar-2026")`
- **Invalid Examples**: `CollectionId("")`

## 4. `PackageId`
- **Meaning**: Identity value object representing the content package template ID across version iterations.
- **Validation Rules**: Must not be blank.
- **Equality Rules**: Structural equality.
- **Examples**: `PackageId("pkg-english-n1")`, `PackageId("pkg-kanji-basic")`
- **Invalid Examples**: `PackageId("")`

## 5. `TopicId`
- **Meaning**: Identity value object for a learning topic.
- **Validation Rules**: Must not be blank.
- **Equality Rules**: Structural equality.
- **Examples**: `TopicId("topic-n5-kanji")`, `TopicId("t-1001")`
- **Invalid Examples**: `TopicId("")`

## 6. `PackageVersion`
- **Meaning**: Value object representing package version semver or integer release string.
- **Validation Rules**: Must not be blank, must match valid version format (e.g., `"1.0"`, `"1"`, `"2.1.0"`).
- **Equality Rules**: Structural equality.
- **Examples**: `PackageVersion("1.0")`, `PackageVersion("2.1.0")`
- **Invalid Examples**: `PackageVersion("")`, `PackageVersion("invalid version")`

## 7. `CollectionName`
- **Meaning**: Human-readable title value object for a `Collection`.
- **Validation Rules**: Must not be blank, trimmed length between 1 and 100 characters.
- **Equality Rules**: Case-insensitive structural equality.
- **Examples**: `CollectionName("JLPT N3 Vocabulary")`, `CollectionName("Medical Terms")`
- **Invalid Examples**: `CollectionName("")`, `CollectionName("   ")`

## 8. `PackageName`
- **Meaning**: Human-readable title value object for an `InstalledPackage`.
- **Validation Rules**: Must not be blank, trimmed length between 1 and 200 characters.
- **Equality Rules**: Structural equality.
- **Examples**: `PackageName("Basic Japanese Kanji")`
- **Invalid Examples**: `PackageName("")`
