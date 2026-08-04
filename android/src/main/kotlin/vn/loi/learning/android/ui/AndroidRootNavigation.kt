package vn.loi.learning.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vn.loi.learning.android.platform.AndroidOperationKind
import vn.loi.learning.android.study.*

enum class AndroidRootDestination(val route:String,val label:String) { HOME("home","Home"),LIBRARY("library","Library"),STUDY("study","Study"),REVIEW("review","Review"),SETTINGS("settings","Settings") }

@Composable fun AndroidRootNavigation(selected:String,onSelect:(AndroidRootDestination)->Unit){NavigationBar{AndroidRootDestination.entries.forEach{destination->NavigationBarItem(selected=selected==destination.route,onClick={onSelect(destination)},icon={Icon(when(destination){AndroidRootDestination.HOME->Icons.Default.Home;AndroidRootDestination.LIBRARY->Icons.AutoMirrored.Filled.MenuBook;AndroidRootDestination.STUDY->Icons.Default.School;AndroidRootDestination.REVIEW->Icons.Default.Refresh;AndroidRootDestination.SETTINGS->Icons.Default.Settings},destination.label)},label={Text(destination.label)})}}}

@Composable fun StudyHub(home:AndroidStudyState.Home,onEvent:(AndroidStudyEvent)->Unit){Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Study",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics{heading()});if(home.availability.canResume)Button(onClick={onEvent(AndroidStudyEvent.Resume)},Modifier.fillMaxWidth()){Text("Continue current session")}else Text("Choose a package or lesson in Library to begin.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}

@Composable fun ReviewHub(home:AndroidStudyState.Home,onEvent:(AndroidStudyEvent)->Unit){Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Review",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics{heading()});val actions=listOf(Triple("Review due items",AndroidSessionEntry.REVIEW,home.availability.canStartReview),Triple("Practice latest session",AndroidSessionEntry.LATEST_SESSION,home.availability.canStartLatestSessionPractice),Triple("Practice Again / Hard",AndroidSessionEntry.DIFFICULT,home.availability.canStartDifficultPractice),Triple("Review learned items",AndroidSessionEntry.LEARNED,home.availability.canStartLearnedReview));actions.filter{it.third}.forEach{action->Button(onClick={onEvent(AndroidStudyEvent.Start(action.second))},Modifier.fillMaxWidth()){Text(action.first)}};if(actions.none{it.third})Text("No review or practice session is available yet. Complete a Study session first.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}

@Composable fun SettingsScreen(onAction:(AndroidOperationKind)->Unit){Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Settings",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics{heading()});Text("Data management",style=MaterialTheme.typography.titleLarge);Button(onClick={onAction(AndroidOperationKind.IMPORT)},Modifier.fillMaxWidth()){Text("Import package")};OutlinedButton(onClick={onAction(AndroidOperationKind.BACKUP)},Modifier.fillMaxWidth()){Text("Create backup")};OutlinedButton(onClick={onAction(AndroidOperationKind.RESTORE)},Modifier.fillMaxWidth()){Text("Restore backup")}}}
