package vn.loi.learning.android.family

import java.time.LocalDate
import kotlin.test.*
import org.junit.Test

class PeopleListPresentationTest {

    @Test
    fun `initials derivation handles various name formats correctly`() {
        assertEquals("LT", PeopleListPresentation.initialsForName("Lê Văn Tính"))
        assertEquals("NH", PeopleListPresentation.initialsForName("Nguyễn Thị Kim Huệ"))
        assertEquals("LT", PeopleListPresentation.initialsForName("Lê Tính"))
        assertEquals("M", PeopleListPresentation.initialsForName("Madonna"))
        assertEquals("LT", PeopleListPresentation.initialsForName("   Lê   Văn   Tính   "))
        assertEquals("?", PeopleListPresentation.initialsForName(""))
        assertEquals("?", PeopleListPresentation.initialsForName("   "))
    }

    @Test
    fun `normalizeForSearch strips diacritics and normalizes case and d-stroke`() {
        assertEquals("le van tinh", PeopleListPresentation.normalizeForSearch("Lê Văn Tính"))
        assertEquals("dong nghiep", PeopleListPresentation.normalizeForSearch("Đồng nghiệp"))
        assertEquals("nguyen", PeopleListPresentation.normalizeForSearch("NGUYỄN"))
        assertEquals("", PeopleListPresentation.normalizeForSearch("   "))
    }

