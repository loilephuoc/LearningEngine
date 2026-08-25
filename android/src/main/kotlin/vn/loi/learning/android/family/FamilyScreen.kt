package vn.loi.learning.android.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlinx.coroutines.launch

private enum class FamilyTab(val title: String) {
    CALENDAR("Lịch"),
    PERSONS("Mọi người"),
    EVENTS("Sự kiện"),
    TASKS("Công việc")
}

internal enum class CalendarViewMode(val title: String) {
    MONTH("Tháng"),
    WEEK("Tuần"),
    LIST("Danh sách")
}

internal enum class CalendarFilter(val title: String) {
    ALL("Tất cả"),
    BIRTHDAY("Sinh nhật"),
    EVENT("Sự kiện"),
    TASK("Công việc")
}

private enum class TaskFilter(val title: String) {
    ALL("Tất cả"),
    TODAY("Hôm nay"),
    UPCOMING("Sắp tới"),
    OVERDUE("Quá hạn"),
    COMPLETED("Đã xong")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(repository: FamilyRepository, initialCalendarDate: LocalDate? = null, onBack: () -> Unit) {
    val snapshot by repository.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    val calendar = remember { AstronomicalVietnameseLunarCalendar() }
    val resolver = remember { ImportantEventOccurrenceResolver(calendar) }
    val today = LocalDate.now()
    val now = LocalDateTime.now()
    val context = LocalContext.current
    val app = context.applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
    val cloudSyncState by (app?.familyCloudSyncController?.state ?: remember { kotlinx.coroutines.flow.MutableStateFlow<FamilySyncState>(FamilySyncState.NotConfigured) }).collectAsState()
    var showCloudSync by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember {
        mutableStateOf(app?.familyNotificationPublisher?.hasPermission() ?: true)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationPermissionGranted = it
        if (it) app?.familyReminderReconciler?.reconcile(forceReschedule = true)
    }

    var tab by remember { mutableStateOf(FamilyTab.CALENDAR) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var taskFilter by remember { mutableStateOf(TaskFilter.ALL) }

    var editingPerson by remember { mutableStateOf<Person?>(null) }
    var addingPerson by remember { mutableStateOf(false) }

    var editingEvent by remember { mutableStateOf<ImportantEvent?>(null) }
    var addingEvent by remember { mutableStateOf(false) }

    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editingOccurrence by remember { mutableStateOf<TaskOccurrence?>(null) }
    var addingTask by remember { mutableStateOf(false) }

    var reminderTarget by remember { mutableStateOf<ReminderTargetInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ngày đáng nhớ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { showCloudSync = true }) {
                        Icon(Icons.Default.CloudSync, "Đồng bộ đám mây")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                when (tab) {
                    FamilyTab.CALENDAR, FamilyTab.EVENTS -> addingEvent = true
                    FamilyTab.PERSONS -> addingPerson = true
                    FamilyTab.TASKS -> addingTask = true
                }
            }) {
                Icon(Icons.Default.Add, "Thêm")
            }
        }
    ) { padding ->
        val onToggleTaskOccurrence: (Task, LocalDate, LocalTime?, Boolean) -> Unit = { task, occDate, occTime, isCurrentlyCompleted ->
            scope.launch {
                if (task.recurrence == RecurrenceType.NONE) {
                    val nextStatus = if (isCurrentlyCompleted) TaskStatus.TODO else TaskStatus.DONE
                    val compAt = if (nextStatus == TaskStatus.DONE) LocalDateTime.now() else null
                    repository.upsertTask(task.copy(status = nextStatus, completedAt = compAt, updatedAtEpochMillis = System.currentTimeMillis()))
                } else {
                    val targetDateTime = occTime?.let { LocalDateTime.of(occDate, it) }
                        ?: task.dueAt?.toLocalTime()?.let { LocalDateTime.of(occDate, it) }
                        ?: occDate.atTime(17, 0)
                    if (isCurrentlyCompleted) {
                        val existingComp = snapshot.taskOccurrenceCompletions.firstOrNull {
                            it.taskId == task.id &&
                            it.occurrenceDateTime.toLocalDate() == occDate &&
                            it.deletedAtEpochMillis == null
                        }
                        if (existingComp != null) {
                            repository.undoTaskOccurrenceCompletion(existingComp.id, System.currentTimeMillis())
                        }
                    } else {
                        val alreadyCompleted = snapshot.taskOccurrenceCompletions.any {
                            it.taskId == task.id &&
                            it.occurrenceDateTime.toLocalDate() == occDate &&
                            it.deletedAtEpochMillis == null
                        }
                        if (!alreadyCompleted) {
                            val comp = TaskOccurrenceCompletion(
                                id = UUID.randomUUID().toString(),
                                taskId = task.id,
                                occurrenceDateTime = targetDateTime,
                                completedAt = LocalDateTime.now(),
                                createdAtEpochMillis = System.currentTimeMillis(),
                                updatedAtEpochMillis = System.currentTimeMillis()
                            )
                            repository.completeTaskOccurrence(comp)
                        }
                    }
                }
            }
        }

        val onEditTaskOccurrence: (Task, LocalDate, LocalTime?) -> Unit = { task, occDate, occTime ->
            val targetDateTime = occTime?.let { LocalDateTime.of(occDate, it) }
                ?: task.dueAt?.toLocalTime()?.let { LocalDateTime.of(occDate, it) }
                ?: occDate.atTime(17, 0)
            val isCompleted = if (task.recurrence == RecurrenceType.NONE) {
                task.status == TaskStatus.DONE
            } else {
                snapshot.taskOccurrenceCompletions.any {
                    it.taskId == task.id &&
                    it.occurrenceDateTime.toLocalDate() == occDate &&
                    it.deletedAtEpochMillis == null
                }
            }
            val checklist = snapshot.checklistItems.filter { it.taskId == task.id && it.deletedAtEpochMillis == null }
            editingOccurrence = TaskOccurrence(
                task = task,
                occurrenceDueAt = targetDateTime,
                isCompleted = isCompleted,
                completedAt = null,
                checklistItems = checklist,
                daysUntil = ChronoUnit.DAYS.between(today, occDate)
            )
            editingTask = task
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(tab.ordinal) {
                FamilyTab.entries.forEach { item ->
                    Tab(tab == item, { tab = item }, text = { Text(item.title) })
                }
            }
            when (tab) {
                FamilyTab.CALENDAR -> CalendarScreenView(
                    snapshot = snapshot,
                    calendar = calendar,
                    today = today,
                    now = now,
                    initialDate = initialCalendarDate,
                    onEditPerson = { editingPerson = it },
                    onEditEvent = { editingEvent = it },
                    onEditTaskOccurrence = onEditTaskOccurrence,
                    onToggleTaskOccurrence = onToggleTaskOccurrence,
                    onReminder = { reminderTarget = it }
                )
                FamilyTab.PERSONS -> PersonList(
                    snapshot.persons.filter { it.deletedAtEpochMillis == null },
                    onEdit = { editingPerson = it },
                    onReminder = { person ->
                        val bdayDate = resolver.nextBirthday(person, today)?.occurrenceDateSolar ?: person.birthDateSolar
                        reminderTarget = ReminderTargetInfo(ReminderTargetType.PERSON_BIRTHDAY, person.id, "Sinh nhật ${person.fullName}", RecurrenceType.YEARLY, bdayDate)
                    }
                )
                FamilyTab.EVENTS -> {
                    val allOccurrences = remember(snapshot, today) {
                        resolver.allUpcoming(snapshot.persons, snapshot.events, today, snapshot.categories)
                    }
                    val filtered = remember(allOccurrences, selectedCategoryId) {
                        if (selectedCategoryId == null) allOccurrences
                        else allOccurrences.filter { it.categoryId == selectedCategoryId }
                    }

                    Column(Modifier.fillMaxSize()) {
                        CategoryFilterRow(
                            categories = snapshot.categories.filter { it.deletedAtEpochMillis == null },
                            selectedCategoryId = selectedCategoryId,
                            onSelect = { selectedCategoryId = it }
                        )
                        OccurrenceList(
                            occurrences = filtered,
                            onEditOccurrence = { occurrence ->
                                when (occurrence.sourceType) {
                                    EventSourceType.BIRTHDAY -> snapshot.persons.firstOrNull { it.id == occurrence.sourceId }?.let { editingPerson = it }
                                    EventSourceType.IMPORTANT_EVENT -> snapshot.events.firstOrNull { it.id == occurrence.sourceId }?.let { editingEvent = it }
                                }
                            },
                            onReminder = { occurrence ->
                                val targetType = when (occurrence.sourceType) {
                                    EventSourceType.BIRTHDAY -> ReminderTargetType.PERSON_BIRTHDAY
                                    EventSourceType.IMPORTANT_EVENT -> ReminderTargetType.EVENT
                                }
                                reminderTarget = ReminderTargetInfo(targetType, occurrence.sourceId, occurrence.title, RecurrenceType.YEARLY, occurrence.occurrenceDateSolar)
                            }
                        )
                    }
                }
                FamilyTab.TASKS -> {
                    val allOccurrences = remember(snapshot, now) {
                        TaskOccurrenceResolver.allOccurrences(
                            snapshot.tasks,
                            now,
                            snapshot.taskOccurrenceCompletions,
                            snapshot.checklistItems
                        )
                    }
                    val completedHistory = remember(snapshot) {
                        TaskQueryHelper.filterCompleted(snapshot.tasks, snapshot.taskOccurrenceCompletions)
                    }

                    Column(Modifier.fillMaxSize()) {
                        TaskFilterRow(selected = taskFilter, onSelect = { taskFilter = it })
                        when (taskFilter) {
                            TaskFilter.COMPLETED -> {
                                CompletedTaskHistoryList(
                                    items = completedHistory,
                                    onUndo = { item ->
                                        scope.launch {
                                            if (item.isRecurring && item.completionId != null) {
                                                repository.undoTaskOccurrenceCompletion(item.completionId, System.currentTimeMillis())
                                            } else {
                                                val task = snapshot.tasks.firstOrNull { it.id == item.taskId }
                                                if (task != null) {
                                                    repository.upsertTask(task.copy(status = TaskStatus.TODO, completedAt = null, updatedAtEpochMillis = System.currentTimeMillis()))
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            else -> {
                                val displayedOccurrences = remember(allOccurrences, taskFilter, today, now) {
                                    when (taskFilter) {
                                        TaskFilter.ALL -> TaskQueryHelper.filterActive(allOccurrences)
                                        TaskFilter.TODAY -> TaskQueryHelper.filterToday(allOccurrences, today)
                                        TaskFilter.UPCOMING -> TaskQueryHelper.filterUpcoming(allOccurrences, now)
                                        TaskFilter.OVERDUE -> TaskQueryHelper.filterOverdue(allOccurrences, now)
                                        TaskFilter.COMPLETED -> emptyList()
                                    }
                                }
                                TaskOccurrenceList(
                                    occurrences = displayedOccurrences,
                                    reminderRules = snapshot.reminderRules,
                                    persons = snapshot.persons,
                                    events = snapshot.events,
                                    onToggleComplete = { occ ->
                                        val occDate = occ.occurrenceDueAt?.toLocalDate() ?: today
                                        onToggleTaskOccurrence(occ.task, occDate, occ.occurrenceDueAt?.toLocalTime(), occ.isCompleted)
                                    },
                                    onEdit = { occ ->
                                        val occDate = occ.occurrenceDueAt?.toLocalDate() ?: today
                                        onEditTaskOccurrence(occ.task, occDate, occ.occurrenceDueAt?.toLocalTime())
                                    },
                                    onReminder = { occ ->
                                        reminderTarget = ReminderTargetInfo(
                                            ReminderTargetType.TASK,
                                            occ.task.id,
                                            occ.task.title,
                                            occ.task.recurrence,
                                            occ.occurrenceDueAt?.toLocalDate()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (addingPerson || editingPerson != null) {
        PersonEditor(
            existing = editingPerson,
            onDismiss = {
                addingPerson = false
                editingPerson = null
            }
        ) { person ->
            scope.launch {
                repository.upsertPerson(person)
                // Ensure default same-day reminder if birthday present and no reminder exists
                if (person.birthDateSolar != null && !hasSameDayReminder(snapshot.reminderRules, ReminderTargetType.PERSON_BIRTHDAY, person.id)) {
                    repository.upsertReminderRule(createDefaultSameDayReminderRule(ReminderTargetType.PERSON_BIRTHDAY, person.id))
                }
            }
            addingPerson = false
            editingPerson = null
        }
    }

    if (addingEvent || editingEvent != null) {
        EventEditor(
            existing = editingEvent,
            categories = snapshot.categories.filter { it.deletedAtEpochMillis == null },
            persons = snapshot.persons.filter { it.deletedAtEpochMillis == null },
            calendar = calendar,
            initialDate = null,
            onAddCategory = { newCategory ->
                scope.launch { repository.upsertCategory(newCategory) }
            },
            onDismiss = {
                addingEvent = false
                editingEvent = null
            }
        ) { event ->
            scope.launch {
                repository.upsertEvent(event)
                // Ensure default same-day reminder for dated event if no reminder exists
                if (!hasSameDayReminder(snapshot.reminderRules, ReminderTargetType.EVENT, event.id)) {
                    repository.upsertReminderRule(createDefaultSameDayReminderRule(ReminderTargetType.EVENT, event.id))
                }
            }
            addingEvent = false
            editingEvent = null
        }
    }

    if (addingTask || editingTask != null) {
        val currentTaskId = editingTask?.id ?: remember { UUID.randomUUID().toString() }
        val currentChecklist = snapshot.checklistItems.filter { it.taskId == currentTaskId && it.deletedAtEpochMillis == null }
        val currentReminders = snapshot.reminderRules.filter { it.targetType == ReminderTargetType.TASK && it.targetId == currentTaskId && it.deletedAtEpochMillis == null }

        TaskEditor(
            existing = editingTask,
            selectedOccurrence = editingOccurrence,
            taskId = currentTaskId,
            persons = snapshot.persons.filter { it.deletedAtEpochMillis == null },
            events = snapshot.events.filter { it.deletedAtEpochMillis == null },
            existingChecklist = currentChecklist,
            existingReminders = currentReminders,
            onDismiss = {
                addingTask = false
                editingTask = null
                editingOccurrence = null
            }
        ) { task, checklist, reminders, completedOccurrence ->
            scope.launch {
                val targetTask = if (task.recurrence != RecurrenceType.NONE) {
                    if (editingOccurrence != null) {
                        val occTime = editingOccurrence!!.occurrenceDueAt ?: LocalDateTime.now()
                        val occDate = occTime.toLocalDate()
                        val isCurrentlyCompleted = editingOccurrence!!.isCompleted
                        if (completedOccurrence && !isCurrentlyCompleted) {
                            val alreadyCompleted = snapshot.taskOccurrenceCompletions.any {
                                it.taskId == task.id &&
                                it.occurrenceDateTime.toLocalDate() == occDate &&
                                it.deletedAtEpochMillis == null
                            }
                            if (!alreadyCompleted) {
                                val completion = TaskOccurrenceCompletion(
                                    id = UUID.randomUUID().toString(),
                                    taskId = task.id,
                                    occurrenceDateTime = occTime,
                                    completedAt = LocalDateTime.now(),
                                    createdAtEpochMillis = System.currentTimeMillis(),
                                    updatedAtEpochMillis = System.currentTimeMillis()
                                )
                                repository.completeTaskOccurrence(completion)
                            }
                        } else if (!completedOccurrence && isCurrentlyCompleted) {
                            val existingComp = snapshot.taskOccurrenceCompletions.firstOrNull {
                                it.taskId == task.id &&
                                it.occurrenceDateTime.toLocalDate() == occDate &&
                                it.deletedAtEpochMillis == null
                            }
                            if (existingComp != null) {
                                repository.undoTaskOccurrenceCompletion(existingComp.id, System.currentTimeMillis())
                            }
                        }
                    }
                    val safeStatus = if (task.status == TaskStatus.DONE) TaskStatus.TODO else task.status
                    task.copy(status = safeStatus)
                } else {
                    task
                }
                repository.upsertTask(targetTask)
                repository.setChecklistItemsForTask(targetTask.id, checklist)
                // If task has due date and no reminders, ensure default same-day rule
                val finalReminders = if (targetTask.dueAt != null && reminders.isEmpty()) {
                    listOf(createDefaultSameDayReminderRule(ReminderTargetType.TASK, targetTask.id))
                } else {
                    reminders
                }
                repository.setReminderRulesForTarget(ReminderTargetType.TASK, targetTask.id, finalReminders)
            }
            addingTask = false
            editingTask = null
            editingOccurrence = null
        }
    }

    reminderTarget?.let { target ->
        val targetRules = snapshot.reminderRules.filter {
            it.targetType == target.type && it.targetId == target.id && it.deletedAtEpochMillis == null
        }
        ReminderManagerDialog(
            targetTitle = target.title,
            targetType = target.type,
            targetId = target.id,
            recurrence = target.recurrence,
            targetOccurrenceDate = target.occurrenceDate,
            existingRules = targetRules,
            notificationPermissionGranted = notificationPermissionGranted,
            alarmPrecision = app?.familyReminderScheduler?.precision,
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onScheduleDebugNotification = if (vn.loi.learning.android.BuildConfig.DEBUG) ({
                app?.familyReminderScheduler?.scheduleDebugTest()
            }) else null,
            onDismiss = { reminderTarget = null }
        ) { updatedRules ->
            scope.launch { repository.setReminderRulesForTarget(target.type, target.id, updatedRules) }
            reminderTarget = null
        }
    }
    if (showCloudSync) {
        FamilyCloudSyncDialog(
            state = cloudSyncState,
            onDismiss = { showCloudSync = false },
            onSignIn = { email, password, done -> app?.familyCloudSyncController?.signIn(email, password, done) },
            onSignOut = { app?.familyCloudSyncController?.signOut() },
            onSync = { app?.familyCloudSyncController?.syncNow() }
        )
    }
}

@Composable
private fun FamilyCloudSyncDialog(
    state: FamilySyncState,
    onDismiss: () -> Unit,
    onSignIn: (String, CharArray, (Result<Unit>) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đồng bộ đám mây") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                FamilySyncState.NotConfigured -> Text("Supabase chưa được cấu hình cho bản build này. Dữ liệu vẫn được lưu cục bộ.")
                FamilySyncState.SignedOut -> {
                    Text("Đăng nhập để sao lưu và đồng bộ dữ liệu giữa các thiết bị. Dữ liệu cục bộ vẫn hoạt động khi ngoại tuyến.")
                    Field(email, { email = it }, "Email", KeyboardType.Email)
                    OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(enabled = email.isNotBlank() && password.isNotEmpty(), onClick = {
                        val secret = password.toCharArray(); password = ""; authError = null
                        onSignIn(email, secret) { result -> authError = result.exceptionOrNull()?.message }
                    }) { Text("Đăng nhập") }
                }
                is FamilySyncState.AccountMismatch -> {
                    Text("Thiết bị đang có dữ liệu của tài khoản khác.", color = MaterialTheme.colorScheme.error)
                    Text("Dữ liệu sẽ không được tải lên tài khoản hiện tại. Hãy đăng xuất hoặc xử lý dữ liệu cục bộ bằng một quy trình chuyển đổi rõ ràng.")
                    TextButton(onClick = onSignOut) { Text("Đăng xuất") }
                }
                else -> {
                    val emailLabel = when (state) {
                        is FamilySyncState.Idle -> state.email
                        is FamilySyncState.Syncing -> state.email
                        is FamilySyncState.Offline -> state.email
                        is FamilySyncState.Failed -> state.email
                        else -> null
                    }
                    Text("Tài khoản: ${emailLabel ?: "Đã đăng nhập"}")
                    Text(when (state) {
                        is FamilySyncState.Idle -> if (state.pendingCount == 0) "Đã đồng bộ" else "${state.pendingCount} thay đổi chưa đồng bộ"
                        is FamilySyncState.Syncing -> "Đang đồng bộ..."
                        is FamilySyncState.Offline -> "Chờ mạng · ${state.pendingCount} thay đổi chưa đồng bộ"
                        is FamilySyncState.Failed -> "Lỗi đồng bộ: ${state.code}"
                        else -> ""
                    })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSync, enabled = state !is FamilySyncState.Syncing) { Text("Đồng bộ ngay") }
                        TextButton(onClick = onSignOut) { Text("Đăng xuất") }
                    }
                    Text("Đăng xuất không xóa dữ liệu trên thiết bị.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

internal fun filterOccurrences(list: List<CalendarOccurrence>, filter: CalendarFilter): List<CalendarOccurrence> {
    return when (filter) {
        CalendarFilter.ALL -> list
        CalendarFilter.BIRTHDAY -> list.filter { it.sourceType == CalendarItemType.BIRTHDAY }
        CalendarFilter.EVENT -> list.filter { it.sourceType == CalendarItemType.EVENT }
        CalendarFilter.TASK -> list.filter { it.sourceType == CalendarItemType.TASK_DUE || it.sourceType == CalendarItemType.TASK_COMPLETION }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarScreenView(
    snapshot: FamilyLocalSnapshot,
    calendar: AstronomicalVietnameseLunarCalendar,
    today: LocalDate,
    now: LocalDateTime,
    initialDate: LocalDate?,
    onEditPerson: (Person) -> Unit,
    onEditEvent: (ImportantEvent) -> Unit,
    onEditTaskOccurrence: (Task, LocalDate, LocalTime?) -> Unit,
    onToggleTaskOccurrence: (Task, LocalDate, LocalTime?, Boolean) -> Unit,
    onReminder: (ReminderTargetInfo) -> Unit
) {
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var filter by remember { mutableStateOf(CalendarFilter.ALL) }
    val startingDate = initialDate ?: today
    var selectedDate by remember { mutableStateOf(startingDate) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(startingDate)) }
    var currentWeekStart by remember { mutableStateOf(startingDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) }

    LaunchedEffect(initialDate) {
        initialDate?.let {
            selectedDate = it
            currentMonth = YearMonth.from(it)
            currentWeekStart = it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            viewMode = CalendarViewMode.MONTH
        }
    }

    val projectionService = remember { CalendarProjectionService(calendar) }

    var filterMenuExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Compact Control Bar - Row 1: View Modes Segmented Button + "Hôm nay" Button
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.height(36.dp)
            ) {
                CalendarViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = CalendarViewMode.entries.size),
                        onClick = { viewMode = mode },
                        selected = viewMode == mode,
                        label = {
                            Text(
                                mode.title,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    currentMonth = YearMonth.from(today)
                    currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    selectedDate = today
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Hôm nay",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Compact Control Bar - Row 2: Unified Navigation Header & Compact Filter Dropdown
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation on Left
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (viewMode) {
                    CalendarViewMode.MONTH -> {
                        IconButton(
                            onClick = {
                                val newMonth = currentMonth.minusMonths(1)
                                currentMonth = newMonth
                                if (selectedDate.month != newMonth.month || selectedDate.year != newMonth.year) {
                                    selectedDate = if (today.year == newMonth.year && today.month == newMonth.month) today else newMonth.atDay(1)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, "Tháng trước", modifier = Modifier.size(20.dp))
                        }
                        Text(
                            "Tháng ${currentMonth.monthValue}/${currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        IconButton(
                            onClick = {
                                val newMonth = currentMonth.plusMonths(1)
                                currentMonth = newMonth
                                if (selectedDate.month != newMonth.month || selectedDate.year != newMonth.year) {
                                    selectedDate = if (today.year == newMonth.year && today.month == newMonth.month) today else newMonth.atDay(1)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, "Tháng sau", modifier = Modifier.size(20.dp))
                        }
                    }
                    CalendarViewMode.WEEK -> {
                        val weekEnd = currentWeekStart.plusDays(6)
                        IconButton(
                            onClick = {
                                val newWeekStart = currentWeekStart.minusWeeks(1)
                                currentWeekStart = newWeekStart
                                val newWeekEnd = newWeekStart.plusDays(6)
                                if (selectedDate.isBefore(newWeekStart) || selectedDate.isAfter(newWeekEnd)) {
                                    selectedDate = if (!today.isBefore(newWeekStart) && !today.isAfter(newWeekEnd)) today else newWeekStart
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, "Tuần trước", modifier = Modifier.size(20.dp))
                        }
                        Text(
                            "Tuần ${currentWeekStart.dayOfMonth}/${currentWeekStart.monthValue} – ${weekEnd.dayOfMonth}/${weekEnd.monthValue}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                        IconButton(
                            onClick = {
                                val newWeekStart = currentWeekStart.plusWeeks(1)
                                currentWeekStart = newWeekStart
                                val newWeekEnd = newWeekStart.plusDays(6)
                                if (selectedDate.isBefore(newWeekStart) || selectedDate.isAfter(newWeekEnd)) {
                                    selectedDate = if (!today.isBefore(newWeekStart) && !today.isAfter(newWeekEnd)) today else newWeekStart
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, "Tuần sau", modifier = Modifier.size(20.dp))
                        }
                    }
                    CalendarViewMode.LIST -> {
                        Text(
                            "Danh sách sự kiện",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Compact Filter Dropdown on Right
            Box {
                OutlinedButton(
                    onClick = { filterMenuExpanded = true },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = filter.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false }
                ) {
                    CalendarFilter.entries.forEach { f ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    f.title,
                                    fontWeight = if (filter == f) FontWeight.Bold else FontWeight.Normal,
                                    color = if (filter == f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                filter = f
                                filterMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))

        when (viewMode) {
            CalendarViewMode.MONTH -> {
                MonthCalendarView(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    today = today,
                    filter = filter,
                    snapshot = snapshot,
                    calendar = calendar,
                    projectionService = projectionService,
                    now = now,
                    onSelectDate = { date ->
                        selectedDate = date
                        if (date.month != currentMonth.month || date.year != currentMonth.year) {
                            currentMonth = YearMonth.from(date)
                        }
                    },
                    onEditPerson = onEditPerson,
                    onEditEvent = onEditEvent,
                    onEditTaskOccurrence = onEditTaskOccurrence,
                    onToggleTaskOccurrence = onToggleTaskOccurrence,
                    onReminder = onReminder
                )
            }
            CalendarViewMode.WEEK -> {
                WeekCalendarView(
                    currentWeekStart = currentWeekStart,
                    selectedDate = selectedDate,
                    today = today,
                    filter = filter,
                    snapshot = snapshot,
                    calendar = calendar,
                    projectionService = projectionService,
                    now = now,
                    onEditPerson = onEditPerson,
                    onEditEvent = onEditEvent,
                    onEditTaskOccurrence = onEditTaskOccurrence,
                    onToggleTaskOccurrence = onToggleTaskOccurrence,
                    onReminder = onReminder
                )
            }
            CalendarViewMode.LIST -> {
                AgendaListCalendarView(
                    today = today,
                    filter = filter,
                    snapshot = snapshot,
                    calendar = calendar,
                    projectionService = projectionService,
                    now = now,
                    onEditPerson = onEditPerson,
                    onEditEvent = onEditEvent,
                    onEditTaskOccurrence = onEditTaskOccurrence,
                    onToggleTaskOccurrence = onToggleTaskOccurrence,
                    onReminder = onReminder
                )
            }
        }
    }
}

@Composable
fun familyLunarDateColor(isCurrentMonth: Boolean = true): Color {
    val isDark = isSystemInDarkTheme()
    val base = if (isDark) Color(0xFFFFB74D) else Color(0xFFD84315)
    return if (isCurrentMonth) base else base.copy(alpha = 0.38f)
}

@Composable
private fun MonthCalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    filter: CalendarFilter,
    snapshot: FamilyLocalSnapshot,
    calendar: AstronomicalVietnameseLunarCalendar,
    projectionService: CalendarProjectionService,
    now: LocalDateTime,
    onSelectDate: (LocalDate) -> Unit,
    onEditPerson: (Person) -> Unit,
    onEditEvent: (ImportantEvent) -> Unit,
    onEditTaskOccurrence: (Task, LocalDate, LocalTime?) -> Unit,
    onToggleTaskOccurrence: (Task, LocalDate, LocalTime?, Boolean) -> Unit,
    onReminder: (ReminderTargetInfo) -> Unit
) {
    val startGridDate = remember(currentMonth) { currentMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val endGridDate = remember(currentMonth) { currentMonth.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)) }

    val monthOccurrences = remember(snapshot, startGridDate, endGridDate, now) {
        projectionService.project(snapshot.persons, snapshot.events, snapshot.tasks, snapshot.taskOccurrenceCompletions, snapshot.categories, startGridDate, endGridDate, now)
    }

    val filteredMonthOccurrences = remember(monthOccurrences, filter) {
        filterOccurrences(monthOccurrences, filter)
    }

    val days = remember(startGridDate, endGridDate) {
        val list = mutableListOf<LocalDate>()
        var cur = startGridDate
        while (!cur.isAfter(endGridDate)) {
            list.add(cur)
            cur = cur.plusDays(1)
        }
        list
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Weekday header
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Month Grid
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp)) {
                week.forEach { date ->
                    val isCurrentMonth = date.month == currentMonth.month
                    val isToday = date == today
                    val isSelected = date == selectedDate
                    val cellOccurrences = filteredMonthOccurrences.filter { it.date == date }
                    val isOccupied = cellOccurrences.isNotEmpty()
                    val lunar = remember(date) { calendar.solarToLunar(date) }
                    val lunarText = if (lunar.day == 1) "${lunar.day}/${lunar.month}" else "${lunar.day}"

                    val cellColor = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                        isOccupied -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val cellBorder = when {
                        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        isToday -> BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary)
                        isOccupied -> BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        else -> null
                    }

                    Surface(
                        onClick = { onSelectDate(date) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .padding(1.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = cellColor,
                        border = cellBorder
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 1.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${date.dayOfMonth}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                            Text(
                                text = lunarText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = if (lunar.day == 1 || lunar.day == 15) FontWeight.Bold else FontWeight.Medium,
                                color = familyLunarDateColor(isCurrentMonth)
                            )
                            // Bounded markers row (max 3 prominent dots + "+N" overflow indicator)
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            ) {
                                if (cellOccurrences.isNotEmpty()) {
                                    val maxDots = 3
                                    val visibleOccurrences = cellOccurrences.take(maxDots)
                                    visibleOccurrences.forEach { occ ->
                                        Box(
                                            Modifier
                                                .size(5.5.dp)
                                                .padding(horizontal = 0.5.dp)
                                                .background(
                                                    color = when (occ.sourceType) {
                                                        CalendarItemType.BIRTHDAY -> MaterialTheme.colorScheme.primary
                                                        CalendarItemType.EVENT -> Color(0xFFF57C00)
                                                        CalendarItemType.TASK_DUE -> if (occ.completed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                                                        CalendarItemType.TASK_COMPLETION -> Color(0xFF2E7D32)
                                                    },
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
                                    val overflow = cellOccurrences.size - visibleOccurrences.size
                                    if (overflow > 0) {
                                        Text(
                                            text = "+$overflow",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()

        // Day Agenda
        val selectedLunar = remember(selectedDate) { calendar.solarToLunar(selectedDate) }
        val dayName = when (selectedDate.dayOfWeek) {
            DayOfWeek.MONDAY -> "Thứ Hai"
            DayOfWeek.TUESDAY -> "Thứ Ba"
            DayOfWeek.WEDNESDAY -> "Thứ Tư"
            DayOfWeek.THURSDAY -> "Thứ Năm"
            DayOfWeek.FRIDAY -> "Thứ Sáu"
            DayOfWeek.SATURDAY -> "Thứ Bảy"
            DayOfWeek.SUNDAY -> "Chủ Nhật"
        }

        val selectedOccurrences = filteredMonthOccurrences.filter { it.date == selectedDate }
        val taskCount = selectedOccurrences.count { it.sourceType == CalendarItemType.TASK_DUE || it.sourceType == CalendarItemType.TASK_COMPLETION }
        val eventCount = selectedOccurrences.count { it.sourceType == CalendarItemType.EVENT }
        val birthdayCount = selectedOccurrences.count { it.sourceType == CalendarItemType.BIRTHDAY }
        val summaryText = remember(selectedOccurrences) {
            if (selectedOccurrences.isEmpty()) {
                "Không có lịch trong ngày này"
            } else {
                buildString {
                    append("${selectedOccurrences.size} mục")
                    val parts = mutableListOf<String>()
                    if (taskCount > 0) parts.add("$taskCount việc")
                    if (eventCount > 0) parts.add("$eventCount sự kiện")
                    if (birthdayCount > 0) parts.add("$birthdayCount sinh nhật")
                    if (parts.isNotEmpty()) {
                        append(" · ")
                        append(parts.joinToString(", "))
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$dayName, ${DateInputHelper.formatDate(selectedDate)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${selectedLunar.day}/${selectedLunar.month} âm lịch",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = familyLunarDateColor(true)
                    )
                }
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedOccurrences.isEmpty()) {
                Text(
                    "Không có lịch trong ngày này.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                selectedOccurrences.forEach { occ ->
                    CalendarOccurrenceCard(
                        occ = occ,
                        snapshot = snapshot,
                        onEditPerson = onEditPerson,
                        onEditEvent = onEditEvent,
                        onEditTaskOccurrence = onEditTaskOccurrence,
                        onToggleTaskOccurrence = onToggleTaskOccurrence,
                        onReminder = onReminder
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekCalendarView(
    currentWeekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    filter: CalendarFilter,
    snapshot: FamilyLocalSnapshot,
    calendar: AstronomicalVietnameseLunarCalendar,
    projectionService: CalendarProjectionService,
    now: LocalDateTime,
    onEditPerson: (Person) -> Unit,
    onEditEvent: (ImportantEvent) -> Unit,
    onEditTaskOccurrence: (Task, LocalDate, LocalTime?) -> Unit,
    onToggleTaskOccurrence: (Task, LocalDate, LocalTime?, Boolean) -> Unit,
    onReminder: (ReminderTargetInfo) -> Unit
) {
    val weekEnd = remember(currentWeekStart) { currentWeekStart.plusDays(6) }
    val weekOccurrences = remember(snapshot, currentWeekStart, weekEnd, now) {
        projectionService.project(snapshot.persons, snapshot.events, snapshot.tasks, snapshot.taskOccurrenceCompletions, snapshot.categories, currentWeekStart, weekEnd, now)
    }
    val filteredWeekOccurrences = remember(weekOccurrences, filter) {
        filterOccurrences(weekOccurrences, filter)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..6) {
                val date = currentWeekStart.plusDays(i.toLong())
                val isToday = date == today
                val lunar = remember(date) { calendar.solarToLunar(date) }
                val dayName = when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> "Thứ Hai"
                    DayOfWeek.TUESDAY -> "Thứ Ba"
                    DayOfWeek.WEDNESDAY -> "Thứ Tư"
                    DayOfWeek.THURSDAY -> "Thứ Năm"
                    DayOfWeek.FRIDAY -> "Thứ Sáu"
                    DayOfWeek.SATURDAY -> "Thứ Bảy"
                    DayOfWeek.SUNDAY -> "Chủ Nhật"
                }
                val dayOccurrences = filteredWeekOccurrences.filter { it.date == date }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isToday) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) else CardDefaults.cardColors()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$dayName, ${DateInputHelper.formatDate(date)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${lunar.day}/${lunar.month} âm",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = familyLunarDateColor(true)
                            )
                        }

                        if (dayOccurrences.isEmpty()) {
                            Text("Không có lịch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            dayOccurrences.forEach { occ ->
                                CalendarOccurrenceCard(
                                    occ = occ,
                                    snapshot = snapshot,
                                    onEditPerson = onEditPerson,
                                    onEditEvent = onEditEvent,
                                    onEditTaskOccurrence = onEditTaskOccurrence,
                                    onToggleTaskOccurrence = onToggleTaskOccurrence,
                                    onReminder = onReminder
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaListCalendarView(
    today: LocalDate,
    filter: CalendarFilter,
    snapshot: FamilyLocalSnapshot,
    calendar: AstronomicalVietnameseLunarCalendar,
    projectionService: CalendarProjectionService,
    now: LocalDateTime,
    onEditPerson: (Person) -> Unit,
    onEditEvent: (ImportantEvent) -> Unit,
    onEditTaskOccurrence: (Task, LocalDate, LocalTime?) -> Unit,
    onToggleTaskOccurrence: (Task, LocalDate, LocalTime?, Boolean) -> Unit,
    onReminder: (ReminderTargetInfo) -> Unit
) {
    val startDate = remember(today) { today.minusDays(7) }
    val endDate = remember(today) { today.plusDays(60) }

    val listOccurrences = remember(snapshot, startDate, endDate, now) {
        projectionService.project(snapshot.persons, snapshot.events, snapshot.tasks, snapshot.taskOccurrenceCompletions, snapshot.categories, startDate, endDate, now)
    }

    val filteredList = remember(listOccurrences, filter) {
        filterOccurrences(listOccurrences, filter)
    }

    val grouped = remember(filteredList) {
        filteredList.groupBy { it.date }
    }

    if (grouped.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Chưa có sự kiện hoặc công việc sắp tới.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            grouped.forEach { (date, occurrences) ->
                item(key = "header_${date}") {
                    val lunar = calendar.solarToLunar(date)
                    val dayName = when (date.dayOfWeek) {
                        DayOfWeek.MONDAY -> "Thứ Hai"
                        DayOfWeek.TUESDAY -> "Thứ Ba"
                        DayOfWeek.WEDNESDAY -> "Thứ Tư"
                        DayOfWeek.THURSDAY -> "Thứ Năm"
                        DayOfWeek.FRIDAY -> "Thứ Sáu"
                        DayOfWeek.SATURDAY -> "Thứ Bảy"
                        DayOfWeek.SUNDAY -> "Chủ Nhật"
                    }
                    val dateLabel = when {
                        date == today -> "Hôm nay — $dayName, ${DateInputHelper.formatDate(date)}"
                        date == today.plusDays(1) -> "Ngày mai — $dayName, ${DateInputHelper.formatDate(date)}"
                        else -> "$dayName, ${DateInputHelper.formatDate(date)}"
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dateLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${lunar.day}/${lunar.month} âm",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = familyLunarDateColor(true)
                        )
                    }
                }

                items(occurrences, key = { it.id }) { occ ->
                    CalendarOccurrenceCard(
                        occ = occ,
                        snapshot = snapshot,
                        onEditPerson = onEditPerson,
                        onEditEvent = onEditEvent,
                        onEditTaskOccurrence = onEditTaskOccurrence,
                        onToggleTaskOccurrence = onToggleTaskOccurrence,
                        onReminder = onReminder
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarOccurrenceCard(
    occ: CalendarOccurrence,
    snapshot: FamilyLocalSnapshot,
    onEditPerson: (Person) -> Unit,
    onEditEvent: (ImportantEvent) -> Unit,
    onEditTaskOccurrence: (Task, LocalDate, LocalTime?) -> Unit,
    onToggleTaskOccurrence: (Task, LocalDate, LocalTime?, Boolean) -> Unit,
    onReminder: (ReminderTargetInfo) -> Unit
) {
    val isTask = occ.sourceType == CalendarItemType.TASK_DUE || occ.sourceType == CalendarItemType.TASK_COMPLETION
    val task = if (isTask) snapshot.tasks.firstOrNull { it.id == occ.sourceId && it.deletedAtEpochMillis == null } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (occ.completed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (task != null) {
                    IconButton(
                        onClick = { onToggleTaskOccurrence(task, occ.date, occ.time, occ.completed) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Checkbox(
                            checked = occ.completed,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                uncheckedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                } else {
                    Text(
                        text = occ.iconKey ?: "📌",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            when (occ.sourceType) {
                                CalendarItemType.BIRTHDAY -> snapshot.persons.firstOrNull { it.id == occ.sourceId }?.let(onEditPerson)
                                CalendarItemType.EVENT -> snapshot.events.firstOrNull { it.id == occ.sourceId }?.let(onEditEvent)
                                CalendarItemType.TASK_DUE, CalendarItemType.TASK_COMPLETION -> {
                                    if (task != null) {
                                        onEditTaskOccurrence(task, occ.date, occ.time)
                                    }
                                }
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = occ.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (occ.completed) TextDecoration.LineThrough else null,
                        color = if (occ.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    val subtitle = buildString {
                        when (occ.sourceType) {
                            CalendarItemType.BIRTHDAY -> append("Sinh nhật")
                            CalendarItemType.EVENT -> {
                                val ev = snapshot.events.firstOrNull { it.id == occ.sourceId }
                                val cat = snapshot.categories.firstOrNull { it.id == ev?.categoryId }
                                val person = snapshot.persons.firstOrNull { it.id == ev?.relatedPersonId }
                                val parts = mutableListOf<String>()
                                if (cat != null) parts.add(cat.name) else parts.add("Sự kiện")
                                if (ev?.calendarType == CalendarType.LUNAR) {
                                    parts.add("${ev.lunarDay}/${ev.lunarMonth} âm")
                                }
                                if (ev?.recurrence == RecurrenceType.YEARLY) parts.add("Hàng năm")
                                else if (ev?.recurrence == RecurrenceType.MONTHLY) parts.add("Hàng tháng")
                                if (person != null) parts.add(person.fullName)
                                else if (!ev?.relatedPersonName.isNullOrBlank()) parts.add(ev.relatedPersonName)
                                append(parts.joinToString(" · "))
                            }
                            CalendarItemType.TASK_DUE -> {
                                if (occ.completed) append("Đã hoàn thành")
                                else if (occ.overdue) append("Quá hạn")
                                else append("Hạn chót")

                                if (occ.time != null) {
                                    append(" · lúc ${occ.time.hour.pad()}:${occ.time.minute.pad()}")
                                }
                            }
                            CalendarItemType.TASK_COMPLETION -> {
                                append("Đã hoàn thành")
                                if (occ.time != null) {
                                    append(" lúc ${occ.time.hour.pad()}:${occ.time.minute.pad()}")
                                }
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            occ.completed -> MaterialTheme.colorScheme.primary
                            occ.overdue -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            // Quick reminder icon if rule exists
            val targetType = when (occ.sourceType) {
                CalendarItemType.BIRTHDAY -> ReminderTargetType.PERSON_BIRTHDAY
                CalendarItemType.EVENT -> ReminderTargetType.EVENT
                CalendarItemType.TASK_DUE, CalendarItemType.TASK_COMPLETION -> ReminderTargetType.TASK
            }
            val ruleCount = snapshot.reminderRules.count {
                it.targetType == targetType && it.targetId == occ.sourceId && it.deletedAtEpochMillis == null && it.enabled
            }
            if (ruleCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "$ruleCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<EventCategory>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onSelect(null) },
                label = { Text("Tất cả") }
            )
        }
        items(categories, key = { it.id }) { cat ->
            FilterChip(
                selected = selectedCategoryId == cat.id,
                onClick = { onSelect(if (selectedCategoryId == cat.id) null else cat.id) },
                label = { Text("${cat.iconKey.orEmpty()} ${cat.name}".trim()) }
            )
        }
    }
}

@Composable
private fun PersonList(
    persons: List<Person>,
    onEdit: (Person) -> Unit,
    onReminder: (Person) -> Unit
) = LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    if (persons.isEmpty()) item { Text("Chưa có người nào. Nhấn + để thêm.") }
    items(persons, key = { it.id }) { person ->
        Card(Modifier.fillMaxWidth().clickable { onEdit(person) }) {
            Column(Modifier.padding(16.dp)) {
                Text(person.fullName, style = MaterialTheme.typography.titleMedium)
                val groupLabel = person.group.displayName()
                val details = listOfNotNull(
                    groupLabel,
                    person.nickname?.takeIf(String::isNotBlank),
                    person.relationshipLabel?.takeIf(String::isNotBlank)
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                person.birthDateSolar?.let { Text("Sinh nhật: ${DateInputHelper.formatDate(it)}") }
                person.phone?.takeIf(String::isNotBlank)?.let { Text(it) }
                TextButton(onClick = { onReminder(person) }) { Text("Nhắc sinh nhật") }
            }
        }
    }
}

@Composable
private fun OccurrenceList(
    occurrences: List<ImportantEventOccurrence>,
    onEditOccurrence: (ImportantEventOccurrence) -> Unit,
    onReminder: (ImportantEventOccurrence) -> Unit
) = LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    if (occurrences.isEmpty()) item { Text("Chưa có sự kiện nào.") }
    items(occurrences, key = { "${it.sourceType}-${it.sourceId}-${it.occurrenceDateSolar}" }) { occ ->
        Card(Modifier.fillMaxWidth().clickable { onEditOccurrence(occ) }) {
            Column(Modifier.padding(16.dp)) {
                val icon = occ.iconKey ?: "📌"
                Text("$icon ${occ.title}", style = MaterialTheme.typography.titleMedium)
                if (occ.calendarLabel != null) {
                    Text(occ.calendarLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                val daysText = when {
                    occ.daysUntil == 0L -> "hôm nay"
                    occ.daysUntil > 0L -> "còn ${occ.daysUntil} ngày"
                    else -> "đã qua"
                }
                Text("${DateInputHelper.formatDate(occ.occurrenceDateSolar)} · $daysText", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onReminder(occ) }) { Text("Nhắc nhở") }
            }
        }
    }
}

private data class ReminderTargetInfo(
    val type: ReminderTargetType,
    val id: String,
    val title: String,
    val recurrence: RecurrenceType,
    val occurrenceDate: LocalDate? = null
)

@Composable
private fun ReminderManagerDialog(
    targetTitle: String,
    targetType: ReminderTargetType,
    targetId: String,
    recurrence: RecurrenceType = RecurrenceType.NONE,
    targetOccurrenceDate: LocalDate? = null,
    existingRules: List<ReminderRule>,
    notificationPermissionGranted: Boolean,
    alarmPrecision: FamilyAlarmPrecision?,
    onRequestNotificationPermission: () -> Unit,
    onScheduleDebugNotification: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSave: (List<ReminderRule>) -> Unit
) {
    val initialRules = remember(existingRules, targetId, targetType) {
        if (existingRules.none { it.amount == 0 && it.unit == ReminderOffsetUnit.DAY }) {
            listOf(createDefaultSameDayReminderRule(targetType, targetId)) + existingRules
        } else {
            existingRules
        }
    }
    var rules by remember(initialRules) { mutableStateOf(initialRules) }
    var addingRule by remember { mutableStateOf(false) }

    var newAmount by remember { mutableStateOf("1") }
    var newUnit by remember { mutableStateOf(ReminderOffsetUnit.DAY) }
    var newHour by remember { mutableStateOf("8") }
    var newMinute by remember { mutableStateOf("0") }
    var repeatSwitch by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nhắc nhở: $targetTitle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (notificationPermissionGranted) "Thông báo đã bật" else "Thông báo đang bị tắt trong hệ thống",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (notificationPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            if (alarmPrecision == FamilyAlarmPrecision.EXACT) "Lịch nhắc chính xác theo phút"
                            else "Hệ thống có thể giao thông báo trễ một khoảng ngắn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!notificationPermissionGranted) {
                            TextButton(onClick = onRequestNotificationPermission) { Text("Cho phép thông báo") }
                        }
                        if (onScheduleDebugNotification != null) {
                            OutlinedButton(
                                onClick = onScheduleDebugNotification,
                                enabled = notificationPermissionGranted,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Test thông báo sau 10 giây") }
                        }
                    }
                }
                if (recurrence == RecurrenceType.YEARLY) {
                    Text(
                        "Mỗi lần nhắc có thể lặp hàng năm hoặc chỉ áp dụng một lần.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (recurrence == RecurrenceType.MONTHLY) {
                    Text(
                        "Mỗi lần nhắc có thể lặp hàng tháng hoặc chỉ áp dụng một lần.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (rules.isEmpty() && !addingRule) {
                    Text("Chưa cài đặt nhắc nhở nào.", style = MaterialTheme.typography.bodyMedium)
                }

                rules.forEachIndexed { index, rule ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Switch(
                                    checked = rule.enabled,
                                    onCheckedChange = { enabled ->
                                        rules = rules.toMutableList().also {
                                            it[index] = rule.copy(enabled = enabled, updatedAtEpochMillis = System.currentTimeMillis())
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    val label = if (rule.amount == 0) {
                                        "Đúng ngày lúc ${rule.remindHour.pad()}:${rule.remindMinute.pad()}"
                                    } else {
                                        "Trước ${rule.amount} ${rule.unit.displayName()} lúc ${rule.remindHour.pad()}:${rule.remindMinute.pad()}"
                                    }
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                    val repeatLabel = when {
                                        rule.repeatMode == ReminderRepeatMode.ONCE -> "Chỉ lần này"
                                        recurrence == RecurrenceType.YEARLY -> "↻ Mỗi năm"
                                        recurrence == RecurrenceType.MONTHLY -> "↻ Mỗi tháng"
                                        else -> null
                                    }
                                    if (repeatLabel != null) {
                                        Text(
                                            repeatLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (rule.repeatMode == ReminderRepeatMode.ONCE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = {
                                rules = rules.filterIndexed { i, _ -> i != index }
                            }) {
                                Icon(Icons.Default.Close, "Xóa lần nhắc")
                            }
                        }
                    }
                }

                if (addingRule) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Thêm lần nhắc mới", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    Field(newAmount, { newAmount = it.filter(Char::isDigit).take(3) }, "Số lượng", KeyboardType.Number)
                                }
                                Box(Modifier.weight(1.5f)) {
                                    Column {
                                        Text("Đơn vị", style = MaterialTheme.typography.labelSmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            ReminderOffsetUnit.entries.forEach { u ->
                                                FilterChip(
                                                    selected = newUnit == u,
                                                    onClick = { newUnit = u },
                                                    label = { Text(u.displayName()) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) {
                                    Field(newHour, { newHour = it.filter(Char::isDigit).take(2) }, "Giờ (0-23)", KeyboardType.Number)
                                }
                                Box(Modifier.weight(1f)) {
                                    Field(newMinute, { newMinute = it.filter(Char::isDigit).take(2) }, "Phút (0-59)", KeyboardType.Number)
                                }
                            }

                            if (recurrence == RecurrenceType.YEARLY || recurrence == RecurrenceType.MONTHLY) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(checked = repeatSwitch, onCheckedChange = { repeatSwitch = it })
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (recurrence == RecurrenceType.YEARLY) "Nhắc lại mỗi năm" else "Nhắc lại mỗi tháng",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { addingRule = false }) { Text("Hủy") }
                                val validAmount = newAmount.toIntOrNull()?.let { it >= 0 } == true
                                val validHour = newHour.toIntOrNull() in 0..23
                                val validMinute = newMinute.toIntOrNull() in 0..59
                                TextButton(
                                    enabled = validAmount && validHour && validMinute,
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        val mode = if (recurrence != RecurrenceType.NONE && !repeatSwitch) ReminderRepeatMode.ONCE else ReminderRepeatMode.FOLLOW_TARGET
                                        val anchorDate = if (mode == ReminderRepeatMode.ONCE) targetOccurrenceDate ?: LocalDate.now() else null
                                        val newRule = ReminderRule(
                                            id = UUID.randomUUID().toString(),
                                            targetType = targetType,
                                            targetId = targetId,
                                            amount = newAmount.toInt(),
                                            unit = newUnit,
                                            remindHour = newHour.toInt(),
                                            remindMinute = newMinute.toInt(),
                                            enabled = true,
                                            createdAtEpochMillis = now,
                                            updatedAtEpochMillis = now,
                                            repeatMode = mode,
                                            occurrenceDate = anchorDate
                                        )
                                        // Avoid exact duplicates
                                        if (rules.none { it.amount == newRule.amount && it.unit == newRule.unit && it.remindHour == newRule.remindHour && it.remindMinute == newRule.remindMinute && it.repeatMode == newRule.repeatMode && it.occurrenceDate == newRule.occurrenceDate }) {
                                            rules = rules + newRule
                                        }
                                        addingRule = false
                                        repeatSwitch = true
                                    }
                                ) { Text("Thêm") }
                            }
                        }
                    }
                } else {
                    OutlinedButton(onClick = {
                        repeatSwitch = true
                        addingRule = true
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm lần nhắc")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(rules) }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonEditor(
    existing: Person?,
    onDismiss: () -> Unit,
    onSave: (Person) -> Unit
) {
    var group by remember(existing) { mutableStateOf(existing?.group ?: PersonGroup.FAMILY) }
    var name by remember(existing) { mutableStateOf(existing?.fullName.orEmpty()) }
    var nickname by remember(existing) { mutableStateOf(existing?.nickname.orEmpty()) }
    var relation by remember(existing) { mutableStateOf(existing?.relationshipLabel.orEmpty()) }
    var birthdayValue by remember(existing) {
        val formatted = DateInputHelper.formatDate(existing?.birthDateSolar)
        mutableStateOf(TextFieldValue(formatted, TextRange(formatted.length)))
    }
    var phone by remember(existing) { mutableStateOf(existing?.phone.orEmpty()) }
    var address by remember(existing) { mutableStateOf(existing?.address.orEmpty()) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }

    val parsedDate = remember(birthdayValue.text) { DateInputHelper.parseDate(birthdayValue.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Thêm người" else "Sửa thông tin") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nhóm:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PersonGroup.entries.forEach { entry ->
                        FilterChip(
                            selected = group == entry,
                            onClick = { group = entry },
                            label = { Text(entry.displayName()) }
                        )
                    }
                }
                Field(name, { name = it }, "Họ tên *")
                Field(nickname, { nickname = it }, "Tên gọi")
                Field(relation, { relation = it }, "Quan hệ / ghi chú quan hệ")
                FamilyGregorianDateField(
                    value = birthdayValue,
                    onValueChange = { birthdayValue = it },
                    label = "Ngày sinh dương lịch (dd-MM-yyyy)"
                )
                Field(phone, { phone = it }, "Điện thoại", KeyboardType.Phone)
                Field(address, { address = it }, "Địa chỉ")
                Field(note, { note = it }, "Ghi chú")
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (birthdayValue.text.isBlank() || parsedDate != null),
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        Person(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            fullName = name.trim(),
                            nickname = nickname.blankNull(),
                            group = group,
                            relationshipLabel = relation.blankNull(),
                            birthDateSolar = parsedDate,
                            phone = phone.blankNull(),
                            address = address.blankNull(),
                            note = note.blankNull(),
                            avatarRef = existing?.avatarRef,
                            createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                            updatedAtEpochMillis = now,
                            deletedAtEpochMillis = existing?.deletedAtEpochMillis
                        )
                    )
                }
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EventEditor(
    existing: ImportantEvent?,
    categories: List<EventCategory>,
    persons: List<Person>,
    calendar: AstronomicalVietnameseLunarCalendar,
    initialDate: LocalDate? = null,
    onAddCategory: (EventCategory) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ImportantEvent) -> Unit
) {
    val defaultCatId = categories.firstOrNull { it.builtInKey == "ANNIVERSARY" }?.id
        ?: categories.firstOrNull { it.builtInKey != "MEMORIAL" && it.builtInKey != "BIRTHDAY" }?.id
        ?: categories.firstOrNull()?.id.orEmpty()
    var selectedCatId by remember(existing) { mutableStateOf(existing?.categoryId ?: defaultCatId) }
    val currentCategory = categories.firstOrNull { it.id == selectedCatId }
    val isMemorial = currentCategory?.builtInKey == "MEMORIAL"
    val isBirthday = currentCategory?.builtInKey == "BIRTHDAY"

    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedPersonId by remember(existing) { mutableStateOf(existing?.relatedPersonId) }
    var relatedPersonName by remember(existing) { mutableStateOf(existing?.relatedPersonName.orEmpty()) }
    var showPersonPicker by remember { mutableStateOf(false) }

    var calendarType by remember(existing, selectedCatId) {
        mutableStateOf(existing?.calendarType ?: if (isMemorial) CalendarType.LUNAR else CalendarType.SOLAR)
    }

    val initialSolar = remember(existing, initialDate) {
        existing?.solarDate ?: if (existing == null) initialDate else null
    }
    var solarDateValue by remember(existing, initialDate) {
        val formatted = DateInputHelper.formatDate(initialSolar)
        mutableStateOf(TextFieldValue(formatted, TextRange(formatted.length)))
    }

    val initialLunar = remember(existing, initialDate) {
        if (existing == null && initialDate != null) calendar.solarToLunar(initialDate) else null
    }
    var lunarDay by remember(existing, initialLunar) {
        mutableStateOf(existing?.lunarDay?.toString() ?: initialLunar?.day?.toString().orEmpty())
    }
    var lunarMonth by remember(existing, initialLunar) {
        mutableStateOf(existing?.lunarMonth?.toString() ?: initialLunar?.month?.toString().orEmpty())
    }
    var sourceYear by remember(existing, initialLunar) {
        mutableStateOf(existing?.sourceYear?.toString() ?: if (existing == null && initialLunar != null) initialLunar.year.toString() else "")
    }
    var lunarLeapMonth by remember(existing, initialLunar) {
        mutableStateOf(existing?.lunarLeapMonth ?: initialLunar?.isLeapMonth ?: false)
    }

    var recurrence by remember(existing, selectedCatId) {
        mutableStateOf(existing?.recurrence ?: if (isMemorial) RecurrenceType.YEARLY else RecurrenceType.YEARLY)
    }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }

    var addingCustomCategory by remember { mutableStateOf(false) }
    var customCatName by remember { mutableStateOf("") }

    val parsedSolar = remember(solarDateValue.text) { DateInputHelper.parseDate(solarDateValue.text) }

    val isValid = when {
        isBirthday -> false // Birthday should be added via Person
        title.isBlank() -> false
        calendarType == CalendarType.SOLAR -> parsedSolar != null
        calendarType == CalendarType.LUNAR -> {
            val d = lunarDay.toIntOrNull()
            val m = lunarMonth.toIntOrNull()
            val validDayMonth = d != null && d in 1..30 && m != null && m in 1..12
            if (recurrence == RecurrenceType.NONE) {
                val y = sourceYear.toIntOrNull()
                validDayMonth && y != null && y in 1900..2100
            } else {
                validDayMonth
            }
        }
        else -> false
    }

    val selectedPerson = remember(selectedPersonId, persons) {
        persons.firstOrNull { it.id == selectedPersonId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Thêm sự kiện" else "Sửa sự kiện") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isBirthday) {
                    Text(
                        "Sinh nhật được tính tự động từ danh sách 'Mọi người'. Hãy thêm hoặc sửa thông tin tại tab 'Mọi người'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // 1. Title
                    Field(title, { title = it }, if (isMemorial) "Họ tên người đã khuất *" else "Tên sự kiện *")

                    // 2. Compact Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${currentCategory?.iconKey.orEmpty()} ${currentCategory?.name.orEmpty()}".trim(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Loại sự kiện") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.iconKey.orEmpty()} ${cat.name}".trim()) },
                                    onClick = {
                                        selectedCatId = cat.id
                                        if (cat.builtInKey == "MEMORIAL") {
                                            calendarType = CalendarType.LUNAR
                                            recurrence = RecurrenceType.YEARLY
                                        }
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Thêm loại sự kiện...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    categoryDropdownExpanded = false
                                    addingCustomCategory = true
                                }
                            )
                        }
                    }

                    // 3. Calendar Type (if not memorial)
                    if (!isMemorial) {
                        Text("Loại lịch:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = calendarType == CalendarType.SOLAR,
                                onClick = { calendarType = CalendarType.SOLAR },
                                label = { Text("Dương lịch") }
                            )
                            FilterChip(
                                selected = calendarType == CalendarType.LUNAR,
                                onClick = { calendarType = CalendarType.LUNAR },
                                label = { Text("Âm lịch") }
                            )
                        }
                    }

                    // 4. Date Fields
                    if (calendarType == CalendarType.SOLAR) {
                        FamilyGregorianDateField(
                            value = solarDateValue,
                            onValueChange = { solarDateValue = it },
                            label = "Ngày dương lịch (dd-MM-yyyy) *"
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ngày âm lịch:", style = MaterialTheme.typography.labelMedium)
                            TextButton(
                                onClick = {
                                    val todayLunar = calendar.solarToLunar(LocalDate.now())
                                    lunarDay = todayLunar.day.toString()
                                    lunarMonth = todayLunar.month.toString()
                                    lunarLeapMonth = todayLunar.isLeapMonth
                                    if (recurrence == RecurrenceType.NONE) {
                                        sourceYear = todayLunar.year.toString()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Hôm nay (Âm lịch)", fontSize = 11.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                Field(lunarDay, { lunarDay = it.filter(Char::isDigit).take(2) }, "Ngày âm *", KeyboardType.Number)
                            }
                            Box(Modifier.weight(1f)) {
                                Field(lunarMonth, { lunarMonth = it.filter(Char::isDigit).take(2) }, "Tháng âm *", KeyboardType.Number)
                            }
                        }
                        if (recurrence == RecurrenceType.NONE) {
                            Field(sourceYear, { sourceYear = it.filter(Char::isDigit).take(4) }, "Năm âm lịch *", KeyboardType.Number)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(lunarLeapMonth, { lunarLeapMonth = it })
                            Spacer(Modifier.width(8.dp))
                            Text(if (isMemorial) "Ngày gốc thuộc tháng nhuận" else "Tháng nhuận")
                        }
                    }

                    // 5. Recurrence
                    if (!isMemorial) {
                        Text("Lặp lại:", style = MaterialTheme.typography.labelMedium)
                        if (calendarType == CalendarType.SOLAR) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = recurrence == RecurrenceType.NONE,
                                    onClick = { recurrence = RecurrenceType.NONE },
                                    label = { Text("Không lặp") }
                                )
                                FilterChip(
                                    selected = recurrence == RecurrenceType.MONTHLY,
                                    onClick = { recurrence = RecurrenceType.MONTHLY },
                                    label = { Text("Hàng tháng") }
                                )
                                FilterChip(
                                    selected = recurrence == RecurrenceType.YEARLY,
                                    onClick = { recurrence = RecurrenceType.YEARLY },
                                    label = { Text("Hàng năm") }
                                )
                            }
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = recurrence == RecurrenceType.NONE,
                                    onClick = { recurrence = RecurrenceType.NONE },
                                    label = { Text("Không lặp") }
                                )
                                FilterChip(
                                    selected = recurrence == RecurrenceType.YEARLY,
                                    onClick = { recurrence = RecurrenceType.YEARLY },
                                    label = { Text("Hàng năm") }
                                )
                            }
                        }
                        if (recurrence != RecurrenceType.NONE) {
                            Text(
                                "Nhắc nhở sẽ tự động áp dụng cho mỗi lần lặp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 6. Person Picker
                    if (persons.isNotEmpty()) {
                        Text("Người liên quan:", style = MaterialTheme.typography.labelMedium)
                        if (selectedPerson != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = true,
                                    onClick = { showPersonPicker = true },
                                    label = { Text(selectedPerson.fullName) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Bỏ chọn người này",
                                            modifier = Modifier.size(16.dp).clickable { selectedPersonId = null }
                                        )
                                    }
                                )
                            }
                        } else {
                            AssistChip(
                                onClick = { showPersonPicker = true },
                                label = { Text("+ Chọn người liên quan") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }

                    // 7. Relationship / Note for Person
                    Field(relatedPersonName, { relatedPersonName = it }, "Quan hệ / ghi chú người liên quan (tùy chọn)")

                    // 8. General Note
                    Field(note, { note = it }, "Ghi chú")
                }
            }
        },
        confirmButton = {
            if (!isBirthday) {
                TextButton(
                    enabled = isValid,
                    onClick = {
                        val now = System.currentTimeMillis()
                        onSave(
                            ImportantEvent(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                categoryId = selectedCatId,
                                title = title.trim(),
                                relatedPersonId = selectedPersonId,
                                relatedPersonName = relatedPersonName.blankNull(),
                                calendarType = calendarType,
                                solarDate = if (calendarType == CalendarType.SOLAR) parsedSolar else null,
                                lunarDay = if (calendarType == CalendarType.LUNAR) lunarDay.toIntOrNull() else null,
                                lunarMonth = if (calendarType == CalendarType.LUNAR) lunarMonth.toIntOrNull() else null,
                                lunarLeapMonth = if (calendarType == CalendarType.LUNAR) lunarLeapMonth else false,
                                sourceYear = if (calendarType == CalendarType.LUNAR && recurrence == RecurrenceType.NONE) sourceYear.toIntOrNull() else null,
                                recurrence = recurrence,
                                note = note.blankNull(),
                                createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                                updatedAtEpochMillis = now,
                                deletedAtEpochMillis = existing?.deletedAtEpochMillis
                            )
                        )
                    }
                ) { Text("Lưu") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )

    if (showPersonPicker) {
        AlertDialog(
            onDismissRequest = { showPersonPicker = false },
            title = { Text("Chọn người liên quan") },
            text = {
                val activePersons = persons.filter { it.deletedAtEpochMillis == null }
                if (activePersons.isEmpty()) {
                    Text("Chưa có hồ sơ trong danh sách 'Mọi người'.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(activePersons, key = { it.id }) { p ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPersonId = p.id
                                        if (relatedPersonName.isBlank() && !p.relationshipLabel.isNullOrBlank()) {
                                            relatedPersonName = p.relationshipLabel
                                        }
                                        showPersonPicker = false
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedPersonId == p.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(p.fullName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        if (!p.relationshipLabel.isNullOrBlank()) {
                                            Text(p.relationshipLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(p.group.displayName(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonPicker = false }) { Text("Đóng") }
            }
        )
    }

    if (addingCustomCategory) {
        AlertDialog(
            onDismissRequest = { addingCustomCategory = false },
            title = { Text("Thêm loại sự kiện") },
            text = {
                Field(customCatName, { customCatName = it }, "Tên loại sự kiện")
            },
            confirmButton = {
                TextButton(
                    enabled = customCatName.isNotBlank(),
                    onClick = {
                        val now = System.currentTimeMillis()
                        val newCat = EventCategory(
                            id = UUID.randomUUID().toString(),
                            name = customCatName.trim(),
                            builtInKey = null,
                            builtIn = false,
                            iconKey = "📌",
                            sortOrder = categories.size + 1,
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now
                        )
                        onAddCategory(newCat)
                        selectedCatId = newCat.id
                        addingCustomCategory = false
                        customCatName = ""
                    }
                ) { Text("Thêm") }
            },
            dismissButton = {
                TextButton(onClick = { addingCustomCategory = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun TaskFilterRow(
    selected: TaskFilter,
    onSelect: (TaskFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(TaskFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.title) }
            )
        }
    }
}

@Composable
private fun TaskOccurrenceList(
    occurrences: List<TaskOccurrence>,
    reminderRules: List<ReminderRule>,
    persons: List<Person>,
    events: List<ImportantEvent>,
    onToggleComplete: (TaskOccurrence) -> Unit,
    onEdit: (TaskOccurrence) -> Unit,
    onReminder: (TaskOccurrence) -> Unit
) = LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    if (occurrences.isEmpty()) item { Text("Không có công việc nào.") }
    items(occurrences, key = { "${it.task.id}-${it.occurrenceDueAt}" }) { occ ->
        val task = occ.task
        val personMap = remember(persons) { persons.associateBy { it.id } }
        val eventMap = remember(events) { events.associateBy { it.id } }
        val rulesCount = remember(reminderRules, task.id) {
            reminderRules.count { it.targetType == ReminderTargetType.TASK && it.targetId == task.id && it.deletedAtEpochMillis == null && it.enabled }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (occ.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onToggleComplete(occ) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Checkbox(
                        checked = occ.isCompleted,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = { onEdit(occ) }),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (occ.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                        if (task.priority != TaskPriority.NORMAL) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(task.priority.displayName(), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    if (!task.description.isNullOrBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (occ.occurrenceDueAt != null) {
                            val due = occ.occurrenceDueAt
                            val days = occ.daysUntil
                            val dueText = when {
                                days == null -> "${DateInputHelper.formatDate(due.toLocalDate())} ${due.hour.pad()}:${due.minute.pad()}"
                                days == 0L -> "Hạn hôm nay ${due.hour.pad()}:${due.minute.pad()}"
                                days > 0L -> "Hạn còn $days ngày (${DateInputHelper.formatDate(due.toLocalDate())})"
                                else -> "Quá hạn ${-days} ngày"
                            }
                            Text(
                                dueText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (days != null && days < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (task.recurrence != RecurrenceType.NONE) {
                            val recLabel = when (task.recurrence) {
                                RecurrenceType.NONE -> ""
                                RecurrenceType.MONTHLY -> "↻ Hàng tháng"
                                RecurrenceType.YEARLY -> "↻ Hàng năm"
                            }
                            Text(recLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    if (occ.checklistItems.isNotEmpty()) {
                        val completedCount = occ.checklistItems.count { it.completed }
                        Text("Checklist $completedCount/${occ.checklistItems.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    val relPerson = task.relatedPersonId?.let { personMap[it] }
                    val relEvent = task.relatedEventId?.let { eventMap[it] }
                    if (relPerson != null || relEvent != null) {
                        val label = listOfNotNull(
                            relPerson?.let { "Người: ${it.fullName}" },
                            relEvent?.let { "Sự kiện: ${it.title}" }
                        ).joinToString(" · ")
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                if (task.dueAt != null) {
                    IconButton(onClick = { onReminder(occ) }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Nhắc nhở",
                            tint = if (rulesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedTaskHistoryList(
    items: List<CompletedTaskHistoryItem>,
    onUndo: (CompletedTaskHistoryItem) -> Unit
) = LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    if (items.isEmpty()) item { Text("Chưa có lịch sử hoàn thành.") }
    items(items, key = { "${it.taskId}-${it.occurrenceDateTime}-${it.completedAt}" }) { item ->
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    val compText = "Hoàn thành: ${DateInputHelper.formatDate(item.completedAt.toLocalDate())} ${item.completedAt.hour.pad()}:${item.completedAt.minute.pad()}"
                    Text(compText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (item.isRecurring && item.occurrenceDateTime != null) {
                        Text("Kỳ hạn: ${DateInputHelper.formatDate(item.occurrenceDateTime.toLocalDate())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { onUndo(item) }) {
                    Text("Hoàn tác")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskEditor(
    existing: Task?,
    selectedOccurrence: TaskOccurrence? = null,
    taskId: String,
    persons: List<Person>,
    events: List<ImportantEvent>,
    existingChecklist: List<ChecklistItem>,
    existingReminders: List<ReminderRule>,
    onDismiss: () -> Unit,
    onSave: (Task, List<ChecklistItem>, List<ReminderRule>, Boolean) -> Unit
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var status by remember(existing) { mutableStateOf(existing?.status ?: TaskStatus.TODO) }
    var priority by remember(existing) { mutableStateOf(existing?.priority ?: TaskPriority.NORMAL) }
    var completeSelectedOccurrence by remember(selectedOccurrence) { mutableStateOf(selectedOccurrence?.isCompleted == true) }

    var hasStart by remember(existing) { mutableStateOf(existing?.startAt != null) }
    var startDateValue by remember(existing) {
        val f = DateInputHelper.formatDate(existing?.startAt?.toLocalDate())
        mutableStateOf(TextFieldValue(f, TextRange(f.length)))
    }
    var startHour by remember(existing) { mutableStateOf(existing?.startAt?.hour?.pad() ?: "08") }
    var startMinute by remember(existing) { mutableStateOf(existing?.startAt?.minute?.pad() ?: "00") }

    var hasDue by remember(existing) { mutableStateOf(existing?.dueAt != null) }
    var dueDateValue by remember(existing) {
        val f = DateInputHelper.formatDate(existing?.dueAt?.toLocalDate())
        mutableStateOf(TextFieldValue(f, TextRange(f.length)))
    }
    var dueHour by remember(existing) { mutableStateOf(existing?.dueAt?.hour?.pad() ?: "18") }
    var dueMinute by remember(existing) { mutableStateOf(existing?.dueAt?.minute?.pad() ?: "00") }

    var recurrence by remember(existing) { mutableStateOf(existing?.recurrence ?: RecurrenceType.NONE) }
    var relatedPersonId by remember(existing) { mutableStateOf(existing?.relatedPersonId) }
    var relatedEventId by remember(existing) { mutableStateOf(existing?.relatedEventId) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }

    var checklist by remember(existingChecklist) { mutableStateOf(existingChecklist) }
    var newChecklistText by remember { mutableStateOf("") }

    var reminderRules by remember(existingReminders, hasDue) {
        val initial = if (existingReminders.isEmpty() && hasDue) {
            listOf(createDefaultSameDayReminderRule(ReminderTargetType.TASK, taskId))
        } else {
            existingReminders
        }
        mutableStateOf(initial)
    }
    var addingReminder by remember { mutableStateOf(false) }
    var remAmount by remember { mutableStateOf("1") }
    var remUnit by remember { mutableStateOf(ReminderOffsetUnit.DAY) }
    var remHour by remember { mutableStateOf("8") }
    var remMinute by remember { mutableStateOf("0") }
    var remRepeatSwitch by remember { mutableStateOf(true) }

    val parsedStartDate = remember(startDateValue.text) { DateInputHelper.parseDate(startDateValue.text) }
    val parsedDueDate = remember(dueDateValue.text) { DateInputHelper.parseDate(dueDateValue.text) }

    val validStartHour = startHour.toIntOrNull() in 0..23
    val validStartMinute = startMinute.toIntOrNull() in 0..59
    val validDueHour = dueHour.toIntOrNull() in 0..23
    val validDueMinute = dueMinute.toIntOrNull() in 0..59

    val startDateTime = if (hasStart && parsedStartDate != null && validStartHour && validStartMinute) {
        LocalDateTime.of(parsedStartDate, java.time.LocalTime.of(startHour.toInt(), startMinute.toInt()))
    } else null

    val dueDateTime = if (hasDue && parsedDueDate != null && validDueHour && validDueMinute) {
        LocalDateTime.of(parsedDueDate, java.time.LocalTime.of(dueHour.toInt(), dueMinute.toInt()))
    } else null

    val isDateOrderValid = when {
        startDateTime != null && dueDateTime != null -> !startDateTime.isAfter(dueDateTime)
        else -> true
    }

    val isValid = title.isNotBlank() &&
            (!hasStart || startDateTime != null) &&
            (!hasDue || dueDateTime != null) &&
            isDateOrderValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Thêm công việc" else "Sửa công việc") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Field(title, { title = it }, "Tiêu đề *")
                Field(description, { description = it }, "Mô tả")

                if ((existing?.recurrence ?: recurrence) != RecurrenceType.NONE && selectedOccurrence != null) {
                    val occDateText = selectedOccurrence.occurrenceDueAt?.toLocalDate()?.let { DateInputHelper.formatDate(it) } ?: "kỳ này"
                    Text("Hoàn thành kỳ được chọn:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    FilterChip(
                        selected = completeSelectedOccurrence,
                        onClick = { completeSelectedOccurrence = !completeSelectedOccurrence },
                        leadingIcon = {
                            Icon(
                                if (completeSelectedOccurrence) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                "Hoàn thành kỳ $occDateText",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Trạng thái chu kỳ lặp:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED).forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s.displayName()) }
                            )
                        }
                    }
                } else {
                    Text("Trạng thái:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskStatus.entries.forEach { s ->
                            val isDone = s == TaskStatus.DONE
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                leadingIcon = if (isDone && status == s) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                label = { Text(s.displayName(), fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal) },
                                colors = if (isDone) {
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else FilterChipDefaults.filterChipColors()
                            )
                        }
                    }
                }

                Text("Mức ưu tiên:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.displayName()) }
                        )
                    }
                }

                // Start date/time section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = hasStart, onCheckedChange = { hasStart = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Có thời gian bắt đầu")
                }
                if (hasStart) {
                    FamilyGregorianDateField(
                        value = startDateValue,
                        onValueChange = { startDateValue = it },
                        label = "Ngày bắt đầu (dd-MM-yyyy)"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            Field(startHour, { startHour = it.filter(Char::isDigit).take(2) }, "Giờ (0-23)", KeyboardType.Number)
                        }
                        Box(Modifier.weight(1f)) {
                            Field(startMinute, { startMinute = it.filter(Char::isDigit).take(2) }, "Phút (0-59)", KeyboardType.Number)
                        }
                    }
                }

                // Due date/time section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = hasDue, onCheckedChange = {
                        hasDue = it
                        if (it && reminderRules.isEmpty()) {
                            reminderRules = listOf(createDefaultSameDayReminderRule(ReminderTargetType.TASK, taskId))
                        }
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("Có hạn chót (Deadline)")
                }
                if (hasDue) {
                    FamilyGregorianDateField(
                        value = dueDateValue,
                        onValueChange = { dueDateValue = it },
                        label = "Hạn chót (dd-MM-yyyy) *"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            Field(dueHour, { dueHour = it.filter(Char::isDigit).take(2) }, "Giờ (0-23)", KeyboardType.Number)
                        }
                        Box(Modifier.weight(1f)) {
                            Field(dueMinute, { dueMinute = it.filter(Char::isDigit).take(2) }, "Phút (0-59)", KeyboardType.Number)
                        }
                    }
                }

                Text("Lặp lại:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurrenceType.entries.forEach { r ->
                        FilterChip(
                            selected = recurrence == r,
                            onClick = { recurrence = r },
                            label = {
                                Text(when (r) {
                                    RecurrenceType.NONE -> "Không lặp"
                                    RecurrenceType.MONTHLY -> "Hàng tháng"
                                    RecurrenceType.YEARLY -> "Hàng năm"
                                })
                            }
                        )
                    }
                }
                if (recurrence != RecurrenceType.NONE) {
                    Text(
                        "Nhắc nhở sẽ tự động áp dụng cho mỗi lần lặp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Related person
                if (persons.isNotEmpty()) {
                    Text("Người liên quan:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = relatedPersonId == null,
                            onClick = { relatedPersonId = null },
                            label = { Text("Không") }
                        )
                        persons.forEach { p ->
                            FilterChip(
                                selected = relatedPersonId == p.id,
                                onClick = { relatedPersonId = if (relatedPersonId == p.id) null else p.id },
                                label = { Text(p.fullName) }
                            )
                        }
                    }
                }

                // Related event
                if (events.isNotEmpty()) {
                    Text("Sự kiện liên quan:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = relatedEventId == null,
                            onClick = { relatedEventId = null },
                            label = { Text("Không") }
                        )
                        events.forEach { ev ->
                            FilterChip(
                                selected = relatedEventId == ev.id,
                                onClick = { relatedEventId = if (relatedEventId == ev.id) null else ev.id },
                                label = { Text(ev.title) }
                            )
                        }
                    }
                }

                // Checklist
                Text("Checklist:", style = MaterialTheme.typography.labelMedium)
                checklist.forEachIndexed { idx, item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.completed,
                            onCheckedChange = { c ->
                                checklist = checklist.toMutableList().also {
                                    it[idx] = item.copy(completed = c, updatedAtEpochMillis = System.currentTimeMillis())
                                }
                            }
                        )
                        Text(item.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = {
                            checklist = checklist.filterIndexed { i, _ -> i != idx }
                        }) {
                            Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = newChecklistText,
                        onValueChange = { newChecklistText = it },
                        placeholder = { Text("Thêm mục...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        enabled = newChecklistText.isNotBlank(),
                        onClick = {
                            val now = System.currentTimeMillis()
                            val newItem = ChecklistItem(
                                id = UUID.randomUUID().toString(),
                                taskId = taskId,
                                text = newChecklistText.trim(),
                                completed = false,
                                sortOrder = checklist.size + 1,
                                createdAtEpochMillis = now,
                                updatedAtEpochMillis = now
                            )
                            checklist = checklist + newItem
                            newChecklistText = ""
                        }
                    ) {
                        Icon(Icons.Default.Add, "Thêm mục")
                    }
                }

                // Reminders
                Text("Nhắc nhở:", style = MaterialTheme.typography.labelMedium)
                if (!hasDue) {
                    Text(
                        "Hạn chót là bắt buộc để cài đặt nhắc nhở.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    reminderRules.forEachIndexed { rIdx, rule ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Switch(
                                    checked = rule.enabled,
                                    onCheckedChange = { en ->
                                        reminderRules = reminderRules.toMutableList().also {
                                            it[rIdx] = rule.copy(enabled = en, updatedAtEpochMillis = System.currentTimeMillis())
                                        }
                                    }
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    val label = if (rule.amount == 0) {
                                        "Đúng ngày ${rule.remindHour.pad()}:${rule.remindMinute.pad()}"
                                    } else {
                                        "Trước ${rule.amount} ${rule.unit.displayName()} lúc ${rule.remindHour.pad()}:${rule.remindMinute.pad()}"
                                    }
                                    Text(label, style = MaterialTheme.typography.bodySmall)
                                    val repeatLabel = when {
                                        rule.repeatMode == ReminderRepeatMode.ONCE -> "Chỉ lần này"
                                        recurrence == RecurrenceType.YEARLY -> "↻ Mỗi năm"
                                        recurrence == RecurrenceType.MONTHLY -> "↻ Mỗi tháng"
                                        else -> null
                                    }
                                    if (repeatLabel != null) {
                                        Text(repeatLabel, style = MaterialTheme.typography.labelSmall, color = if (rule.repeatMode == ReminderRepeatMode.ONCE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                            IconButton(onClick = {
                                reminderRules = reminderRules.filterIndexed { i, _ -> i != rIdx }
                            }) {
                                Icon(Icons.Default.Close, "Xóa nhắc")
                            }
                        }
                    }

                    if (addingReminder) {
                        Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Thêm lần nhắc", style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) {
                                        Field(remAmount, { remAmount = it.filter(Char::isDigit).take(3) }, "Số lượng", KeyboardType.Number)
                                    }
                                    Box(Modifier.weight(1.5f)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            ReminderOffsetUnit.entries.forEach { u ->
                                                FilterChip(
                                                    selected = remUnit == u,
                                                    onClick = { remUnit = u },
                                                    label = { Text(u.displayName(), style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.weight(1f)) {
                                        Field(remHour, { remHour = it.filter(Char::isDigit).take(2) }, "Giờ", KeyboardType.Number)
                                    }
                                    Box(Modifier.weight(1f)) {
                                        Field(remMinute, { remMinute = it.filter(Char::isDigit).take(2) }, "Phút", KeyboardType.Number)
                                    }
                                }
                                if (recurrence == RecurrenceType.YEARLY || recurrence == RecurrenceType.MONTHLY) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(checked = remRepeatSwitch, onCheckedChange = { remRepeatSwitch = it })
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (recurrence == RecurrenceType.YEARLY) "Nhắc lại mỗi năm" else "Nhắc lại mỗi tháng",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { addingReminder = false }) { Text("Hủy") }
                                    val vAm = remAmount.toIntOrNull()?.let { it >= 0 } == true
                                    val vH = remHour.toIntOrNull() in 0..23
                                    val vM = remMinute.toIntOrNull() in 0..59
                                    TextButton(
                                        enabled = vAm && vH && vM,
                                        onClick = {
                                            val now = System.currentTimeMillis()
                                            val mode = if (recurrence != RecurrenceType.NONE && !remRepeatSwitch) ReminderRepeatMode.ONCE else ReminderRepeatMode.FOLLOW_TARGET
                                            val anchorDate = if (mode == ReminderRepeatMode.ONCE) dueDateTime?.toLocalDate() ?: LocalDate.now() else null
                                            val newRule = ReminderRule(
                                                id = UUID.randomUUID().toString(),
                                                targetType = ReminderTargetType.TASK,
                                                targetId = taskId,
                                                amount = remAmount.toInt(),
                                                unit = remUnit,
                                                remindHour = remHour.toInt(),
                                                remindMinute = remMinute.toInt(),
                                                enabled = true,
                                                createdAtEpochMillis = now,
                                                updatedAtEpochMillis = now,
                                                repeatMode = mode,
                                                occurrenceDate = anchorDate
                                            )
                                            if (reminderRules.none { it.amount == newRule.amount && it.unit == newRule.unit && it.remindHour == newRule.remindHour && it.remindMinute == newRule.remindMinute && it.repeatMode == newRule.repeatMode && it.occurrenceDate == newRule.occurrenceDate }) {
                                                reminderRules = reminderRules + newRule
                                            }
                                            addingReminder = false
                                            remRepeatSwitch = true
                                        }
                                    ) { Text("Thêm") }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(onClick = {
                            remRepeatSwitch = true
                            addingReminder = true
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp))
                            Text("+ Thêm lần nhắc")
                        }
                    }
                }

                Field(note, { note = it }, "Ghi chú")
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val now = System.currentTimeMillis()
                    val completedTime = if (status == TaskStatus.DONE) existing?.completedAt ?: LocalDateTime.now() else null
                    val savedTask = Task(
                        id = existing?.id ?: taskId,
                        title = title.trim(),
                        description = description.blankNull(),
                        startAt = startDateTime,
                        dueAt = dueDateTime,
                        recurrence = recurrence,
                        status = status,
                        priority = priority,
                        relatedEventId = relatedEventId,
                        relatedPersonId = relatedPersonId,
                        completedAt = completedTime,
                        note = note.blankNull(),
                        createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                        updatedAtEpochMillis = now,
                        deletedAtEpochMillis = existing?.deletedAtEpochMillis
                    )
                    onSave(savedTask, checklist, reminderRules, completeSelectedOccurrence)
                }
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun Field(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    type: KeyboardType = KeyboardType.Text
) = OutlinedTextField(
    value = value,
    onValueChange = onValue,
    label = { Text(label) },
    keyboardOptions = KeyboardOptions(keyboardType = type),
    modifier = Modifier.fillMaxWidth()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyGregorianDateField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            onValueChange(DateInputHelper.formatTextFieldValue(value, next))
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        val todayStr = DateInputHelper.formatDate(LocalDate.now())
                        onValueChange(TextFieldValue(todayStr, TextRange(todayStr.length)))
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Hôm nay", fontSize = 11.sp)
                }
                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Chọn ngày trên lịch",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showDatePicker) {
        val parsed = DateInputHelper.parseDate(value.text) ?: LocalDate.now()
        val initialEpochMillis = parsed.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            val pickedDate = Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            val pickedStr = DateInputHelper.formatDate(pickedDate)
                            onValueChange(TextFieldValue(pickedStr, TextRange(pickedStr.length)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Alias for backwards compatibility
@Composable
private fun DateInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) = FamilyGregorianDateField(value, onValueChange, label, modifier)

private fun String.blankNull() = trim().takeIf(String::isNotBlank)
private fun Int.pad() = toString().padStart(2, '0')

fun PersonGroup.displayName(): String = when (this) {
    PersonGroup.FAMILY -> "Gia đình"
    PersonGroup.FRIEND -> "Bạn bè"
    PersonGroup.COLLEAGUE -> "Đồng nghiệp"
    PersonGroup.OTHER -> "Khác"
}

fun ReminderOffsetUnit.displayName(): String = when (this) {
    ReminderOffsetUnit.DAY -> "Ngày"
    ReminderOffsetUnit.WEEK -> "Tuần"
    ReminderOffsetUnit.MONTH -> "Tháng"
}

fun TaskStatus.displayName(): String = when (this) {
    TaskStatus.TODO -> "Cần làm"
    TaskStatus.IN_PROGRESS -> "Đang làm"
    TaskStatus.DONE -> "Hoàn thành"
    TaskStatus.CANCELLED -> "Đã hủy"
}

fun TaskPriority.displayName(): String = when (this) {
    TaskPriority.LOW -> "Thấp"
    TaskPriority.NORMAL -> "Bình thường"
    TaskPriority.HIGH -> "Cao"
    TaskPriority.URGENT -> "Khẩn cấp"
}

@Composable
fun FamilyHomeEntry(snapshot: FamilyLocalSnapshot, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val activePersons = snapshot.persons.count { it.deletedAtEpochMillis == null }
    val today = LocalDate.now()
    val now = LocalDateTime.now()
    val resolver = remember { ImportantEventOccurrenceResolver(AstronomicalVietnameseLunarCalendar()) }

    val nextEventOrBirthday = remember(snapshot, today) {
        resolver.allUpcoming(snapshot.persons, snapshot.events, today, snapshot.categories)
            .filter { it.daysUntil >= 0 }
            .minByOrNull { it.occurrenceDateSolar }
    }

    val nextTask = remember(snapshot, now) {
        TaskOccurrenceResolver.allOccurrences(snapshot.tasks, now, snapshot.taskOccurrenceCompletions)
            .filter { !it.isCompleted && it.occurrenceDueAt != null && it.daysUntil != null && it.daysUntil >= 0 }
            .minByOrNull { it.occurrenceDueAt!! }
    }

    val subtitle = buildString {
        append("$activePersons người")
        val taskDays = nextTask?.daysUntil
        val eventDays = nextEventOrBirthday?.daysUntil

        if (nextEventOrBirthday != null && (taskDays == null || (eventDays != null && eventDays <= taskDays))) {
            append(" · ")
            val icon = if (nextEventOrBirthday.sourceType == EventSourceType.BIRTHDAY) "🎂 Sinh nhật" else "${nextEventOrBirthday.iconKey ?: "📌"}"
            val daysText = if (nextEventOrBirthday.daysUntil == 0L) "hôm nay" else "còn ${nextEventOrBirthday.daysUntil} ngày"
            append("$icon ${nextEventOrBirthday.title} $daysText")
        } else if (nextTask != null) {
            append(" · ")
            val daysText = if (nextTask.daysUntil == 0L) "hôm nay" else "còn ${nextTask.daysUntil} ngày"
            append("📋 Hạn \"${nextTask.task.title}\" $daysText")
        }
    }

    ElevatedCard(modifier.clickable(onClick = onOpen)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FamilyRestroom, null)
            Column {
                Text("Ngày đáng nhớ", style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
