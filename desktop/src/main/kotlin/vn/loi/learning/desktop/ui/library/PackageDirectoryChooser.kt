package vn.loi.learning.desktop.ui.library

import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun choosePackageFile(): Path? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Select Package File (.opd3, .pkg, .json)"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isAcceptAllFileFilterUsed = false
        addChoosableFileFilter(
            FileNameExtensionFilter(
                "Learning Package Files (*.opd3, *.pkg, *.json)",
                "opd3", "pkg", "json"
            )
        )
    }

    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.toPath()
    } else {
        null
    }
}

fun choosePackageDirectory(): Path? = choosePackageFile()
