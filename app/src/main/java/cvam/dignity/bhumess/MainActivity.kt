package cvam.dignity.bhumess

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

class MainActivity : FragmentActivity() {

    // =========================================================
    // Google Play In-App Update
    // =========================================================

    private lateinit var appUpdateManager: AppUpdateManager

    private val updateLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode != RESULT_OK) {
                // User cancelled the update or the update failed.
                // No action required here.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Initialize Google Play update manager
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // Check for available update
        checkForAppUpdate()

        setContent {
            BHUMESSTheme {
                MainApp()
            }
        }
    }

    // =========================================================
    // Check Google Play for App Update
    // =========================================================

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


// =============================================================
// Main App Navigation
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
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

    // =========================================================
    // Back Navigation
    // =========================================================

    BackHandler(
        enabled = destinationStack.size > 1
    ) {
        destinationStack.removeAt(
            destinationStack.lastIndex
        )
    }

    // =========================================================
    // Main Scaffold
    // =========================================================

    Scaffold(

        topBar = {

            if (currentDestination == AppDestination.Main) {

                CenterAlignedTopAppBar(

                    title = {
                        Text(
                            text = "BHUMESS",
                            fontWeight = FontWeight.Black
                        )
                    }
                )
            }
        }

    ) { innerPadding ->

        val contentModifier =
            if (currentDestination == AppDestination.Main) {

                Modifier.padding(innerPadding)

            } else {

                Modifier
            }

        Box(
            modifier = contentModifier
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

                    // =================================================
                    // HOME
                    // =================================================

                    AppDestination.Main -> {

                        DashboardScreen(
                            onNavigate = {
                                destinationStack.add(it)
                            }
                        )
                    }


                    // =================================================
                    // SCORE CALCULATOR
                    // =================================================

                    AppDestination.ScoreCalculator -> {

                        ScoreCalculatorScreen(
                            onBack = {

                                if (
                                    destinationStack.size > 1
                                ) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            }
                        )
                    }


                    // =================================================
                    // DOWNLOADED FILES
                    // =================================================

                    AppDestination.DownloadedFiles -> {

                        DownloadedFilesScreen(

                            onBack = {

                                if (
                                    destinationStack.size > 1
                                ) {
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


                    // =================================================
                    // GOOGLE DRIVE EXPLORER
                    // =================================================

                    is AppDestination.DriveExplorer -> {

                        DriveExplorer(

                            initialFolderId =
                                destination.folderId,

                            initialFolderName =
                                destination.title,

                            onBack = {

                                if (
                                    destinationStack.size > 1
                                ) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            },

                            onOpenDownloads = {

                                destinationStack.add(
                                    AppDestination.DownloadedFiles
                                )
                            },

                            onOpenPdf = { uri, title ->

                                destinationStack.add(
                                    AppDestination.PdfViewer(
                                        uri = uri,
                                        title = title
                                    )
                                )
                            }
                        )
                    }


                    // =================================================
                    // HTML / WEB TOOL
                    // =================================================

                    is AppDestination.HtmlViewer -> {

                        BhuHtmlViewerScreen(

                            url = destination.url,

                            title = destination.title,

                            onBack = {

                                if (
                                    destinationStack.size > 1
                                ) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            }
                        )
                    }


                    // =================================================
                    // PDF VIEWER
                    // =================================================

                    is AppDestination.PdfViewer -> {

                        PdfViewerScreen(

                            uri = destination.uri,

                            title = destination.title,

                            onBack = {

                                if (
                                    destinationStack.size > 1
                                ) {
                                    destinationStack.removeAt(
                                        destinationStack.lastIndex
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}