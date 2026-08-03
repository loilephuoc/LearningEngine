package vn.loi.learning.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import vn.loi.learning.android.study.AndroidStudyFacade
import vn.loi.learning.android.study.AndroidStudyViewModel
import vn.loi.learning.android.study.StudyScreen
import vn.loi.learning.android.ui.LearningEngineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engineContext = (application as LearningEngineAndroidApplication).graph.engine
        setContent {
            LearningEngineTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "study") {
                    composable("study") {
                        val studyViewModel = viewModel<AndroidStudyViewModel> {
                            AndroidStudyViewModel(AndroidStudyFacade(engineContext), createSavedStateHandle())
                        }
                        StudyScreen(
                            state = studyViewModel.state.collectAsStateWithLifecycle().value,
                            onEvent = studyViewModel::onEvent
                        )
                    }
                }
            }
        }
    }
}
