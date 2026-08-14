package cvam.dignity.bhumess.screens.students

import android.app.Activity
import android.util.Log
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat

// CRITICAL: Full package import for Barteksc PDF Viewer library
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import cvam.dignity.bhumess.utils.PdfViewerUtils
import cvam.dignity.bhumess.ads.AdConfig
import java.io.File

private val PdfBackground = Color(0xFFF3F4F6)
private val PdfHeaderColor = Color.White
private val PdfPrimary = Color(0xFF2563EB)
private val PdfText = Color(0xFF111827)
private val PdfSecondaryText = Color(0xFF6B7280)
private val PdfProgressTrack = Color(0xFFE5E7EB)


/**
 * Interstitial manager using the centralized AdMob interstitial ad unit.
 *
 * The interstitial is requested when this screen starts. If the ad loads,
 * it is shown once for this screen entry. Frequency capping should be
 * configured in AdMob (for example, 1 impression per user every 10 minutes).
 */
private class PdfInterstitialAdManager(
    private val activity: Activity
) {
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

                    ad.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                adError: com.google.android.gms.ads.AdError
                            ) {
                                Log.e(
                                    TAG,
                                    "Interstitial failed to show: " +
                                            "${adError.code} ${adError.message}"
                                )
                                interstitialAd = null
                            }
                        }

                    Log.d(TAG, "Interstitial loaded; showing on screen start")
                    showIfReady()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null

                    Log.e(
                        TAG,
                        "Interstitial failed to load: " +
                                "code=${error.code}, " +
                                "message=${error.message}, " +
                                "domain=${error.domain}"
                    )
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
fun PdfViewerScreen(
    fileUri: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val file = remember(fileUri) { File(fileUri) }

    // Request an interstitial when this screen starts.
    // AdMob frequency capping controls how often the user actually sees it.
    val interstitialManager = remember(activity) {
        activity?.let { PdfInterstitialAdManager(it) }
    }

    DisposableEffect(interstitialManager) {
        onDispose {
            interstitialManager?.destroy()
        }
    }

    var pdfViewRef by remember { mutableStateOf<PDFView?>(null) }
    var currentPage by remember { mutableIntStateOf(1) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isUiVisible by remember { mutableStateOf(true) }

    val progress = if (pageCount <= 1) 1f else (currentPage - 1).toFloat() / (pageCount - 1)

    SideEffect {
        (context as? Activity)?.window?.let { window ->
            window.statusBarColor = AndroidColor.WHITE
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PdfBackground)
    ) {
        if (file.exists()) {
            AndroidView(
                factory = { ctx ->
                    PDFView(ctx, null).apply {
                        pdfViewRef = this
                        fromFile(file)
                            .defaultPage(0)
                            .enableSwipe(true)
                            .swipeHorizontal(false)
                            .enableDoubletap(true)
                            .enableAnnotationRendering(true)
                            .scrollHandle(DefaultScrollHandle(ctx))
                            .spacing(10)
                            .pageFitPolicy(FitPolicy.WIDTH)
                            .onPageChange { page, total ->
                                currentPage = page + 1
                                pageCount = total
                                pdfViewRef?.let { currentZoom = it.zoom }
                            }
                            .onLoad { totalPages ->
                                pageCount = totalPages
                                isLoading = false
                                hasError = false
                            }
                            .onError {
                                isLoading = false
                                hasError = true
                            }
                            .onTap {
                                isUiVisible = !isUiVisible
                                true
                            }
                            .load()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            hasError = true
            isLoading = false
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp, color = PdfPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Opening document…", color = PdfSecondaryText, fontSize = 14.sp)
                }
            }
        }

        if (hasError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Unable to open PDF file", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }

        AnimatedVisibility(
            visible = isUiVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            PdfHeader(
                title = title,
                currentPage = currentPage,
                pageCount = pageCount,
                progress = progress,
                scale = currentZoom,
                onBack = {
                    onBack()
                },
                onShare = {
                    PdfViewerUtils.shareResourceLocation(context, title)
                },
                onZoomIn = {
                    pdfViewRef?.let { pv ->
                        val target = (pv.zoom + 0.5f).coerceAtMost(pv.maxZoom)
                        pv.zoomWithAnimation(target)
                        currentZoom = target
                    }
                },
                onZoomOut = {
                    pdfViewRef?.let { pv ->
                        val target = (pv.zoom - 0.5f).coerceAtLeast(pv.minZoom)
                        pv.zoomWithAnimation(target)
                        currentZoom = target
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = !isUiVisible && pageCount > 0,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(shape = RoundedCornerShape(50.dp), color = Color.White, shadowElevation = 4.dp) {
                Text(
                    text = "$currentPage / $pageCount",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = PdfText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PdfText)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(text = title, color = PdfText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (pageCount > 0) "Page $currentPage of $pageCount" else "Loading...",
                    color = PdfSecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            IconButton(onClick = onZoomOut) {
                Icon(Icons.Rounded.ZoomOut, contentDescription = "Zoom out", tint = Color(0xFF4B5563))
            }

            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF3F4F6)) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color(0xFF374151),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onZoomIn) {
                Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom in", tint = Color(0xFF4B5563))
            }

            IconButton(onClick = onShare) {
                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color(0xFF374151))
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