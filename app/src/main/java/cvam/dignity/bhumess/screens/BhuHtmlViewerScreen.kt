package cvam.dignity.bhumess.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cvam.dignity.bhumess.ui.components.BhuTopBar

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BhuHtmlViewerScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var fileCallback by remember {
        mutableStateOf<ValueCallback<Array<Uri>>?>(null)
    }
    var progress by remember { mutableStateOf(0) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        fileCallback?.onReceiveValue(
            if (uri != null) arrayOf(uri) else null
        )
        fileCallback = null
    }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            BhuTopBar(
                title = title,
                onBack = onBack
            )
        }    ) { padding ->

        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {

                        webViewRef = this

                        webViewClient = WebViewClient()

                        webChromeClient = object : WebChromeClient() {

                            override fun onProgressChanged(
                                view: WebView?,
                                newProgress: Int
                            ) {
                                progress = newProgress
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileCallback = filePathCallback
                                filePicker.launch("*/*")
                                return true
                            }
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }

                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (progress < 100) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = "Loading Web Tool...",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
