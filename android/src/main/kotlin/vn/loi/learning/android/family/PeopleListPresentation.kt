package vn.loi.learning.android.family

import java.text.Collator
import java.text.Normalizer
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class PersonSortOption(val displayName: String) {
    NAME_AZ("Tên A–Z"),
    UPCOMING_BIRTHDAY("Sinh nhật gần nhất"),
    RECENTLY_UPDATED("Mới cập nhật")
}

object PeopleListPresentation {

    private val VIETNAMESE_COLLATOR: Collator = Collator.getInstance(Locale.forLanguageTag("vi-VN")).apply {
        strength = Collator.SECONDARY
    }

    fun initialsForName(fullName: String): String {
        val words = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return "?"
        if (words.size == 1) {
            val first = words[0].firstOrNull()?.uppercase() ?: return "?"
            return first
        }
        val first = words.first().firstOrNull()?.uppercase() ?: ""
        val last = words.last().firstOrNull()?.uppercase() ?: ""
        val res = "$first$last"
        return if (res.isBlank()) "?" else res
    }

    fun normalizeForSearch(text: String): String {
        if (text.isBlank()) return ""
        val replaced = text.replace('đ', 'd').replace('Đ', 'D')
        val normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    fun calculateNextBirthday(birthDateSolar: LocalDate, today: LocalDate): LocalDate {
        val birthMonth = birthDateSolar.monthValue
        val birthDay = if (birthMonth == 2 && birthDateSolar.dayOfMonth == 29 && !Year.isLeap(today.year.toLong())) {
            28
        } else {
            birthDateSolar.dayOfMonth
        }
        val candidate = LocalDate.of(today.year, birthMonth, birthDay)
        return if (!candidate.isBefore(today)) {
            candidate
        } else {
            val nextYear = today.year + 1
            val nextDay = if (birthMonth == 2 && birthDateSolar.dayOfMonth == 29 && !Year.isLeap(nextYear.toLong())) {
                28
            } else {
                birthDateSolar.dayOfMonth
            }
            LocalDate.of(nextYear, birthMonth, nextDay)
        }
    }

    fun daysUntilNextBirthday(birthDateSolar: LocalDate, today: LocalDate): Long {
        val nextBday = calculateNextBirthday(birthDateSolar, today)
        return ChronoUnit.DAYS.between(today, nextBday)
    }

    fun matchesPersonSearch(
        person: Person,
        activeContactFields: List<PersonContactField>,
        query: String
    ): Boolean {
        if (person.deletedAtEpochMillis != null) return false
        val normalizedQuery = normalizeForSearch(query)
        if (normalizedQuery.isBlank()) return true

        val targets = listOfNotNull(
            person.fullName,
            person.nickname,
            person.relationshipLabel,
            person.group.displayName(),
            person.phone,
            person.address,
            person.note
        )

        for (target in targets) {
            if (normalizeForSearch(target).contains(normalizedQuery)) return true
        }

        for (cf in activeContactFields) {
            if (cf.deletedAtEpochMillis != null) continue
            cf.label?.let {
                if (normalizeForSearch(it).contains(normalizedQuery)) return true
            }
            if (normalizeForSearch(cf.value).contains(normalizedQuery)) return true
        }

        return false
    }

    fun filterAndSortPeople(
        persons: List<Person>,
        contactFieldsByPersonId: Map<String, List<PersonContactField>>,
        searchQuery: String,
        selectedGroup: PersonGroup?,
        sortOption: PersonSortOption,
        today: LocalDate = LocalDate.now()
    ): List<Person> {
        val normalizedQuery = normalizeForSearch(searchQuery)

        val filtered = persons.filter { person ->
            if (person.deletedAtEpochMillis != null) return@filter false
            if (selectedGroup != null && person.group != selectedGroup) return@filter false
            if (normalizedQuery.isNotEmpty()) {
                val activeFields = contactFieldsByPersonId[person.id]
                    ?.filter { it.deletedAtEpochMillis == null }
                    .orEmpty()
                if (!matchesPersonSearch(person, activeFields, normalizedQuery)) {
                    return@filter false
                }
            }
            true
        }

        return when (sortOption) {
            PersonSortOption.NAME_AZ -> {
                filtered.sortedWith { p1, p2 ->
                    val cmp = VIETNAMESE_COLLATOR.compare(p1.fullName, p2.fullName)
                    if (cmp != 0) cmp else p1.id.compareTo(p2.id)
                }
            }
            PersonSortOption.UPCOMING_BIRTHDAY -> {
                filtered.sortedWith { p1, p2 ->
                    val b1 = p1.birthDateSolar
                    val b2 = p2.birthDateSolar
                    when {
                        b1 != null && b2 != null -> {
                            val d1 = daysUntilNextBirthday(b1, today)
                            val d2 = daysUntilNextBirthday(b2, today)
                            if (d1 != d2) d1.compareTo(d2)
                            else {
                                val nameCmp = VIETNAMESE_COLLATOR.compare(p1.fullName, p2.fullName)
                                if (nameCmp != 0) nameCmp else p1.id.compareTo(p2.id)
                            }
                        }
                        b1 != null && b2 == null -> -1
                        b1 == null && b2 != null -> 1
                        else -> {
                            val nameCmp = VIETNAMESE_COLLATOR.compare(p1.fullName, p2.fullName)
                            if (nameCmp != 0) nameCmp else p1.id.compareTo(p2.id)
                        }
                    }
                }
            }
            PersonSortOption.RECENTLY_UPDATED -> {
                filtered.sortedWith(compareByDescending<Person> { it.updatedAtEpochMillis }.thenBy { it.id })
            }
        }
    }
}