    @Test
    fun `search matches full name and diacritic-insensitive query`() {
        val person = Person(
            id = "p1",
            fullName = "Lê Văn Tính",
            group = PersonGroup.FAMILY,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "Tính"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "tinh"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "Le Van"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "van"))
        assertFalse(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "Hoàng"))
    }

    @Test
    fun `search matches nickname and relationshipLabel and group displayName`() {
        val person = Person(
            id = "p2",
            fullName = "Nguyễn Văn A",
            nickname = "Tèo",
            relationshipLabel = "Anh họ",
            group = PersonGroup.FAMILY,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "teo"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "Tèo"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "anh ho"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "Gia đình"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, emptyList(), "gia dinh"))
    }

    @Test
    fun `search matches active contact fields label and value but ignores deleted contact fields`() {
        val person = Person(
            id = "p3",
            fullName = "Trần Thị B",
            group = PersonGroup.COLLEAGUE,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val activePhone = PersonContactField(
            id = "cf1",
            personId = "p3",
            type = PersonContactFieldType.PHONE,
            value = "0772123456",
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
        val activeCustomBank = PersonContactField(
            id = "cf2",
            personId = "p3",
            type = PersonContactFieldType.CUSTOM,
            label = "Ngân hàng",
            value = "Agribank 12345678",
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
        val activeCustomCccd = PersonContactField(
            id = "cf3",
            personId = "p3",
            type = PersonContactFieldType.CUSTOM,
            label = "CCCD",
            value = "079123456789",
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
        val deletedField = PersonContactField(
            id = "cf4",
            personId = "p3",
            type = PersonContactFieldType.CUSTOM,
            label = "Mã nhân viên",
            value = "NV002",
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L,
            deletedAtEpochMillis = 2000L
        )

        val fields = listOf(activePhone, activeCustomBank, activeCustomCccd, deletedField)

        assertTrue(PeopleListPresentation.matchesPersonSearch(person, fields, "0772"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, fields, "Agribank"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, fields, "agribank"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, fields, "CCCD"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, fields, "cccd"))
        assertTrue(PeopleListPresentation.matchesPersonSearch(person, fields, "Ngan hang"))

        // Deleted field NV002 must NOT match
        assertFalse(PeopleListPresentation.matchesPersonSearch(person, fields, "NV002"))
        assertFalse(PeopleListPresentation.matchesPersonSearch(person, fields, "Ma nhan vien"))
    }

    @Test
    fun `deleted person never matches search`() {
        val deletedPerson = Person(
            id = "del1",
            fullName = "Người Đã Xóa",
            group = PersonGroup.FAMILY,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 2000L,
            deletedAtEpochMillis = 2000L
        )
        assertFalse(PeopleListPresentation.matchesPersonSearch(deletedPerson, emptyList(), "Người Đã Xóa"))
        assertFalse(PeopleListPresentation.matchesPersonSearch(deletedPerson, emptyList(), ""))
    }

    @Test
    fun `calculateNextBirthday and daysUntilNextBirthday handle past, future, and year rollover`() {
        val today = LocalDate.of(2026, 8, 25)

        // Birthday later this year (e.g. Sept 10)
        val bdayLater = LocalDate.of(1995, 9, 10)
        assertEquals(LocalDate.of(2026, 9, 10), PeopleListPresentation.calculateNextBirthday(bdayLater, today))
        assertEquals(16L, PeopleListPresentation.daysUntilNextBirthday(bdayLater, today))

        // Birthday today (Aug 25)
        val bdayToday = LocalDate.of(1990, 8, 25)
        assertEquals(LocalDate.of(2026, 8, 25), PeopleListPresentation.calculateNextBirthday(bdayToday, today))
        assertEquals(0L, PeopleListPresentation.daysUntilNextBirthday(bdayToday, today))

        // Birthday already passed this year (e.g. Jan 15) -> rolls over to 2027
        val bdayPast = LocalDate.of(1988, 1, 15)
        assertEquals(LocalDate.of(2027, 1, 15), PeopleListPresentation.calculateNextBirthday(bdayPast, today))
        assertTrue(PeopleListPresentation.daysUntilNextBirthday(bdayPast, today) > 0)

        // Leap year birthday Feb 29 on non-leap year (2026 is non-leap, 2027 is non-leap, 2028 is leap)
        val bdayLeap = LocalDate.of(2000, 2, 29)
        val dec2026 = LocalDate.of(2026, 12, 1)
        val nextIn2027 = PeopleListPresentation.calculateNextBirthday(bdayLeap, dec2026)
        assertEquals(LocalDate.of(2027, 2, 28), nextIn2027)

        val jan2028 = LocalDate.of(2028, 1, 1)
        val nextIn2028 = PeopleListPresentation.calculateNextBirthday(bdayLeap, jan2028)
        assertEquals(LocalDate.of(2028, 2, 29), nextIn2028)
    }

    @Test
    fun `filterAndSortPeople combines filter and search properly`() {
        val p1 = Person("1", "Lê Văn Tính", group = PersonGroup.FAMILY, createdAtEpochMillis = 10, updatedAtEpochMillis = 10)
        val p2 = Person("2", "Lê Văn Tâm", group = PersonGroup.FRIEND, createdAtEpochMillis = 20, updatedAtEpochMillis = 20)
        val p3 = Person("3", "Trần Văn Bình", group = PersonGroup.FAMILY, createdAtEpochMillis = 30, updatedAtEpochMillis = 30)

        val people = listOf(p1, p2, p3)

        // Filter group FAMILY + search "Lê"
        val result = PeopleListPresentation.filterAndSortPeople(
            persons = people,
            contactFieldsByPersonId = emptyMap(),
            searchQuery = "Le",
            selectedGroup = PersonGroup.FAMILY,
            sortOption = PersonSortOption.NAME_AZ
        )

        assertEquals(listOf(p1), result)

        // Filter group FRIEND + search "Lê"
        val friendResult = PeopleListPresentation.filterAndSortPeople(
            persons = people,
            contactFieldsByPersonId = emptyMap(),
            searchQuery = "Le",
            selectedGroup = PersonGroup.FRIEND,
            sortOption = PersonSortOption.NAME_AZ
        )
        assertEquals(listOf(p2), friendResult)

        // Filter All (null) + search "Lê"
        val allResult = PeopleListPresentation.filterAndSortPeople(
            persons = people,
            contactFieldsByPersonId = emptyMap(),
            searchQuery = "Le",
            selectedGroup = null,
            sortOption = PersonSortOption.NAME_AZ
        )
        assertEquals(listOf(p2, p1), allResult) // Sorted A-Z: Tâm before Tính
    }

    @Test
    fun `sorting by Name A-Z orders deterministically`() {
        val p1 = Person("1", "Vũ Cường", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val p2 = Person("2", "An Nguyễn", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val p3 = Person("3", "Bình Trần", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val p4 = Person("4", "Đặng Dũng", createdAtEpochMillis = 1, updatedAtEpochMillis = 1)

        val list = listOf(p1, p2, p3, p4)
        val sorted = PeopleListPresentation.filterAndSortPeople(
            persons = list,
            contactFieldsByPersonId = emptyMap(),
            searchQuery = "",
            selectedGroup = null,
            sortOption = PersonSortOption.NAME_AZ
        )

        assertEquals(listOf("2", "3", "4", "1"), sorted.map { it.id })
    }

    @Test
    fun `sorting by Upcoming Birthday places upcoming first and missing DOB last`() {
        val today = LocalDate.of(2026, 8, 25)
        val pTomorrow = Person("1", "Bé B", birthDateSolar = LocalDate.of(2015, 8, 26), createdAtEpochMillis = 1, updatedAtEpochMillis = 1) // 1 day
        val pNextMonth = Person("2", "Cô C", birthDateSolar = LocalDate.of(1980, 9, 25), createdAtEpochMillis = 1, updatedAtEpochMillis = 1) // 31 days
        val pPassed = Person("3", "Anh A", birthDateSolar = LocalDate.of(1992, 8, 20), createdAtEpochMillis = 1, updatedAtEpochMillis = 1) // next year (approx 360 days)
        val pNoDob1 = Person("4", "Không Ngày 1", birthDateSolar = null, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)
        val pNoDob2 = Person("5", "Không Ngày 2", birthDateSolar = null, createdAtEpochMillis = 1, updatedAtEpochMillis = 1)

        val list = listOf(pNoDob2, pPassed, pNextMonth, pNoDob1, pTomorrow)
        val sorted = PeopleListPresentation.filterAndSortPeople(
            persons = list,
            contactFieldsByPersonId = emptyMap(),
            searchQuery = "",
            selectedGroup = null,
            sortOption = PersonSortOption.UPCOMING_BIRTHDAY,
            today = today
        )

        assertEquals(listOf("1", "2", "3", "4", "5"), sorted.map { it.id })
    }

    @Test
    fun `sorting by Recently Updated places newest updatedAtEpochMillis first`() {
        val pOld = Person("1", "A", createdAtEpochMillis = 100, updatedAtEpochMillis = 100)
        val pMid = Person("2", "B", createdAtEpochMillis = 100, updatedAtEpochMillis = 200)
        val pNew = Person("3", "C", createdAtEpochMillis = 100, updatedAtEpochMillis = 300)

        val list = listOf(pMid, pOld, pNew)
        val sorted = PeopleListPresentation.filterAndSortPeople(
            persons = list,
            contactFieldsByPersonId = emptyMap(),
            searchQuery = "",
            selectedGroup = null,
            sortOption = PersonSortOption.RECENTLY_UPDATED
        )

        assertEquals(listOf("3", "2", "1"), sorted.map { it.id })
    }
}
