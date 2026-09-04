package cvam.dignity.bhumess

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
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
import androidx.core.content.FileProvider
import cvam.dignity.bhumess.navigation.AppDestination
import java.io.File

private fun deleteDownloadedFile(
    context: Context,
    file: File
): Boolean {
    if (!file.exists()) return true

    val dm =
        context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager

    try {
        dm.query(
            DownloadManager.Query()
        ).use { cursor ->

            if (cursor.moveToFirst()) {

                val idIndex =
                    cursor.getColumnIndex(
                        DownloadManager.COLUMN_ID
                    )

                val localUriIndex =
                    cursor.getColumnIndex(
                        DownloadManager.COLUMN_LOCAL_URI
                    )

                val mediaProviderUriIndex =
                    cursor.getColumnIndex(
                        DownloadManager.COLUMN_MEDIAPROVIDER_URI
                    )

                val titleIndex =
                    cursor.getColumnIndex(
                        DownloadManager.COLUMN_TITLE
                    )

                do {
                    val id =
                        if (idIndex >= 0) {
                            cursor.getLong(idIndex)
                        } else {
                            -1L
                        }

                    val localUri =
                        if (localUriIndex >= 0) {
                            cursor.getString(localUriIndex)
                        } else {
                            null
                        }

                    val mediaProviderUri =
                        if (mediaProviderUriIndex >= 0) {
                            cursor.getString(mediaProviderUriIndex)
                        } else {
                            null
                        }

                    val title =
                        if (titleIndex >= 0) {
                            cursor.getString(titleIndex)
                        } else {
                            null
                        }

                    val fileUri =
                        file.toURI().toString()

                    val sameFile =
                        title == file.name ||
                                localUri == fileUri ||
                                localUri?.endsWith(
                                    "/${Uri.encode(file.name)}"
                                ) == true ||
                                localUri?.endsWith(
                                    "/${file.name}"
                                ) == true ||
                                mediaProviderUri?.endsWith(
                                    "/${Uri.encode(file.name)}"
                                ) == true ||
                                mediaProviderUri?.endsWith(
                                    "/${file.name}"
                                ) == true

                    if (id >= 0L && sameFile) {
                        /*
                         * DownloadManager.remove() also removes the
                         * DownloadManager/MediaProvider record.
                         */
                        dm.remove(id)
                    }

                } while (cursor.moveToNext())
            }
        }

    } catch (e: Exception) {
        android.util.Log.w(
            "DownloadsScreen",
            "Could not remove DownloadManager record",
            e
        )
    }

    /*
     * Fallback for old files whose DownloadManager record no longer exists.
     */
    return try {
        if (file.exists()) {
            file.delete()
        }

        !file.exists()

    } catch (e: Exception) {
        android.util.Log.e(
            "DownloadsScreen",
            "Could not delete ${file.absolutePath}",
            e
        )
        false
    }
}

/**
 * Offline Library / Downloaded Files
 *
 * Shows PDFs downloaded to:
 * Download/BHU Ji/AcademicResources
 *
 * PDFs are opened through FileProvider so the PDF viewer
 * receives a content:// URI instead of a raw filesystem path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedFilesScreen(
    onBack: () -> Unit,
    onViewFile: (AppDestination.PdfViewer) -> Unit
) {
    val context = LocalContext.current

    var downloadedFiles by remember {
        mutableStateOf<List<File>>(emptyList())
    }

    var fileToDelete by remember {
        mutableStateOf<File?>(null)
    }

    // Refresh files whenever this screen is entered.
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

    // Delete confirmation dialog
    fileToDelete?.let { file ->

        AlertDialog(
            onDismissRequest = {
                fileToDelete = null
            },

            title = {
                Text(
                    "Delete Resource?",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    "Are you sure you want to permanently delete '${file.name}'?"
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        val deleted = deleteDownloadedFile(context, file)

                        if (deleted) {
                            downloadedFiles =
                                downloadedFiles.filter {
                                    it.absolutePath != file.absolutePath
                                }
                        }

                        fileToDelete = null
                    },

                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Text(
                        "Delete",
                        fontWeight = FontWeight.Black
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        fileToDelete = null
                    }
                ) {
                    Text(
                        "Cancel",
                        color = Color.Gray
                    )
                }
            },

            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {

            Box(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {

                CenterAlignedTopAppBar(

                    title = {
                        Text(
                            "Offline Library",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    },

                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },

                    colors =
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                )
            }
        },

        containerColor = Color(0xFFF8FAFC)

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            // Empty state
            if (downloadedFiles.isEmpty()) {

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Rounded.PictureAsPdf,

                        contentDescription = null,

                        modifier = Modifier.size(64.dp),

                        tint =
                            Color.LightGray.copy(alpha = 0.5f)
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        "No downloaded files found.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

            } else {

                LazyColumn(

                    modifier = Modifier.fillMaxSize(),

                    contentPadding =
                        PaddingValues(16.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)

                ) {

                    items(
                        items = downloadedFiles,
                        key = { it.absolutePath }
                    ) { file ->

                        DownloadedFileRow(

                            file = file,

                            onClick = {

                                // Only PDF files are opened
                                // in the PDF viewer.
                                if (
                                    file.extension.equals(
                                        "pdf",
                                        ignoreCase = true
                                    )
                                ) {

                                    /*
                                     * IMPORTANT:
                                     *
                                     * Do NOT pass file.absolutePath.
                                     *
                                     * Bouquet/PdfRenderer needs a readable
                                     * content URI.
                                     *
                                     * FileProvider converts:
                                     *
                                     * /storage/emulated/0/Download/...
                                     *
                                     * into:
                                     *
                                     * content://cvam.dignity.bhumess.provider/...
                                     */
                                    val pdfUri =
                                        FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )

                                    // Give the PDF viewer the provider URI.
                                    onViewFile(
                                        AppDestination.PdfViewer(
                                            uri = pdfUri.toString(),
                                            title = file.name
                                        )
                                    )
                                }
                            },

                            onDelete = {
                                fileToDelete = file
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadedFileRow(
    file: File,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    Surface(
        onClick = onClick,

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        color = Color.White,

        shadowElevation = 0.5.dp
    ) {

        Row(
            modifier = Modifier.padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xFFEF4444)
                            .copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Rounded.PictureAsPdf,

                    contentDescription = null,

                    tint = Color(0xFFEF4444),

                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = file.name,

                    fontWeight = FontWeight.Bold,

                    fontSize = 14.sp,

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text =
                        "${file.length() / 1024} KB • PDF",

                    fontSize = 11.sp,

                    color = Color.Gray
                )
            }

            IconButton(
                onClick = onDelete
            ) {

                Icon(
                    imageVector =
                        Icons.Rounded.Delete,

                    contentDescription =
                        "Delete",

                    tint =
                        Color.LightGray.copy(
                            alpha = 0.6f
                        ),

                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
