package cvam.dignity.bhumess

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import cvam.dignity.bhumess.AdConfig
import cvam.dignity.bhumess.PdfViewerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "BhuPdfViewer"

private val PdfBackground = Color(0xFFF3F4F6)
private val PdfHeaderColor = Color.White
private val PdfPrimary = Color(0xFF2563EB)
private val PdfText = Color(0xFF111827)
private val PdfSecondaryText = Color(0xFF6B7280)
private val PdfProgressTrack = Color(0xFFE5E7EB)

/**
 * Safely manages PdfRenderer and ParcelFileDescriptor resources.
 */
private class PdfRenderSession(
    val renderer: PdfRenderer,
    val pfd: ParcelFileDescriptor
) {
    val pageCount: Int get() = renderer.pageCount

    fun close() {
        try {
            renderer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing PdfRenderer", e)
        }
        try {
            pfd.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ParcelFileDescriptor", e)
        }
    }
}

/**
 * Opens a ParcelFileDescriptor from a file path or content Uri string.
 */
private fun openFileDescriptor(context: Context, fileUriString: String): ParcelFileDescriptor? {
    return try {
        val uri = Uri.parse(fileUriString)
        if (uri.scheme == "content") {
            context.contentResolver.openFileDescriptor(uri, "r")
        } else {
            val file = if (uri.scheme == "file") File(uri.path ?: "") else File(fileUriString)
            if (file.exists()) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open descriptor for $fileUriString", e)
        null
    }
}

/**
 * Renders a single PDF page to a Bitmap on the I/O thread pool safely.
 */
private suspend fun renderPageBitmap(
    session: PdfRenderSession,
    pageIndex: Int,
    targetWidthPx: Int
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (pageIndex < 0 || pageIndex >= session.pageCount) return@withContext null
        synchronized(session) {
            val page = session.renderer.openPage(pageIndex)
            val aspect = page.height.toFloat() / page.width.toFloat()
            val width = targetWidthPx.coerceAtLeast(300)
            val height = (width * aspect).toInt().coerceAtLeast(300)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error rendering page index $pageIndex", e)
        null
    }
}

/**
 * Pure Jetpack Compose Native PDF Viewer Screen.
 */
@Composable
fun PdfViewerScreen(
    fileUri: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val density = LocalDensity.current

    val screenWidthPx = remember(density) {
        (context.resources.displayMetrics.widthPixels).coerceAtLeast(400)
    }

    var renderSession by remember { mutableStateOf<PdfRenderSession?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Unable to open PDF file") }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val pageCount = renderSession?.pageCount ?: 0
    val currentPage by remember {
        derivedStateOf {
            if (pageCount > 0) (listState.firstVisibleItemIndex + 1).coerceAtMost(pageCount) else 1
        }
    }

    val interstitialManager = remember(activity) {
        activity?.let { PdfInterstitialAdManager(it) }
    }

    DisposableEffect(interstitialManager) {
        onDispose {
            interstitialManager?.destroy()
        }
    }

    // Load PDF file in background thread
    LaunchedEffect(fileUri) {
        isLoading = true
        hasError = false
        renderSession?.close()
        renderSession = null

        withContext(Dispatchers.IO) {
            val pfd = openFileDescriptor(context, fileUri)
            if (pfd != null) {
                try {
                    val renderer = PdfRenderer(pfd)
                    val session = PdfRenderSession(renderer, pfd)
                    withContext(Dispatchers.Main) {
                        renderSession = session
                        isLoading = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize PdfRenderer", e)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        hasError = true
                        errorMessage = "Corrupted or unsupported PDF file"
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    hasError = true
                    errorMessage = "PDF file not found"
                }
            }
        }
    }

    // Clean up session resources when exiting screen
    DisposableEffect(Unit) {
        onDispose {
            renderSession?.close()
            renderSession = null
        }
    }

    // Configure status bar appearance
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.WHITE
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PdfBackground)
    ) {
        if (renderSession != null && !hasError) {
            val session = renderSession!!

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 3f)
                            scale = newScale
                            if (newScale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 70.dp, bottom = 24.dp)
                ) {
                    items(
                        count = session.pageCount,
                        key = { index -> index }
                    ) { index ->
                        PdfPageCard(
                            session = session,
                            pageIndex = index,
                            targetWidthPx = screenWidthPx
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp,
                        color = PdfPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Opening documentâ€¦",
                        color = PdfSecondaryText,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !hasError,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            PdfHeader(
                title = title,
                currentPage = currentPage,
                pageCount = pageCount,
                progress = if (pageCount <= 1) 1f else (currentPage - 1).toFloat() / (pageCount - 1),
                scale = scale,
                onBack = onBack,
                onShare = {
                    PdfViewerUtils.shareResourceLocation(context, title)
                },
                onZoomIn = {
                    scale = (scale + 0.25f).coerceAtMost(3f)
                },
                onZoomOut = {
                    val newScale = (scale - 0.25f).coerceAtLeast(1f)
                    scale = newScale
                    if (newScale == 1f) offset = Offset.Zero
                }
            )
        }
    }
}

/**
 * Individual rendered page card inside LazyColumn.
 */
@Composable
private fun PdfPageCard(
    session: PdfRenderSession,
    pageIndex: Int,
    targetWidthPx: Int
) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, session, pageIndex) {
        value = renderPageBitmap(session, pageIndex, targetWidthPx)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = PdfPrimary
                )
            }
        }
    }
}

private class PdfInterstitialAdManager(private val activity: Activity) {

    companion object {
        private const val TAG = "PdfInterstitialAd"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var hasShownForThisScreen = false

    init {
        loadAd()
    }

    private fun loadAd() {
        if (isLoading || interstitialAd != null || hasShownForThisScreen) return

        isLoading = true

        InterstitialAd.load(
            activity,
            AdConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                            Log.e(TAG, "Interstitial failed: ${adError.code} ${adError.message}")
                            interstitialAd = null
                        }
                    }

                    Log.d(TAG, "Interstitial loaded")
                    showIfReady()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: code=${error.code}, message=${error.message}")
                }
            }
        )
    }

    private fun showIfReady() {
        if (hasShownForThisScreen) return
        val ad = interstitialAd ?: return

        interstitialAd = null
        hasShownForThisScreen = true
        ad.show(activity)
    }

    fun destroy() {
        interstitialAd = null
    }
}

@Composable
private fun PdfHeader(
    title: String,
    currentPage: Int,
    pageCount: Int,
    progress: Float,
    scale: Float,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PdfHeaderColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = PdfText
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    color = PdfText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (pageCount > 0) "Page $currentPage of $pageCount" else "Loading...",
                    color = PdfSecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            IconButton(onClick = onZoomOut) {
                Icon(
                    Icons.Rounded.ZoomOut,
                    contentDescription = "Zoom out",
                    tint = Color(0xFF4B5563)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF3F4F6)
            ) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color(0xFF374151),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onZoomIn) {
                Icon(
                    Icons.Rounded.ZoomIn,
                    contentDescription = "Zoom in",
                    tint = Color(0xFF4B5563)
                )
            }

            IconButton(onClick = onShare) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = "Share",
                    tint = Color(0xFF374151)
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = PdfPrimary,
            trackColor = PdfProgressTrack
        )
    }
}

