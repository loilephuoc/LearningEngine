package vn.loi.learning.android.family

internal object FamilyLegacyImportDuplicateDetector {
    fun attach(candidate: LegacyImportCandidate, snapshot: FamilyLocalSnapshot): LegacyImportCandidate {
        val incomingName = candidate.fullName?.let(FamilyLegacyImportNormalizer::normalizeName)
        val activePeople = snapshot.persons.filter { it.deletedAtEpochMillis == null }
        val activeFields = snapshot.personContactFields.filter { it.deletedAtEpochMillis == null }.groupBy { it.personId }
        val matches = activePeople.mapNotNull { person ->
            val existingFields = activeFields[person.id].orEmpty()
            val reasons = mutableListOf<String>()
            var strength = 0
            val sameName = incomingName != null && FamilyLegacyImportNormalizer.normalizeName(person.fullName) == incomingName
            if (sameName && candidate.birthDateSolar != null && person.birthDateSolar == candidate.birthDateSolar) {
                reasons += "Trùng họ tên + ngày sinh"; strength = maxOf(strength, 3)
            }
            val incomingCccd = candidate.readyFields.firstOrNull { it.label == "Số CCCD" }?.value
            val existingCccd = existingFields.firstOrNull { it.label.equals("Số CCCD", true) }?.value
            if (incomingCccd != null && existingCccd != null && incomingCccd == existingCccd) {
                reasons += "Trùng CCCD"; strength = 4
            }
            val existingPhones = existingFields.filter { it.type == PersonContactFieldType.PHONE }
                .map { FamilyLegacyImportNormalizer.normalizePhoneForComparison(it.value) }.toSet()
            if (candidate.readyFields.any { it.type == PersonContactFieldType.PHONE && it.normalizedComparison in existingPhones }) {
                reasons += "Trùng số điện thoại"; strength = maxOf(strength, 2)
            }
            val existingEmails = existingFields.filter { it.type == PersonContactFieldType.EMAIL }
                .map { FamilyLegacyImportNormalizer.normalizeEmail(it.value) }.toSet()
            if (candidate.readyFields.any { it.type == PersonContactFieldType.EMAIL && it.normalizedComparison in existingEmails }) {
                reasons += "Trùng email"; strength = maxOf(strength, 2)
            }
            if (reasons.isEmpty()) null else Triple(person, strength, reasons.toList())
        }.sortedWith(compareByDescending<Triple<Person, Int, List<String>>> { it.second }.thenBy { it.first.id })

        val best = matches.firstOrNull() ?: return candidate
        val existing = best.first
        val conflicts = classify(candidate.fields, activeFields[existing.id].orEmpty())
        val duplicateStatus = if (best.second >= 3) LegacyDuplicateStatus.STRONG_DUPLICATE else LegacyDuplicateStatus.POSSIBLE_DUPLICATE
        return candidate.copy(
            duplicate = LegacyDuplicateMatch(existing.id, existing.fullName, duplicateStatus, best.third, conflicts),
            status = duplicateStatus
        )
    }

    fun classify(incoming: List<LegacyImportField>, existing: List<PersonContactField>): List<LegacyFieldConflict> = incoming.map { field ->
        val sameLabel = existing.filter { it.type == field.type && it.label.orEmpty().equals(field.label, true) }
        val type = when (field.disposition) {
            LegacyImportFieldDisposition.BLOCKED -> LegacyConflictType.SENSITIVE_BLOCKED
            LegacyImportFieldDisposition.IGNORED -> LegacyConflictType.EMPTY_INCOMING
            LegacyImportFieldDisposition.NEEDS_REVIEW -> LegacyConflictType.CONFLICT
            LegacyImportFieldDisposition.READY -> {
                when {
                    sameLabel.any { valuesEqual(field, it) } -> LegacyConflictType.SAME
                    sameLabel.isEmpty() -> LegacyConflictType.NEW_VALUE
                    else -> LegacyConflictType.CONFLICT
                }
            }
        }
        LegacyFieldConflict(field, type, sameLabel.firstOrNull()?.value)
    }

    private fun valuesEqual(incoming: LegacyImportField, existing: PersonContactField): Boolean = when (incoming.type) {
        PersonContactFieldType.PHONE -> incoming.normalizedComparison == FamilyLegacyImportNormalizer.normalizePhoneForComparison(existing.value)
        PersonContactFieldType.EMAIL -> incoming.normalizedComparison == FamilyLegacyImportNormalizer.normalizeEmail(existing.value)
        else -> incoming.value?.trim() == existing.value.trim()
    }
}

internal object FamilyLegacyImportPreviewService {
    fun preview(csvText: String, currentSnapshot: FamilyLocalSnapshot): LegacyImportPreview {
        val parsed = FamilyLegacyCsvParser.parse(csvText)
        val candidates = parsed.rows.map { row ->
            FamilyLegacyImportDuplicateDetector.attach(FamilyLegacyImportMapper.map(row), currentSnapshot)
        }
        return LegacyImportPreview(parsed.rows.size, candidates, parsed.issues)
    }
}
