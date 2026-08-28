package cvam.dignity.bhumess

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cvam.dignity.bhumess.navigation.AppDestination

/**
 * Premium Web-Based Score Calculator Screen.
 * FIXED: JS Dialogs (Alert/Confirm/Prompt), Multiple File Upload, and Parameter mismatch.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreCalculatorScreen(
    isTabMode: Boolean = false,
    onNavigate: (AppDestination) -> Unit = {}, // Added to match MainActivity call
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val url = "https://zodax.gamer.gd/answer_checker/"
    val waUrl = "https://chat.whatsapp.com/HNf3YvlEUzCLZOssSZH7PJ"

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var hasError by remember { mutableStateOf(false) }

    // FIXED: Support for picking MULTIPLE PDF files
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            fileCallback?.onReceiveValue(uris.toTypedArray())
        } else {
            fileCallback?.onReceiveValue(null)
        }
        fileCallback = null
    }

    // Handle system back button for WebView internal history
    BackHandler(enabled = true) {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            onBack?.invoke()
        }
    }

    Scaffold(
        topBar = {
            if (!isTabMode) {
                Box(Modifier.background(Color.White).statusBarsPadding()) {
                    CenterAlignedTopAppBar(
                        title = { Text("RankJi Calculator", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                        navigationIcon = {
                            IconButton(onClick = { onBack?.invoke() }) {
                                Icon(Icons.Rounded.ArrowBackIosNew, null)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
                            }) {
                                Icon(Icons.Rounded.Groups, null, tint = Color(0xFF25D366))
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(if (isTabMode) PaddingValues(0.dp) else padding)
            .background(Color.White)
        ) {
            if (hasError) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.SignalWifiOff, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("Connection Failed", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "Please check your internet connection. Campus Wi-Fi might be blocking the tool; try Mobile Data.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray, fontSize = 14.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            hasError = false
                            progress = 0
                            webViewRef?.reload()
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Loading")
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(v: WebView?, r: Int, d: String?, f: String?) {
                                    hasError = true
                                }
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val requestUrl = request?.url?.toString() ?: ""
                                    // Handle WhatsApp/External Intents
                                    return if (requestUrl.contains("whatsapp.com") || requestUrl.contains("wa.me") || requestUrl.startsWith("tel:")) {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl)))
                                        true
                                    } else false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(v: WebView?, p: Int) { progress = p }

                                // File picker handling
                                override fun onShowFileChooser(wv: WebView?, fpc: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                                    fileCallback = fpc
                                    filePicker.launch(arrayOf("application/pdf"))
                                    return true
                                }

                                // FIXED: window.alert() handling
                                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                    AlertDialog.Builder(ctx)
                                        .setTitle("RankJi Notification")
                                        .setMessage(message)
                                        .setPositiveButton("OK") { _, _ -> result?.confirm() }
                                        .setOnCancelListener { result?.cancel() }
                                        .setCancelable(true)
                                        .show()
                                    return true
                                }

                                // FIXED: window.confirm() handling
                                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                    AlertDialog.Builder(ctx)
                                        .setTitle("Confirmation")
                                        .setMessage(message)
                                        .setPositiveButton("Yes") { _, _ -> result?.confirm() }
                                        .setNegativeButton("No") { _, _ -> result?.cancel() }
                                        .setOnCancelListener { result?.cancel() }
                                        .setCancelable(true)
                                        .show()
                                    return true
                                }

                                // FIXED: window.prompt() handling
                                override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                                    val input = EditText(ctx).apply { setText(defaultValue) }
                                    val container = FrameLayout(ctx).apply {
                                        val p = (24 * ctx.resources.displayMetrics.density).toInt()
                                        setPadding(p, 0, p, 0)
                                        addView(input)
                                    }
                                    AlertDialog.Builder(ctx)
                                        .setTitle("Input Required")
                                        .setMessage(message)
                                        .setView(container)
                                        .setPositiveButton("OK") { _, _ -> result?.confirm(input.text.toString()) }
                                        .setNegativeButton("Cancel") { _, _ -> result?.cancel() }
                                        .setOnCancelListener { result?.cancel() }
                                        .show()
                                    return true
                                }
                            }

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                javaScriptCanOpenWindowsAutomatically = true
                                // Set to FALSE to force "popups" to open in the same window (common fix for mobile)
                                setSupportMultipleWindows(false)
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (progress < 100 && !hasError) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

