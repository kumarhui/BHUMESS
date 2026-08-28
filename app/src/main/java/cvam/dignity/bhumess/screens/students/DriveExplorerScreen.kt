package cvam.dignity.bhumess.screens.students

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.AdListener
import cvam.dignity.bhumess.api.DriveApiService
import cvam.dignity.bhumess.api.DriveFile
import cvam.dignity.bhumess.data.DriveCacheManager
import cvam.dignity.bhumess.navigation.SubView
import cvam.dignity.bhumess.ads.AdConfig
import cvam.dignity.bhumess.utils.NetworkUtils
import kotlinx.coroutines.delay
import java.io.File
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import cvam.dignity.bhumess.navigation.FolderEntry

/*
 * Convert Google Drive file size in bytes
 * into a human-readable format.
 *
 * Examples:
 * 1024        -> 1 KB
 * 1048576     -> 1.0 MB
 * 5242880     -> 5.0 MB
 * 1073741824  -> 1.0 GB
 */

private fun cancelDownload(
    context: Context,
    fileName: String
) {
    val dm = context.getSystemService(
        Context.DOWNLOAD_SERVICE
    ) as DownloadManager

    val query = DownloadManager.Query()
    val cursor = dm.query(query)

    try {
        if (cursor.moveToFirst()) {
            do {
                val titleIndex =
                    cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)

                val idIndex =
                    cursor.getColumnIndex(DownloadManager.COLUMN_ID)

                if (titleIndex >= 0 && idIndex >= 0) {
                    val title = cursor.getString(titleIndex)

                    if (title == fileName) {
                        val downloadId = cursor.getLong(idIndex)
                        dm.remove(downloadId)
                    }
                }
            } while (cursor.moveToNext())
        }
    } finally {
        cursor.close()
    }
}

private fun deleteLocalFile(
    file: File
): Boolean {
    return try {
        !file.exists() || file.delete()
    } catch (e: Exception) {
        Log.e(
            "DriveExplorer",
            "Failed to delete ${file.absolutePath}",
            e
        )
        false
    }
}

private fun formatFileSize(bytes: Long?): String {

    if (bytes == null || bytes <= 0L) {
        return "Size unknown"
    }

    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0

    return when {

        bytes >= gb ->
            String.format("%.1f GB", bytes / gb)

        bytes >= mb ->
            String.format("%.1f MB", bytes / mb)

        bytes >= kb ->
            String.format("%.0f KB", bytes / kb)

        else ->
            "$bytes B"
    }
}


