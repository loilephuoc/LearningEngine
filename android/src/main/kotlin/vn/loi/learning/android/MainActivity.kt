package vn.loi.learning.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import vn.loi.learning.android.study.*
import vn.loi.learning.android.ui.LearningEngineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                val navController = rememberNavController()
                NavHost(
                    navController,
                    startDestination = if (state is AndroidStudyState.Home) "home" else "study"
                ) {
                    composable("home") {
                        val home = state as? AndroidStudyState.Home ?: return@composable
                        HomeScreen(home) { event ->
                            studyViewModel.onEvent(event)
                            if (event is AndroidStudyEvent.Start || event == AndroidStudyEvent.Resume) {
                                navController.navigate("study")
                            }
                        }
                    }
                    composable("study") {
                        BackHandler {
                            studyViewModel.onEvent(AndroidStudyEvent.Home)
                            navController.navigate("home") { popUpTo("study") { inclusive = true } }
                        }
                        StudyScreen(state, onEvent = { event ->
                            studyViewModel.onEvent(event)
                            if (event == AndroidStudyEvent.Home) {
                                navController.navigate("home") { popUpTo("study") { inclusive = true } }
                            }
                        })
                    }
                }
            }
        }
    }
}
