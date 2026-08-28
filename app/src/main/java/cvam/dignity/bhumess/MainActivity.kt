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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import cvam.dignity.bhumess.navigation.FolderEntry
import cvam.dignity.bhumess.navigation.SubView
import cvam.dignity.bhumess.screens.BhuHtmlViewerScreen
import cvam.dignity.bhumess.navigation.*
import cvam.dignity.bhumess.screens.*
import cvam.dignity.bhumess.screens.students.*
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

    val subViewStack = remember {
        mutableStateListOf<SubView>(SubView.Main)
    }

    /*
     * Drive Explorer's folder hierarchy must live above
     * DriveExplorerScreen so that navigation inside the
     * explorer survives when another screen is opened.
     */
    var explorerFolderStack by remember {
        mutableStateOf<List<FolderEntry>?>(null)
    }

    val currentSubView by remember {
        derivedStateOf {
            subViewStack.lastOrNull() ?: SubView.Main
        }
    }

    /*
     * System back navigation.
     *
     * Drive Explorer handles its own internal navigation.
     * Other screens simply pop the current destination.
     */
    BackHandler(
        enabled = subViewStack.size > 1 &&
                currentSubView !is SubView.DriveExplorer
    ) {
        subViewStack.removeAt(subViewStack.lastIndex)
    }

    Scaffold(
        topBar = {

            /*
             * Main dashboard gets a simple clean top bar.
             *
             * App drawer / hamburger button removed.
             * Downloads shortcut removed.
             */
            if (currentSubView == SubView.Main) {

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

        /*
         * Main dashboard uses the Scaffold padding.
         *
         * Other screens retain their own top-bar/layout behavior.
         */
        val contentModifier =
            if (currentSubView == SubView.Main) {
                Modifier.padding(innerPadding)
            } else {
                Modifier.padding(
                    top = 0.dp
                )
            }

        Box(
            modifier = contentModifier
                .fillMaxSize()
                .background(
                    androidx.compose.material3.MaterialTheme
                        .colorScheme
                        .background
                )
        ) {

            AnimatedContent(
                targetState = currentSubView,
                label = "Transition"
            ) { view ->

                when (view) {

                    /*
                     * =========================
                     * DASHBOARD
                     * =========================
                     *
                     * StudyResourcesScreen now contains
                     * exactly four circular tools:
                     *
                     * 1. Study Notes
                     * 2. PYQs
                     * 3. Syllabus Hub
                     * 4. Downloads
                     */
                    is SubView.Main -> {

                        StudyResourcesScreen { target ->

                            if (target is SubView.DriveExplorer) {

                                explorerFolderStack = listOf(
                                    FolderEntry(
                                        target.folderId,
                                        target.title
                                    )
                                )
                            }

                            subViewStack.add(target)
                        }
                    }

                    /*
                     * =========================
                     * SCORE CALCULATOR
                     * =========================
                     */
                    is SubView.ScoreCalculator -> {

                        ScoreCalculatorScreen {
                            subViewStack.removeAt(
                                subViewStack.lastIndex
                            )
                        }
                    }

                    /*
                     * =========================
                     * DOWNLOADS
                     * =========================
                     */
                    is SubView.DownloadedFiles -> {

                        DownloadedFilesScreen(
                            onBack = {
                                subViewStack.removeAt(
                                    subViewStack.lastIndex
                                )
                            },

                            onViewFile = {
                                subViewStack.add(it)
                            }
                        )
                    }

                    /*
                     * =========================
                     * DRIVE EXPLORER
                     * =========================
                     */
                    is SubView.DriveExplorer -> {

                        DriveExplorerScreen(

                            initialFolderId = view.folderId,

                            initialFolderName = view.title,

                            initialFolderStack =
                                explorerFolderStack
                                    ?: listOf(
                                        FolderEntry(
                                            view.folderId,
                                            view.title
                                        )
                                    ),

                            onExitExplorer = {

                                explorerFolderStack = null

                                subViewStack.removeAt(
                                    subViewStack.lastIndex
                                )
                            },

                            onOpenDownloads = {

                                subViewStack.add(
                                    SubView.DownloadedFiles
                                )
                            },

                            onFolderStackChanged = {
                                    updatedStack ->

                                explorerFolderStack =
                                    updatedStack
                            },

                            onNavigate = {
                                    target ->

                                subViewStack.add(target)
                            }
                        )
                    }

                    /*
                     * =========================
                     * HTML VIEWER
                     * =========================
                     */
                    is SubView.HtmlViewer -> {

                        BhuHtmlViewerScreen(
                            view.url,
                            view.title
                        ) {

                            subViewStack.removeAt(
                                subViewStack.lastIndex
                            )
                        }
                    }

                    /*
                     * =========================
                     * PDF VIEWER
                     * =========================
                     */
                    is SubView.PdfViewer -> {

                        PdfViewerScreen(
                            view.uri,
                            view.title
                        ) {

                            subViewStack.removeAt(
                                subViewStack.lastIndex
                            )
                        }
                    }

                    /*
                     * =========================
                     * PROFILE
                     * =========================
                     *
                     * Still supported internally.
                     * It is simply no longer exposed
                     * through the app drawer.
                     */
                    is SubView.Profile -> {

                        StudentProfileScreen {

                            subViewStack.removeAt(
                                subViewStack.lastIndex
                            )
                        }
                    }

                    /*
                     * =========================
                     * SETTINGS
                     * =========================
                     *
                     * Still supported internally.
                     */
                    is SubView.Settings -> {

                        SettingsTabScreen {

                            subViewStack.removeAt(
                                subViewStack.lastIndex
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}