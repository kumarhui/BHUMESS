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
    onExitExplorer: () -> Unit,
    onOpenDownloads: () -> Unit,
    onNavigate: (SubView) -> Unit
) {

    val context = LocalContext.current

    val folderStack = remember {
        mutableStateListOf(initialFolderId)
    }

    val folderNames = remember {
        mutableStateListOf(initialFolderName)
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

            folderStack.removeAt(
                folderStack.size - 1
            )

            folderNames.removeAt(
                folderNames.size - 1
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
                        text = folderNames.last(),
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

                                folderStack.removeAt(
                                    folderStack.size - 1
                                )

                                folderNames.removeAt(
                                    folderNames.size - 1
                                )

                            } else {

                                onExitExplorer()
                            }
                        }
                    ) {

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

                targetState =
                    folderStack.last(),

                transitionSpec = {

                    if (folderStack.size > 1) {

                        (
                                slideInHorizontally {
                                    it
                                } + fadeIn()
                                ) togetherWith (
                                slideOutHorizontally {
                                    -it
                                } + fadeOut()
                                )

                    } else {

                        fadeIn() togetherWith
                                fadeOut()
                    }
                },

                label = "ExplorerContent"

            ) { currentId ->

                FolderContent(

                    folderId = currentId,

                    onFolderClick = { id, name ->

                        folderStack.add(id)
                        folderNames.add(name)
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

                val isDownloaded =
                    localFile.exists()

                val progress =
                    downloadingFiles[file.name]


                FileRow(

                    file = file,

                    isFolder = isFolder,

                    isDownloaded =
                        isDownloaded,

                    downloadProgress =
                        progress,

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
    onClick: () -> Unit
) {

    Surface(

        onClick = onClick,

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        color =
            Color.White,

        shadowElevation =
            0.5.dp

    ) {

        Row(

            modifier =
                Modifier.padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            /*
             * File / folder icon.
             */
            Box(

                modifier = Modifier
                    .size(44.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
                    .background(

                        if (isFolder)

                            Color(0xFFF59E0B)
                                .copy(0.1f)

                        else

                            Color(0xFF3B82F6)
                                .copy(0.1f)
                    ),

                contentAlignment =
                    Alignment.Center

            ) {

                Icon(

                    imageVector =

                        if (isFolder)

                            Icons.Rounded.Folder

                        else

                            Icons.Rounded.Description,

                    contentDescription =
                        null,

                    tint =

                        if (isFolder)

                            Color(0xFFF59E0B)

                        else

                            Color(0xFF3B82F6),

                    modifier =
                        Modifier.size(22.dp)
                )
            }


            Spacer(
                Modifier.width(16.dp)
            )


            /*
             * Filename + file size.
             */
            Column(

                modifier =
                    Modifier.weight(1f)

            ) {

                Text(

                    text = file.name,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        14.sp,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )


                Text(

                    text =

                        if (isFolder) {

                            "Folder"

                        } else {

                            "PDF • ${
                                formatFileSize(
                                    file.sizeBytes
                                )
                            }"
                        },

                    fontSize =
                        11.sp,

                    color =
                        Color.Gray,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }


            /*
             * Right-side action.
             */
            when {

                /*
                 * Folder.
                 */
                isFolder -> {

                    Icon(

                        Icons.Rounded.ChevronRight,

                        contentDescription =
                            null,

                        tint =
                            Color.LightGray
                    )
                }


                /*
                 * Currently downloading.
                 */
                downloadProgress != null -> {

                    CircularProgressIndicator(

                        progress = {
                            downloadProgress
                        },

                        modifier =
                            Modifier.size(20.dp),

                        strokeWidth =
                            3.dp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,

                        trackColor =
                            Color.LightGray
                                .copy(0.2f)
                    )
                }


                /*
                 * Already downloaded.
                 */
                isDownloaded -> {

                    Icon(

                        Icons.Rounded.OfflinePin,

                        contentDescription =
                            "Downloaded",

                        tint =
                            Color(0xFF10B981),

                        modifier =
                            Modifier.size(22.dp)
                    )
                }


                /*
                 * Not downloaded.
                 */
                else -> {

                    Icon(

                        Icons.Rounded.CloudDownload,

                        contentDescription =
                            "Download",

                        tint =
                            Color.LightGray,

                        modifier =
                            Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}