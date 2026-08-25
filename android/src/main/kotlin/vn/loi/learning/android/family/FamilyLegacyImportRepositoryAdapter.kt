package vn.loi.learning.android.family

import java.util.UUID

/**
 * V3.2-C repository integration layer.
 *
 * Builds the complete next FAMILY snapshot in memory first.
 *
 * Persistent mutation is performed only after the complete import has been
 * prepared successfully.
 *
 * When the runtime repository is SyncAwareFamilyRepository, the adapter uses
 * the dedicated batch-import boundary so all imported Person and
 * PersonContactField mutations are queued for sync and sync is triggered
 * exactly once for the batch.
 *
 * No Supabase dependency lives here.
 */
internal object FamilyLegacyImportRepositoryAdapter {

    internal data class PreparedImport(
        val snapshot: FamilyLocalSnapshot,
        val createdPersons: List<Person>,
        val createdFields: List<PersonContactField>
    ) {
        val peopleCreated: Int
            get() = createdPersons.size

        val fieldsCreated: Int
            get() = createdFields.size
    }

    /**
     * Pure preparation.
     *
     * Does not mutate [current].
     * Does not call FamilyRepository.
     */
    fun prepare(
        current: FamilyLocalSnapshot,
        plan: LegacyImportPlan,
        nowEpochMillis: Long = System.currentTimeMillis(),
        idFactory: () -> String = { UUID.randomUUID().toString() }
    ): PreparedImport {
        if (plan.entries.isEmpty()) {
            return PreparedImport(
                snapshot = current,
                createdPersons = emptyList(),
                createdFields = emptyList()
            )
        }

        val newPersons = mutableListOf<Person>()
        val newFields = mutableListOf<PersonContactField>()

        plan.entries.forEach { entry ->
            val write = FamilyLegacyImportExecutor.prepareWrite(entry)
            val personId = idFactory()

            val person = Person(
                id = personId,
                fullName = write.person.fullName,
                group = PersonGroup.FAMILY,
                birthDateSolar = write.person.birthDateSolar,
                note = write.person.note,
                createdAtEpochMillis = nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis
            )

            val fields = write.fields.map { field ->
                PersonContactField(
                    id = idFactory(),
                    personId = personId,
                    type = field.type,
                    label = field.label,
                    value = field.value,
                    isPrimary = field.isPrimary,
                    sortOrder = field.sortOrder,
                    createdAtEpochMillis = nowEpochMillis,
                    updatedAtEpochMillis = nowEpochMillis
                )
            }

            newPersons += person
            newFields += fields
        }

        require(
            newPersons.map { it.id }.distinct().size == newPersons.size
        ) {
            "Duplicate generated Person IDs"
        }

        require(
            newFields.map { it.id }.distinct().size == newFields.size
        ) {
            "Duplicate generated PersonContactField IDs"
        }

        val existingPersonIds =
            current.persons.mapTo(hashSetOf()) { it.id }

        val existingFieldIds =
            current.personContactFields.mapTo(hashSetOf()) { it.id }

        require(
            newPersons.none { it.id in existingPersonIds }
        ) {
            "Generated Person ID collides with existing data"
        }

        require(
            newFields.none { it.id in existingFieldIds }
        ) {
            "Generated PersonContactField ID collides with existing data"
        }

        val createdPersonIds =
            newPersons.mapTo(hashSetOf()) { it.id }

        require(
            newFields.all { it.personId in createdPersonIds }
        ) {
            "Imported contact field references unknown imported person"
        }

        val immutablePersons = newPersons.toList()
        val immutableFields = newFields.toList()

        return PreparedImport(
            snapshot = current.copy(
                persons = current.persons + immutablePersons,
                personContactFields =
                    current.personContactFields + immutableFields
            ),
            createdPersons = immutablePersons,
            createdFields = immutableFields
        )
    }

    /**
     * Persistent boundary.
     *
     * The complete snapshot is prepared before any repository mutation.
     *
     * SyncAwareFamilyRepository receives the exact imported entities so it can
     * persist the snapshot, queue PERSON/PERSON_CONTACT_FIELD mutations and
     * trigger sync once for the complete batch.
     *
     * Plain FamilyRepository keeps the existing single replaceSnapshot()
     * behavior. This is intentionally retained for tests and non-sync-aware
     * repository implementations.
     */
    suspend fun commit(
        repository: FamilyRepository,
        plan: LegacyImportPlan,
        nowEpochMillis: Long = System.currentTimeMillis(),
        idFactory: () -> String = { UUID.randomUUID().toString() }
    ): PreparedImport {
        val current = repository.snapshot.value

        val prepared = prepare(
            current = current,
            plan = plan,
            nowEpochMillis = nowEpochMillis,
            idFactory = idFactory
        )

        if (prepared.peopleCreated == 0) {
            return prepared
        }

        if (repository is SyncAwareFamilyRepository) {
            repository.replaceSnapshotAndQueueImportedPeople(
                snapshot = prepared.snapshot,
                importedPersons = prepared.createdPersons,
                importedFields = prepared.createdFields
            )
        } else {
            repository.replaceSnapshot(prepared.snapshot)
        }

        return prepared
    }
}