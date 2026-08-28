package cvam.dignity.bhumess.navigation

sealed class AppDestination {

    data object Main : AppDestination()

    data object ScoreCalculator : AppDestination()

    data class DriveExplorer(
        val folderId: String,
        val title: String
    ) : AppDestination()

    data object DownloadedFiles : AppDestination()

    data class HtmlViewer(
        val url: String,
        val title: String
    ) : AppDestination()

    data class PdfViewer(
        val uri: String,
        val title: String
    ) : AppDestination()
}