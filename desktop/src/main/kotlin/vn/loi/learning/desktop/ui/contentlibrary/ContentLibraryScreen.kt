package vn.loi.learning.desktop.ui.contentlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.nio.file.Path
import javax.swing.JFileChooser

@Composable
fun ContentLibraryScreen(
    viewModel: ContentLibraryViewModel,
    onStartLessonStudy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ContentLibraryContent(
        uiState = viewModel.uiState,
        lessonBrowserUiState =
            viewModel.lessonBrowserUiState,
        onRefresh = viewModel::refresh,
        onImportDirectory =
            viewModel::importFromDirectory,
        onOpenLibrary =
            viewModel::openLibrary,
        onCloseLibrary =
            viewModel::closeLibrary,
        onSelectLesson =
            viewModel::selectLesson,
        onClearLessonSelection =
            viewModel::clearLessonSelection,
        onStartLessonStudy =
            onStartLessonStudy,
        modifier = modifier
    )
}

@Composable
private fun ContentLibraryContent(
    uiState: ContentLibraryUiState,
    lessonBrowserUiState: LessonBrowserUiState?,
    onRefresh: () -> Unit,
    onImportDirectory: (Path) -> Unit,
    onOpenLibrary: (String) -> Unit,
    onCloseLibrary: () -> Unit,
    onSelectLesson: (String) -> Unit,
    onClearLessonSelection: () -> Unit,
    onStartLessonStudy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        ContentLibraryHeader(
            packageCount = uiState.packageCount,
            libraryCount = uiState.libraryCount,
            onRefresh = onRefresh,
            onImportDirectory =
                onImportDirectory
        )

        uiState.importMessage?.let { message ->
            ImportMessageCard(
                message = message,
                isError = false
            )
        }

        uiState.importError?.let { message ->
            ImportMessageCard(
                message = message,
                isError = true
            )
        }

        if (uiState.isEmpty) {
            EmptyContentLibrary()
        } else {
            if (
                lessonBrowserUiState == null &&
                uiState.libraries.isNotEmpty()
            ) {
                SectionTitle("Libraries")

                uiState.libraries.forEach { libraryItem ->
                    ContentLibraryCard(
                        libraryItem = libraryItem,
                        onOpen = {
                            onOpenLibrary(
                                libraryItem.id
                            )
                        }
                    )
                }
            }

            lessonBrowserUiState?.let { browserUiState ->
                LessonBrowserCard(
                    uiState = browserUiState,
                    onClose = onCloseLibrary,
                    onSelectLesson =
                        onSelectLesson,
                    onClearLessonSelection =
                        onClearLessonSelection,
                    onStartStudy =
                        onStartLessonStudy
                )
            }

            if (
                lessonBrowserUiState == null &&
                uiState.packages.isNotEmpty()
            ) {
                SectionTitle(
                    "Installed Packages"
                )

                uiState.packages.forEach { packageItem ->
                    ContentPackageCard(
                        packageItem
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentLibraryHeader(
    packageCount: Int,
    libraryCount: Int,
    onRefresh: () -> Unit,
    onImportDirectory: (Path) -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Content Library",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    buildString {
                        append(libraryCount)

                        append(
                            if (libraryCount == 1) {
                                " library"
                            } else {
                                " libraries"
                            }
                        )

                        append(" · ")
                        append(packageCount)
                        append(" installed package")

                        if (packageCount != 1) {
                            append("s")
                        }
                    },
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRefresh
            ) {
                Text("Refresh")
            }

            Button(
                onClick = {
                    choosePackageDirectory()
                        ?.let(onImportDirectory)
                }
            ) {
                Text("Import Package")
            }
        }
    }
}

private fun choosePackageDirectory(): Path? {
    val chooser =
        JFileChooser().apply {
            dialogTitle =
                "Select directory containing .opd3 or .pkg files"

            fileSelectionMode =
                JFileChooser.DIRECTORIES_ONLY

            isAcceptAllFileFilterUsed =
                false
        }

    return if (
        chooser.showOpenDialog(null) ==
        JFileChooser.APPROVE_OPTION
    ) {
        chooser.selectedFile.toPath()
    } else {
        null
    }
}

@Composable
private fun ImportMessageCard(
    message: String,
    isError: Boolean
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isError) {
                        MaterialTheme
                            .colorScheme
                            .errorContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer
                    }
            )
    ) {
        Text(
            text = message,
            modifier =
                Modifier.padding(16.dp),
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                if (isError) {
                    MaterialTheme
                        .colorScheme
                        .onErrorContainer
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
                }
        )
    }
}

@Composable
private fun EmptyContentLibrary() {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors()
    ) {
        Column(
            modifier =
                Modifier.padding(24.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No content libraries",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "Choose Import Package and select a directory containing .opd3 or .pkg files.",
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        style =
            MaterialTheme
                .typography
                .titleLarge,
        fontWeight =
            FontWeight.SemiBold
    )
}

@Composable
private fun ContentLibraryCard(
    libraryItem: ContentLibraryItem,
    onOpen: () -> Unit
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors()
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = libraryItem.name,
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            PackageProperty(
                label = "Contents",
                value =
                    libraryItem
                        .contentCount
                        .toString()
            )

            PackageProperty(
                label = "Learning Items",
                value =
                    libraryItem
                        .learningItemCount
                        .toString()
            )

            PackageProperty(
                label = "Library ID",
                value = libraryItem.id
            )

            Button(
                onClick = onOpen
            ) {
                Text("Open Library")
            }
        }
    }
}

@Composable
private fun ContentPackageCard(
    packageItem: ContentLibraryPackageItem
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors()
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = packageItem.name,
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            PackageProperty(
                label = "Version",
                value = packageItem.version
            )

            PackageProperty(
                label = "Format",
                value = packageItem.format
            )

            PackageProperty(
                label = "Libraries",
                value =
                    packageItem
                        .libraryCount
                        .toString()
            )

            PackageProperty(
                label = "Package ID",
                value = packageItem.id
            )
        }
    }
}

@Composable
private fun PackageProperty(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(
            text = value,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            fontWeight =
                FontWeight.Medium
        )
    }
}