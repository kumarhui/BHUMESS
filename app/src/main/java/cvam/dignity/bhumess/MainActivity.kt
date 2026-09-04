package cvam.dignity.bhumess

import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import cvam.dignity.bhumess.drive.DriveExplorer
import cvam.dignity.bhumess.navigation.AppDestination
import cvam.dignity.bhumess.pdfViewer.PdfViewerScreen
import cvam.dignity.bhumess.screens.BhuHtmlViewerScreen
import cvam.dignity.bhumess.ui.theme.BHUMESSTheme
import java.io.File

class MainActivity : FragmentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager

    private val updateLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != RESULT_OK) {
                // User cancelled the update or update failed.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Runs only once for migration version 1.
        cleanupLegacyDownloadsOnce()

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdate()

        setContent {
            BHUMESSTheme {
                MainApp()
            }
        }
    }

    /**
     * Removes downloads created by the old BHUMESS downloader.
     *
     * This is deliberately a ONE-TIME migration:
     * - Existing users: old/corrupt downloads are removed once.
     * - Future launches: nothing is deleted.
     * - Future app updates: increase cleanupVersion only if another
     *   intentional migration is needed.
     */
    private fun cleanupLegacyDownloadsOnce() {
        val prefs = getSharedPreferences(
            "bhumess_migration",
            Context.MODE_PRIVATE
        )

        val cleanupVersion = 1
        val completedVersion = prefs.getInt(
            "legacy_download_cleanup_version",
            0
        )

        if (completedVersion >= cleanupVersion) return

        try {
            val downloadManager =
                getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val legacyDir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ),
                "BHU Ji/AcademicResources"
            )

            // Remove DownloadManager records whose local URI points to
            // the old BHU Ji/AcademicResources directory.
            val idsToRemove = mutableListOf<Long>()

            downloadManager.query(DownloadManager.Query()).use { cursor ->
                val idIndex =
                    cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                val localUriIndex =
                    cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                if (idIndex >= 0) {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)

                        val localUri =
                            if (localUriIndex >= 0) {
                                cursor.getString(localUriIndex)
                            } else {
                                null
                            }

                        val legacyPath =
                            localUri?.contains(
                                "/Download/BHU%20Ji/AcademicResources/"
                            ) == true ||
                                    localUri?.contains(
                                        "/Download/BHU Ji/AcademicResources/"
                                    ) == true

                        if (legacyPath) {
                            idsToRemove += id
                        }
                    }
                }
            }

            idsToRemove.forEach { id ->
                try {
                    downloadManager.remove(id)
                } catch (e: Exception) {
                    Log.w(
                        "LegacyCleanup",
                        "Could not remove DownloadManager id=$id",
                        e
                    )
                }
            }

            // Remove the old physical files as a fallback.
            if (legacyDir.exists()) {
                legacyDir.listFiles()?.forEach { file ->
                    try {
                        if (file.isDirectory) {
                            file.deleteRecursively()
                        } else {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.w(
                            "LegacyCleanup",
                            "Could not delete ${file.absolutePath}",
                            e
                        )
                    }
                }
            }

            // Do not run this cleanup again on the next launch.
            prefs.edit()
                .putInt(
                    "legacy_download_cleanup_version",
                    cleanupVersion
                )
                .apply()

            Log.i(
                "LegacyCleanup",
                "One-time legacy download cleanup completed."
            )
        } catch (e: Exception) {
            // If cleanup fails completely, do not mark it completed.
            // The next launch gets another chance.
            Log.e(
                "LegacyCleanup",
                "Legacy download cleanup failed.",
                e
            )
        }
    }

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (
                    appUpdateInfo.updateAvailability() ==
                    UpdateAvailability.UPDATE_AVAILABLE &&

                    appUpdateInfo.isUpdateTypeAllowed(
                        AppUpdateOptions.newBuilder(
                            AppUpdateType.IMMEDIATE
                        ).build()
                    )
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(
                            AppUpdateType.IMMEDIATE
                        ).build()
                    )
                }
            }
    }
}

