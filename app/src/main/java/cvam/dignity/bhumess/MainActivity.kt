package cvam.dignity.bhumess

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvam.dignity.bhumess.navigation.*
import cvam.dignity.bhumess.screens.*
import cvam.dignity.bhumess.screens.students.*
import cvam.dignity.bhumess.ui.theme.BHUMESSTheme
import kotlinx.coroutines.launch

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
    val subViewStack = remember { mutableStateListOf<SubView>(SubView.Main) }
    val currentSubView by remember { derivedStateOf { subViewStack.lastOrNull() ?: SubView.Main } }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val layoutDirection = LocalLayoutDirection.current

    BackHandler(enabled = subViewStack.size > 1) {
        subViewStack.removeAt(subViewStack.size - 1)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentSubView == SubView.Main,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp).background(
                        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)))
                    ).padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AccountCircle, null, Modifier.size(40.dp), tint = Color.White) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("BHU Student Portal", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("Vidyaya'mritamashnute", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text("My Profile", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; subViewStack.add(SubView.Profile) },
                    icon = { Icon(Icons.Rounded.Person, null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; subViewStack.add(SubView.Settings) },
                    icon = { Icon(Icons.Rounded.Settings, null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.weight(1f))
                Text("Made with ❤️ for BHU Students", modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentSubView == SubView.Main) {
                    CenterAlignedTopAppBar(
                        title = { Text("Bhu Ji", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Rounded.Menu, null) } },
                        actions = {
                            IconButton(onClick = { subViewStack.add(SubView.DownloadedFiles) }) {
                                Icon(Icons.Rounded.CloudDone, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            val contentModifier = if (currentSubView == SubView.Main) Modifier.padding(innerPadding) else Modifier.padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding(),
                top = 0.dp
            )

            Box(contentModifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AnimatedContent(targetState = currentSubView, label = "Transition") { view ->
                    when (view) {
                        is SubView.Main -> StudyResourcesScreen { target -> subViewStack.add(target) }
                        is SubView.ScoreCalculator -> ScoreCalculatorScreen { subViewStack.removeAt(subViewStack.size - 1) }
                        is SubView.DownloadedFiles -> DownloadedFilesScreen(
                            onBack = { subViewStack.removeAt(subViewStack.size - 1) },
                            onViewFile = { subViewStack.add(it) }
                        )
                        is SubView.DriveExplorer -> DriveExplorerScreen(
                            view.folderId, view.title,
                            { subViewStack.removeAt(subViewStack.size - 1) },
                            { subViewStack.add(SubView.DownloadedFiles) },
                            { subViewStack.add(it) }
                        )
                        is SubView.HtmlViewer -> BhuHtmlViewerScreen(view.url, view.title) { subViewStack.removeAt(subViewStack.size - 1) }
                        is SubView.PdfViewer -> PdfViewerScreen(view.uri, view.title) { subViewStack.removeAt(subViewStack.size - 1) }
                        is SubView.Profile -> StudentProfileScreen { subViewStack.removeAt(subViewStack.size - 1) }
                        is SubView.Settings -> SettingsTabScreen { subViewStack.removeAt(subViewStack.size - 1) }
                        else -> Unit
                    }
                }
            }
        }
    }
}