@Composable
private fun AdMobBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val adView = remember {
        AdView(context).apply {
            adUnitId = AdConfig.BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("Ads", "Banner loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(
                        "Ads",
                        "Banner failed: code=${error.code}, " +
                                "message=${error.message}, " +
                                "domain=${error.domain}"
                    )
                }
            }
        }
    }

    DisposableEffect(adView) {
        adView.loadAd(
            AdRequest.Builder().build()
        )

        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier,
        update = { view ->
            view.visibility = android.view.View.VISIBLE
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveExplorerScreen(
    initialFolderId: String,
    initialFolderName: String,
    initialFolderStack: List<FolderEntry>,
    onExitExplorer: () -> Unit,
    onOpenDownloads: () -> Unit,
    onFolderStackChanged: (List<FolderEntry>) -> Unit,
    onNavigate: (SubView) -> Unit
) {

    val context = LocalContext.current

    /*
     * The hierarchy is owned by MainActivity so it survives while
     * PDF Viewer / Downloads is shown above the explorer.
     */
    val folderStack = remember(
        initialFolderId,
        initialFolderName,
        initialFolderStack
    ) {
        mutableStateListOf<FolderEntry>().apply {
            addAll(initialFolderStack)
        }
    }

    var isGoingBack by remember {
        mutableStateOf(false)
    }

    /*
     * File name -> download progress
     * 0f to 1f
     */
    val downloadingFiles = remember {
        mutableStateMapOf<String, Float>()
    }

    val dm = remember {
        context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager
    }

    /*
     * Android back button.
     */
    BackHandler {

        if (folderStack.size > 1) {

            isGoingBack = true

            folderStack.removeAt(
                folderStack.lastIndex
            )

            onFolderStackChanged(
                folderStack.toList()
            )

        } else {

            onExitExplorer()
        }
    }
    /*
     * Poll DownloadManager every second.
     *
     * Updates:
     * - download progress
     * - downloaded state
     */
    LaunchedEffect(downloadingFiles.size) {

        while (downloadingFiles.isNotEmpty()) {

            delay(1000)

            val iterator =
                downloadingFiles.entries.iterator()

            while (iterator.hasNext()) {

                val entry = iterator.next()

                val fileName = entry.key

                val query =
                    DownloadManager.Query()
                        .setFilterByStatus(
                            DownloadManager.STATUS_RUNNING or
                                    DownloadManager.STATUS_PENDING or
                                    DownloadManager.STATUS_SUCCESSFUL
                        )

                val cursor: Cursor =
                    dm.query(query)

                var foundDownload = false

                try {

                    if (cursor.moveToFirst()) {

                        do {

                            val title =
                                cursor.getString(
                                    cursor.getColumnIndexOrThrow(
                                        DownloadManager.COLUMN_TITLE
                                    )
                                )

                            if (title == fileName) {

                                foundDownload = true

                                val status =
                                    cursor.getInt(
                                        cursor.getColumnIndexOrThrow(
                                            DownloadManager.COLUMN_STATUS
                                        )
                                    )

                                when (status) {

                                    DownloadManager.STATUS_SUCCESSFUL -> {

                                        /*
                                         * Download completed.
                                         */
                                        iterator.remove()
                                    }

                                    DownloadManager.STATUS_RUNNING,
                                    DownloadManager.STATUS_PENDING -> {

                                        val bytesDownloaded =
                                            cursor.getLong(
                                                cursor.getColumnIndexOrThrow(
                                                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                                )
                                            )

                                        val bytesTotal =
                                            cursor.getLong(
                                                cursor.getColumnIndexOrThrow(
                                                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                                                )
                                            )

                                        if (bytesTotal > 0) {

                                            downloadingFiles[fileName] =
                                                bytesDownloaded.toFloat() /
                                                        bytesTotal.toFloat()

                                        } else {

                                            downloadingFiles[fileName] =
                                                0f
                                        }
                                    }
                                }
                            }

                        } while (cursor.moveToNext())
                    }

                } finally {

                    cursor.close()
                }

                /*
                 * DownloadManager no longer has
                 * this download.
                 */
                if (
                    !foundDownload &&
                    downloadingFiles.containsKey(fileName)
                ) {

                    try {

                        iterator.remove()

                    } catch (_: Exception) {

                        downloadingFiles.remove(fileName)
                    }
                }
            }
        }
    }


    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {
                    Text(
                        text = folderStack.last().name,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },

                navigationIcon = {

                    IconButton(

                        onClick = {
                            if (folderStack.size > 1) {

                                isGoingBack = true

                                folderStack.removeAt(
                                    folderStack.lastIndex
                                )

                                onFolderStackChanged(
                                    folderStack.toList()
                                )

                            } else {

                                onExitExplorer()
                            }
                        }                ) {

                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = onOpenDownloads
                    ) {

                        Icon(
                            Icons.Rounded.CloudDone,
                            contentDescription = "Downloads",
                            tint = MaterialTheme
                                .colorScheme
                                .primary
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .centerAlignedTopAppBarColors(
                            containerColor =
                                Color.Transparent
                        )
            )
        },

        bottomBar = {

            AdMobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }

    ) { p ->

        Box(

            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF8FAFC)
                )
                .padding(p)
        ) {

            AnimatedContent(
                targetState = folderStack.last().id,

                transitionSpec = {

                    if (isGoingBack) {

                        (
                                slideInHorizontally { -it } + fadeIn()
                                ) togetherWith (
                                slideOutHorizontally { it } + fadeOut()
                                )

                    } else {

                        (
                                slideInHorizontally { it } + fadeIn()
                                ) togetherWith (
                                slideOutHorizontally { -it } + fadeOut()
                                )
                    }
                },

                label = "ExplorerContent"

            ) { currentId ->

                FolderContent(

                    folderId = currentId,

                    onFolderClick = { id, name ->

                        isGoingBack = false

                        folderStack.add(
                            FolderEntry(
                                id = id,
                                name = name
                            )
                        )

                        onFolderStackChanged(
                            folderStack.toList()
                        )
                    },

                    downloadingFiles =
                        downloadingFiles,

                    onNavigate =
                        onNavigate
                )
            }
        }
    }
}


@Composable
fun FolderContent(
    folderId: String,
    onFolderClick: (String, String) -> Unit,
    downloadingFiles: Map<String, Float>,
    onNavigate: (SubView) -> Unit
) {

    val context = LocalContext.current

    val driveService =
        remember {
            DriveApiService(context)
        }

    val dm =
        remember {
            context.getSystemService(
                Context.DOWNLOAD_SERVICE
            ) as DownloadManager
        }

    var files by remember {

        mutableStateOf<List<DriveFile>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }


    /*
     * Load folder.
     *
     * First show cache.
     * Then fetch fresh Drive data.
     */
    LaunchedEffect(folderId) {

        isLoading = true

        val cached =
            DriveCacheManager.getFolderCache(
                context,
                folderId
            )

        if (cached != null) {

            files = cached
            isLoading = false
        }

        if (
            NetworkUtils
                .isInternetAvailable(context)
        ) {

            try {

                val fresh =
                    driveService
                        .fetchFilesFromFolder(
                            folderId
                        )

                files = fresh

                DriveCacheManager
                    .saveFolderCache(
                        context,
                        folderId,
                        fresh
                    )

            } catch (_: Exception) {

                /*
                 * Keep cached files if
                 * fresh fetch fails.
                 */

            } finally {

                isLoading = false
            }

        } else if (files.isEmpty()) {

            isLoading = false
        }
    }


    /*
     * Loading state.
     */
    if (
        isLoading &&
        files.isEmpty()
    ) {

        Box(
            Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                CircularProgressIndicator(
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    strokeWidth = 3.dp
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                Text(
                    "Fetching items...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

    }

    /*
     * Empty folder.
     */
    else if (files.isEmpty()) {

        Box(
            Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                "This folder is empty",
                color = Color.Gray
            )
        }

    }

    /*
     * File / folder list.
     */
    else {

        LazyColumn(

            contentPadding =
                PaddingValues(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)

        ) {

            items(
                files,
                key = { it.id }
            ) { file ->

                val isFolder =
                    file.mimeType
                        .contains("apps.folder")


                /*
                 * Download folder:
                 *
                 * Download/
                 *   BHU Ji/
                 *     AcademicResources/
                 */
                val downloadFolder =
                    File(
                        Environment
                            .getExternalStoragePublicDirectory(
                                Environment
                                    .DIRECTORY_DOWNLOADS
                            ),
                        "BHU Ji/AcademicResources"
                    )

                if (!downloadFolder.exists()) {
                    downloadFolder.mkdirs()
                }


                val localFile =
                    File(
                        downloadFolder,
                        file.name
                    )

                var isDownloaded by remember(file.id) {
                    mutableStateOf(
                        localFile.exists()
                    )
                }

                val progress =
                    downloadingFiles[file.name]

                /*
                 * DownloadManager completion removes the progress entry.
                 * Re-check the actual file so the UI changes to Download.
                 */
                LaunchedEffect(progress) {
                    if (progress == null) {
                        isDownloaded = localFile.exists()
                    }
                }


                FileRow(

                    file = file,

                    isFolder = isFolder,

                    isDownloaded =
                        isDownloaded,

                    downloadProgress =
                        progress,

                    onDelete = {
                        if (progress != null) {
                            cancelDownload(
                                context,
                                file.name
                            )

                            if (downloadingFiles is MutableMap) {
                                @Suppress("UNCHECKED_CAST")
                                (
                                        downloadingFiles as MutableMap<String, Float>
                                        ).remove(file.name)
                            }
                        }

                        if (isDownloaded) {
                            deleteLocalFile(localFile)

                            /*
                             * Always synchronize the UI with the
                             * actual filesystem state.
                             */
                            isDownloaded = localFile.exists()
                        }
                    },

                    onClick = {

                        /*
                         * Folder.
                         */
                        if (isFolder) {

                            onFolderClick(
                                file.id,
                                file.name
                            )

                        } else {

                            /*
                             * Already downloaded.
                             */
                            if (isDownloaded) {

                                onNavigate(
                                    SubView.PdfViewer(
                                        localFile.absolutePath,
                                        file.name
                                    )
                                )

                            }

                            /*
                             * Start download.
                             */
                            else if (progress == null) {

                                file.webContentLink
                                    ?.let { link ->

                                        /*
                                         * Immediately show
                                         * progress.
                                         */
                                        if (
                                            downloadingFiles
                                                    is MutableMap
                                        ) {

                                            @Suppress(
                                                "UNCHECKED_CAST"
                                            )

                                            (
                                                    downloadingFiles
                                                            as MutableMap<
                                                            String,
                                                            Float
                                                            >
                                                    )[file.name] = 0f
                                        }


                                        val request =
                                            DownloadManager
                                                .Request(
                                                    Uri.parse(
                                                        link
                                                    )
                                                )
                                                .setTitle(
                                                    file.name
                                                )
                                                .setDescription(
                                                    "Downloading ${file.name}"
                                                )
                                                .setNotificationVisibility(
                                                    DownloadManager
                                                        .Request
                                                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                                )
                                                .setDestinationInExternalPublicDir(
                                                    Environment
                                                        .DIRECTORY_DOWNLOADS,
                                                    "BHU Ji/AcademicResources/${file.name}"
                                                )

                                        dm.enqueue(request)
                                    }
                            }
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun FileRow(
    file: DriveFile,
    isFolder: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    val progress =
        (downloadProgress ?: 0f)
            .coerceIn(0f, 1f)

    /*
     * The progress fill is drawn from left to right behind
     * the card contents. The card itself remains clickable.
     */
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        if (!isFolder && downloadProgress != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.10f
                        )
                    )
            )
        }

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                /*
                 * File / folder icon.
                 */
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isFolder) {
                                Color(0xFFF59E0B).copy(0.1f)
                            } else {
                                Color(0xFF3B82F6).copy(0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            if (isFolder) {
                                Icons.Rounded.Folder
                            } else {
                                Icons.Rounded.Description
                            },
                        contentDescription = null,
                        tint =
                            if (isFolder) {
                                Color(0xFFF59E0B)
                            } else {
                                Color(0xFF3B82F6)
                            },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(
                    Modifier.width(16.dp)
                )

                /*
                 * Filename + file size.
                 */
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text =
                            if (isFolder) {
                                "Folder"
                            } else {
                                if (downloadProgress != null) {
                                    "Downloading • ${(progress * 100).toInt()}%"
                                } else {
                                    "PDF • ${
                                        formatFileSize(
                                            file.sizeBytes
                                        )
                                    }"
                                }
                            },
                        fontSize = 11.sp,
                        color =
                            if (downloadProgress != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Gray
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                /*
                 * Right-side action.
                 */
                when {
                    isFolder -> {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color.LightGray
                        )
                    }

                    downloadProgress != null -> {
                        /*
                         * Delete/cancel the active download.
                         * The confirmation dialog is shown before
                         * DownloadManager.remove() is called.
                         */
                        IconButton(
                            onClick = {
                                showDeleteConfirmation = true
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Cancel download",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    isDownloaded -> {
                        /*
                         * Delete the downloaded local file.
                         * Opening the PDF is still done by tapping
                         * the card itself.
                         */
                        IconButton(
                            onClick = {
                                showDeleteConfirmation = true
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete downloaded file",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    else -> {
                        IconButton(
                            onClick = onClick
                        ) {
                            Icon(
                                Icons.Rounded.CloudDownload,
                                contentDescription = "Download",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    /*
     * Delete confirmation.
     */
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
            },
            icon = {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    if (downloadProgress != null) {
                        "Cancel download?"
                    } else {
                        "Delete downloaded file?"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (downloadProgress != null) {
                        "Do you want to cancel the download of \"${file.name}\"?"
                    } else {
                        "This will delete \"${file.name}\" from your device. The file will remain available in Drive."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text(
                        if (downloadProgress != null) {
                            "Cancel Download"
                        } else {
                            "Delete"
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Keep")
                }
            }
        )
    }
}