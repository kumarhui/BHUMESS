package cvam.dignity.bhumess.pdfViewer

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import com.rizzi.bouquet.ResourceType
import com.rizzi.bouquet.VerticalPDFReader
import com.rizzi.bouquet.rememberVerticalPdfReaderState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val pdfUri = remember(uri) {
        Uri.parse(uri)
    }

    var showInfo by remember {
        mutableStateOf(false)
    }


    // =========================================================
    // PDF Reader
    // =========================================================

    val pdfState = rememberVerticalPdfReaderState(
        resource = ResourceType.Local(pdfUri),
        isZoomEnable = true,
        isAccessibleEnable = false
    )


    // =========================================================
    // Open PDF With Another App
    // =========================================================

    fun openWithOtherApp() {

        val intent = Intent(
            Intent.ACTION_VIEW
        ).apply {

            setDataAndType(
                pdfUri,
                "application/pdf"
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

        try {

            context.startActivity(
                Intent.createChooser(
                    intent,
                    "Open PDF with"
                )
            )

        } catch (_: Exception) {

            // No PDF application installed.
        }
    }


    // =========================================================
    // Screen
    // =========================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = title,
                        maxLines = 1
                    )
                },


                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Rounded.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },


                actions = {

                    // =============================================
                    // Open With Other Apps
                    // =============================================

                    IconButton(
                        onClick = {
                            openWithOtherApp()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Rounded.OpenInNew,

                            contentDescription =
                                "Open with another app"
                        )
                    }


                    // =============================================
                    // PDF Information
                    // =============================================

                    IconButton(
                        onClick = {
                            showInfo = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Rounded.Info,

                            contentDescription =
                                "PDF information"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFE8E8E8)
                )
                .padding(paddingValues)
        ) {


            // =====================================================
            // PDF
            // =====================================================

            VerticalPDFReader(
                state = pdfState,
                modifier = Modifier.fillMaxSize()
            )


            // =====================================================
            // Error
            // =====================================================

            pdfState.error?.let {

                Text(
                    text = "Unable to open PDF",

                    modifier =
                        Modifier.align(
                            Alignment.Center
                        )
                )
            }
        }
    }


    // =========================================================
    // PDF Info Dialog
    // =========================================================

    if (showInfo) {

        PdfInfoDialog(
            title = title,
            onDismiss = {
                showInfo = false
            }
        )
    }
}


// =============================================================
// PDF Information Dialog
// =============================================================

@Composable
private fun PdfInfoDialog(
    title: String,
    onDismiss: () -> Unit
) {

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("PDF")
        },

        text = {
            Text(title)
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("OK")
            }
        }
    )
}