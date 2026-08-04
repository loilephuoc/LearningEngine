package vn.loi.learning.android

import android.os.Bundle
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import vn.loi.learning.android.study.*
import vn.loi.learning.android.platform.*
import vn.loi.learning.android.ui.LearningEngineTheme
import vn.loi.learning.android.ui.*
import vn.loi.learning.android.library.*
import vn.loi.learning.domain.library.model.InstalledPackageId
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LearningEngineAndroidApplication
        AndroidStartupTrace.mark("set_content_reached")
        setContent {
            val themeMode by app.themeController.mode.collectAsStateWithLifecycle()
            LearningEngineTheme(mode = themeMode) {
                LaunchedEffect(Unit){AndroidStartupTrace.mark("first_composition_reached");withFrameNanos{AndroidStartupTrace.mark("first_frame_committed")}}
                var graphRetry by rememberSaveable { mutableIntStateOf(0) }
                val rootState by produceState<AndroidRootState>(AndroidRootState.Bootstrapping, graphRetry) {
                    value=withContext(Dispatchers.IO) {
                        runCatching { AndroidRootState.Ready(app.graph) }
                            .getOrElse { error ->
                                AndroidStartupTrace.write(true,"phase=root_shell_state state=FAILURE type=${error.javaClass.simpleName}")
                                AndroidRootState.Failed("Your learning data could not be opened safely.")
                            }
                    }
                }
                val graph=(rootState as? AndroidRootState.Ready)?.graph
                LaunchedEffect(rootState::class) {
                    AndroidStartupTrace.write(false,"phase=root_shell_state state=${rootState.javaClass.simpleName} thread=${Thread.currentThread().name}")
                }
                if(graph==null) {
                    when(val root=rootState) {
                        AndroidRootState.Bootstrapping -> AndroidStartupShell()
                        is AndroidRootState.Ready -> AndroidFeatureLoading("Opening Learning Engine")
                        is AndroidRootState.Failed -> AndroidRootFailure(root) { graphRetry += 1 }
                    }
                } else {
                val studyViewModel = viewModel<AndroidStudyViewModel> {
                    AndroidStudyViewModel(
                        AndroidStudyFacade(graph.engine, resolveMedia = { reference ->
                            graph.media.resolve(reference)?.toString()
                        }),
                        createSavedStateHandle()
                    )
                }
                val state = studyViewModel.state.collectAsStateWithLifecycle().value
                val contentViewModel = viewModel<AndroidContentViewModel> {
                    AndroidContentViewModel(AndroidContentOperations(graph), createSavedStateHandle())
                }
                val contentState = contentViewModel.state.collectAsStateWithLifecycle().value
                val libraryViewModel = viewModel<AndroidLibraryViewModel> {
                    AndroidLibraryViewModel(AndroidLibraryFacade(graph.engine), createSavedStateHandle())
                }
                val libraryState = libraryViewModel.state.collectAsStateWithLifecycle().value
                val packageOperations=remember { AndroidPackageOperations(graph) }
                val operationScope=rememberCoroutineScope()
                var packageActionId by rememberSaveable { mutableStateOf<String?>(null) }
                var packageOperationMessage by remember { mutableStateOf<String?>(null) }
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri == null) contentViewModel.cancel() else {
                        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        val name = runCatching {
                            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) cursor.getString(0) else null
                            }
                        }.getOrNull() ?: "package.opd3"
                        contentViewModel.importDocument(name) { contentResolver.openInputStream(uri) }
                    }
                }
                val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                    if (uri == null) contentViewModel.cancel() else contentViewModel.createBackup { contentResolver.openOutputStream(uri, "wt") }
                }
                val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri == null) contentViewModel.cancel() else contentViewModel.restoreBackup { contentResolver.openInputStream(uri) }
                }
                val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                    val id=packageActionId;packageActionId=null
                    if(uri!=null&&id!=null) operationScope.launch { packageOperationMessage=packageOperations.export(InstalledPackageId(id)){contentResolver.openOutputStream(uri,"wt")}.message() }
                }
                val verifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if(uri!=null) operationScope.launch { packageOperationMessage=packageOperations.verify{contentResolver.openInputStream(uri)}.message() }
                }
                LaunchedEffect(contentState) {
                    if (contentState is AndroidContentOperationState.Succeeded) {
                        studyViewModel.onEvent(AndroidStudyEvent.Home)
                        libraryViewModel.reload()
                    }
                }
                val navController = rememberNavController()
                LaunchedEffect(libraryState) {
                    if (libraryState is AndroidLibraryState.StudyStarted) {
                        studyViewModel.onEvent(AndroidStudyEvent.OpenSession(libraryState.sessionId))
                        libraryViewModel.consumeStudyStarted(libraryState.sessionId)
                    }
                }
                val currentRoute=AndroidRootDestination.fromRoute(navController.currentBackStackEntryAsState().value?.destination?.route).route
                LaunchedEffect(currentRoute) {
                    AndroidStartupTrace.write(false,"phase=destination_changed destination=$currentRoute thread=${Thread.currentThread().name}")
                }
                LaunchedEffect(state) {
                    if ((state is AndroidStudyState.Runtime || state is AndroidStudyState.Completion || state is AndroidStudyState.Failed) && currentRoute != "study") {
                        navController.navigate("study") { launchSingleTop = true }
                    }
                }
                val showRootNavigation = when(currentRoute) {
                    "library" -> libraryState is AndroidLibraryState.Root
                    "study" -> state is AndroidStudyState.Home
                    else -> true
                }
                Scaffold(bottomBar={if(showRootNavigation)AndroidRootNavigation(currentRoute){destination->navController.navigate(destination.route){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}}}) { innerPadding -> NavHost(
                    navController,
                    startDestination = if (state is AndroidStudyState.Runtime || state is AndroidStudyState.Completion) "study" else "home",
                    modifier = androidx.compose.ui.Modifier.padding(innerPadding)
                ) {
                    composable("home", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        val home = state as? AndroidStudyState.Home
                        if(home==null){
                            when(state) {
                                AndroidStudyState.Loading -> AndroidFeatureLoading("Preparing your learning overview")
                                is AndroidStudyState.Failed -> AndroidFeatureFailure("Learning overview unavailable",state.message,
                                    if (state.retryable) ({ studyViewModel.onEvent(AndroidStudyEvent.Retry) }) else null)
                                else -> AndroidFeatureLoading("Opening Study")
                            }
                            return@composable
                        }
                        BackHandler(enabled = contentState is AndroidContentOperationState.Running) {
                            contentViewModel.cancel()
                        }
                        HomeScreen(home, contentState, onEvent = { event ->
                                studyViewModel.onEvent(event)
                            }, onLibrary = { navController.navigate("library") }, onReview = { navController.navigate("review") }, onContentDismiss = contentViewModel::cancel, onContentAction = { kind ->
                                contentViewModel.begin(kind)
                                when (kind) {
                                    AndroidOperationKind.IMPORT -> importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/json"))
                                    AndroidOperationKind.BACKUP -> backupLauncher.launch("learning-engine-backup.lebak")
                                    AndroidOperationKind.RESTORE -> restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                }
                            }
                        )
                    }
                    composable("library", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        LibraryScreen(libraryState, libraryViewModel::openPackage, libraryViewModel::search,
                            libraryViewModel::globalSearch, libraryViewModel::openSearchResult, libraryViewModel::select, libraryViewModel::beginEdit,
                            libraryViewModel::updateDraft, libraryViewModel::saveEdit, libraryViewModel::openLessons,
                            libraryViewModel::startPackage, libraryViewModel::startLesson, libraryViewModel::startSelected,
                            libraryViewModel::back, libraryViewModel::reload, resolveMedia={ graph.media.resolve(it)?.toString() },
                            operationMessage=packageOperationMessage,onExport={id->packageActionId=id;exportLauncher.launch("${id}.opd3")},
                            onVerify={verifyLauncher.launch(arrayOf("application/zip","application/octet-stream"))},onUninstall={id->operationScope.launch { packageOperationMessage=packageOperations.uninstall(InstalledPackageId(id)).message();libraryViewModel.back() }},
                            contentState=contentState,onImport={contentViewModel.begin(AndroidOperationKind.IMPORT);importLauncher.launch(arrayOf("application/zip","application/octet-stream","application/json"))},
                            onFilter=libraryViewModel::filter,onOpenCollection=libraryViewModel::openCollection)
                    }
                    composable("study", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        if(state is AndroidStudyState.Home) StudyHub(state, { event -> studyViewModel.onEvent(event) }, onLibrary={navController.navigate("library")})
                        else {
                        BackHandler {
                            studyViewModel.onEvent(AndroidStudyEvent.Home)
                            navController.navigate("home") { popUpTo("study") { inclusive = true } }
                        }
                        StudyScreen(state, onEvent = { event ->
                            studyViewModel.onEvent(event)
                            if (event == AndroidStudyEvent.Home) {
                                navController.navigate("home") { popUpTo("study") { inclusive = true } }
                            }
                        }) }
                    }
                    composable("review", enterTransition={fadeIn()},exitTransition={fadeOut()}) {
                        val home=state as? AndroidStudyState.Home
                        if(home==null) {
                            when(state) {
                                AndroidStudyState.Loading -> AndroidFeatureLoading("Preparing Review")
                                is AndroidStudyState.Failed -> AndroidFeatureFailure("Review unavailable",state.message,
                                    if (state.retryable) ({ studyViewModel.onEvent(AndroidStudyEvent.Retry) }) else null)
                                else -> AndroidFeatureLoading("Opening Study")
                            }
                        } else ReviewHub(home) { event->studyViewModel.onEvent(event) }
                    }
                    composable("settings", enterTransition={fadeIn()},exitTransition={fadeOut()}) {
                        SettingsScreen(themeMode,app.themeController::setMode) { kind->contentViewModel.begin(kind);when(kind){AndroidOperationKind.IMPORT->importLauncher.launch(arrayOf("application/zip","application/octet-stream","application/json"));AndroidOperationKind.BACKUP->backupLauncher.launch("learning-engine-backup.lebak");AndroidOperationKind.RESTORE->restoreLauncher.launch(arrayOf("application/zip","application/octet-stream"))} }
                    }
                } } }
            }
        }
    }
}

private fun AndroidPackageOperationResult.message()=when(this){is AndroidPackageOperationResult.Success->message;is AndroidPackageOperationResult.Failed->message;is AndroidPackageOperationResult.Verification->if(valid)"Package verification passed." else "Package verification failed: ${errors.joinToString()}"}
