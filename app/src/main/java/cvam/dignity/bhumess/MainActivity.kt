package cvam.dignity.bhumess

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import cvam.dignity.bhumess.drive.DriveExplorer
import cvam.dignity.bhumess.navigation.AppDestination
import cvam.dignity.bhumess.pdfViewer.PdfViewerScreen
import cvam.dignity.bhumess.screens.BhuHtmlViewerScreen
import cvam.dignity.bhumess.ui.theme.BHUMESSTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BHUMESSTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val destinationStack = remember {
        mutableStateListOf<AppDestination>(AppDestination.Main)
    }

    val currentDestination by remember {
        derivedStateOf {
            destinationStack.lastOrNull() ?: AppDestination.Main
        }
    }

    BackHandler(enabled = destinationStack.size > 1) {
        destinationStack.removeAt(destinationStack.lastIndex)
    }

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
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                label = "Transition"
            ) { destination ->

                when (destination) {
                    AppDestination.Main -> {
                        DashboardScreen(
                            onNavigate = { destinationStack.add(it) }
                        )
                    }

                    AppDestination.ScoreCalculator -> {
                        ScoreCalculatorScreen(
                            onBack = {
                                if (destinationStack.size > 1) {
                                    destinationStack.removeAt(destinationStack.lastIndex)
                                }
                            }
                        )
                    }

                    AppDestination.DownloadedFiles -> {
                        DownloadedFilesScreen(
                            onBack = {
                                if (destinationStack.size > 1) {
                                    destinationStack.removeAt(destinationStack.lastIndex)
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
                                    destinationStack.removeAt(destinationStack.lastIndex)
                                }
                            },
                            onOpenDownloads = {
                                destinationStack.add(AppDestination.DownloadedFiles)
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

                    is AppDestination.HtmlViewer -> {
                        BhuHtmlViewerScreen(
                            destination.url,
                            destination.title
                        ) {
                            if (destinationStack.size > 1) {
                                destinationStack.removeAt(destinationStack.lastIndex)
                            }
                        }
                    }

                    is AppDestination.PdfViewer -> {
                        PdfViewerScreen(
                            uri = destination.uri,
                            title = destination.title,
                            onBack = {
                                if (destinationStack.size > 1) {
                                    destinationStack.removeAt(destinationStack.lastIndex)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