@Composable
fun MainApp() {

    val destinationStack = remember {
        mutableStateListOf<AppDestination>(
            AppDestination.Main
        )
    }

    val currentDestination by remember {
        derivedStateOf {
            destinationStack.lastOrNull()
                ?: AppDestination.Main
        }
    }

    /*
     * IMPORTANT:
     *
     * PDF is displayed as an overlay over DriveExplorer instead of
     * being pushed onto destinationStack.
     *
     * This keeps the existing DriveExplorer composition alive, so its
     * internal folderStack is NOT destroyed when a PDF is opened.
     *
     * Example:
     * Physics -> Sem 1 -> 2024 -> file.pdf
     *
     * Opening the PDF does NOT replace DriveExplorer.
     * Pressing Back simply removes this overlay and reveals 2024 again.
     */
    var pdfOverlay by remember {
        mutableStateOf<AppDestination.PdfViewer?>(null)
    }

    BackHandler(
        enabled = pdfOverlay == null && destinationStack.size > 1
    ) {
        destinationStack.removeAt(
            destinationStack.lastIndex
        )
    }

    Scaffold { _ ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
        ) {

            AnimatedContent(
                targetState = currentDestination,
                label = "Transition"
            ) { destination ->

                when (destination) {

                    AppDestination.Main -> {
                        DashboardScreen(
                            onNavigate = { destinationToOpen ->

                                /*
                                 * Starting a NEW DriveExplorer from the
                                 * dashboard is a new browsing session.
                                 */
                                if (
                                    destinationToOpen
                                            is AppDestination.DriveExplorer
                                ) {
                                    pdfOverlay = null
                                }

                                destinationStack.add(
                                    destinationToOpen
                                )
                            }
                        )
                    }

                    AppDestination.ScoreCalculator -> {
                        ScoreCalculatorScreen(
                            onBack = {
                                if (destinationStack.size > 1) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            }
                        )
                    }

                    AppDestination.DownloadedFiles -> {
                        DownloadedFilesScreen(
                            onBack = {
                                if (destinationStack.size > 1) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            },
                            onViewFile = {
                                destinationStack.add(it)
                            }
                        )
                    }

                    is AppDestination.DriveExplorer -> {

                        DriveExplorer(
                            initialFolderId = destination.folderId,
                            initialFolderName = destination.title,

                            onBack = {
                                if (destinationStack.size > 1) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            },

                            onOpenDownloads = {
                                /*
                                 * PDF overlay is only relevant while
                                 * DriveExplorer is visible.
                                 */
                                pdfOverlay = null

                                destinationStack.add(
                                    AppDestination.DownloadedFiles
                                )
                            },

                            onOpenPdf = { uri, title ->

                                /*
                                 * DO NOT:
                                 *
                                 * destinationStack.add(
                                 *     AppDestination.PdfViewer(...)
                                 * )
                                 *
                                 * That destroys DriveExplorer's local
                                 * folder state.
                                 *
                                 * Instead show PDF above DriveExplorer.
                                 */
                                pdfOverlay =
                                    AppDestination.PdfViewer(
                                        uri = uri,
                                        title = title
                                    )
                            }
                        )
                    }

                    is AppDestination.HtmlViewer -> {
                        BhuHtmlViewerScreen(
                            url = destination.url,
                            title = destination.title,
                            onBack = {
                                if (destinationStack.size > 1) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            }
                        )
                    }

                    /*
                     * Normally PdfViewer is not pushed into the stack
                     * anymore. This branch is kept for safety in case
                     * another part of the app opens it as a destination.
                     */
                    is AppDestination.PdfViewer -> {
                        PdfViewerScreen(
                            uri = destination.uri,
                            title = destination.title,
                            onBack = {
                                if (destinationStack.size > 1) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            }
                        )
                    }
                }
            }

            /*
             * PDF overlay.
             *
             * DriveExplorer stays underneath this Box and therefore
             * retains its current folder stack.
             */
            pdfOverlay?.let { pdf ->

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background
                        )
                ) {
                    PdfViewerScreen(
                        uri = pdf.uri,
                        title = pdf.title,
                        onBack = {
                            pdfOverlay = null
                        }
                    )
                }
            }
        }
    }
}
