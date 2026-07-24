package vn.loi.learning.desktop.ui.library

import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun choosePackageFiles(): List<Path> {
    val chooser = JFileChooser().apply {
        dialogTitle = "Select Package File (.opd3) or Legacy Pair (.json + .pkg)"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = true
        isAcceptAllFileFilterUsed = false
        addChoosableFileFilter(
            FileNameExtensionFilter(
                "Learning Package Files (*.opd3, *.json, *.pkg)",
                "opd3", "json", "pkg"
            )
        )
    }

    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val selected = chooser.selectedFiles
        if (selected != null && selected.isNotEmpty()) {
            selected.map { it.toPath() }
        } else if (chooser.selectedFile != null) {
            listOf(chooser.selectedFile.toPath())
        } else {
            emptyList()
        }
    } else {
        emptyList()
    }
}

fun choosePackageFile(): Path? = choosePackageFiles().firstOrNull()

fun choosePackageDirectory(): Path? = choosePackageFile()
