package cvam.dignity.bhumess.screens.students

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvam.dignity.bhumess.navigation.SubView
import cvam.dignity.bhumess.utils.FileUtils
import java.io.File

/**
 * Optimized Downloaded Files Screen.
 * Uses statusBarsPadding to fix Top Bar placement and matches the Bhu Ji UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedFilesScreen(
    onBack: () -> Unit,
    onViewFile: (SubView.PdfViewer) -> Unit
) {
    val context = LocalContext.current
    var downloadedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    // Refresh file list on entry
    LaunchedEffect(Unit) {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ),
            "BHU Ji/AcademicResources"
        )

        if (!folder.exists()) {
            folder.mkdirs()
        }

        downloadedFiles =
            folder.listFiles()
                ?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
    }
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Resource?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${fileToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.delete()
                        downloadedFiles = downloadedFiles.filter { it.name != fileToDelete?.name }
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) { Text("Delete", fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel", color = Color.Gray) }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            // Box wrapper handles the system status bar area correctly
            Box(Modifier.background(Color.White).statusBarsPadding()) {
                CenterAlignedTopAppBar(
                    title = { Text("Offline Library", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { p ->
        Box(Modifier.padding(p).fillMaxSize()) {
            if (downloadedFiles.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.PictureAsPdf,
                        null,
                        Modifier.size(64.dp),
                        tint = Color.LightGray.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No downloaded files found.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(downloadedFiles) { file ->
                        DownloadedFileRow(
                            file = file,
                            onClick = {
                                if (file.name.endsWith(".pdf", ignoreCase = true)) {
                                    onViewFile(SubView.PdfViewer(file.absolutePath, file.name))
                                }
                            },
                            onDelete = { fileToDelete = file }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadedFileRow(file: File, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 0.5.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFEF4444).copy(0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${(file.length() / 1024)} KB • PDF", fontSize = 11.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null, tint = Color.LightGray.copy(0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}