package cvam.dignity.bhumess

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.ui.graphics.vector.ImageVector

data class FolderEntry(
    val id: String,
    val name: String
)

sealed class SubView {

    // Dashboard
    object Main : SubView()

    // Score Calculator
    object ScoreCalculator : SubView()

    // Academic Resources
    data class DriveExplorer(
        val folderId: String,
        val title: String
    ) : SubView()

    object DownloadedFiles : SubView()

    data class PdfViewer(
        val uri: String,
        val title: String
    ) : SubView()

    // Website Viewer
    data class HtmlViewer(
        val url: String,
        val title: String
    ) : SubView()
}

sealed class NavItem(
    val icon: ImageVector,
    val label: String
) {
    object Resources :
        NavItem(Icons.AutoMirrored.Rounded.LibraryBooks, "Study")

    object Calculator :
        NavItem(Icons.Rounded.Calculate, "Score Calculator")
}
