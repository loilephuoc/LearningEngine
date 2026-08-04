package vn.loi.learning.android

import android.os.Bundle
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as LearningEngineAndroidApplication).graph
        setContent {
            LearningEngineTheme {
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
                LaunchedEffect(contentState) {
                    if (contentState is AndroidContentOperationState.Succeeded) studyViewModel.onEvent(AndroidStudyEvent.Home)
                }
                val navController = rememberNavController()
                LaunchedEffect(libraryState) {
                    if (libraryState is AndroidLibraryState.StudyStarted) {
                        studyViewModel.onEvent(AndroidStudyEvent.Resume)
                        navController.navigate("study") { launchSingleTop = true }
                    }
                }
                val currentRoute=navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
                val showRootNavigation = when(currentRoute) {
                    "library" -> libraryState is AndroidLibraryState.Root
                    "study" -> state is AndroidStudyState.Home
                    else -> true
                }
                Scaffold(bottomBar={if(showRootNavigation)AndroidRootNavigation(currentRoute){destination->navController.navigate(destination.route){popUpTo("home"){saveState=true};launchSingleTop=true;restoreState=true}}}) { innerPadding -> NavHost(
                    navController,
                    startDestination = if (state is AndroidStudyState.Home) "home" else "study",
                    modifier = androidx.compose.ui.Modifier.padding(innerPadding)
                ) {
                    composable("home", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        val home = state as? AndroidStudyState.Home ?: return@composable
                        BackHandler(enabled = contentState is AndroidContentOperationState.Running) {
                            contentViewModel.cancel()
                        }
                        HomeScreen(home, contentState, onEvent = { event ->
                                studyViewModel.onEvent(event)
                                if (event is AndroidStudyEvent.Start || event == AndroidStudyEvent.Resume) {
                                    navController.navigate("study")
                                }
                            }, onLibrary = { navController.navigate("library") }, onContentDismiss = contentViewModel::cancel, onContentAction = { kind ->
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
                            libraryViewModel::back, libraryViewModel::reload, resolveMedia={ graph.media.resolve(it)?.toString() })
                    }
                    composable("study", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                        if(state is AndroidStudyState.Home) StudyHub(state) { event -> studyViewModel.onEvent(event);if(event==AndroidStudyEvent.Resume)navController.navigate("study"){launchSingleTop=true} }
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
                        val home=state as? AndroidStudyState.Home ?: return@composable
                        ReviewHub(home) { event->studyViewModel.onEvent(event);navController.navigate("study"){launchSingleTop=true} }
                    }
                    composable("settings", enterTransition={fadeIn()},exitTransition={fadeOut()}) {
                        SettingsScreen { kind->contentViewModel.begin(kind);when(kind){AndroidOperationKind.IMPORT->importLauncher.launch(arrayOf("application/zip","application/octet-stream","application/json"));AndroidOperationKind.BACKUP->backupLauncher.launch("learning-engine-backup.lebak");AndroidOperationKind.RESTORE->restoreLauncher.launch(arrayOf("application/zip","application/octet-stream"))} }
                    }
                } }
            }
        }
    }
}
