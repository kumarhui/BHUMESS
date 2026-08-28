package cvam.dignity.bhumess

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.pm.PackageManager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import cvam.dignity.bhumess.navigation.AppDestination


private const val NOTES_FOLDER_ID =
    "116XFzMyxgGi6TwMffEyU1wC31768v-Eu"

private const val PYQS_FOLDER_ID =
    "1YITLhxtuu8mh4HlBGEe8XlIp-XuctbCq"

private const val SYLLABUS_URL =
    "https://zodax.gamer.gd/BHU_JI_NEO/index.html?i=1"


@Composable
fun DashboardScreen(
    onNavigate: (AppDestination) -> Unit
) {

    var showWhatsappDialog by remember {
        mutableStateOf(false)
    }

    var showInfoDialog by remember {
        mutableStateOf(false)
    }


    Scaffold { _ ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
        ) {

            // =====================================================
            // Dashboard Header
            // =====================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "BHUMESS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }


            // =====================================================
            // Content
            // =====================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Academic Resources",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(30.dp)
                )


                // =================================================
                // ROW 1
                // =================================================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    DashboardTool(
                        title = "Notes",
                        icon = Icons.Rounded.AutoStories,
                        color = Color(0xFF6366F1),
                        onClick = {

                            onNavigate(
                                AppDestination.DriveExplorer(
                                    folderId = NOTES_FOLDER_ID,
                                    title = "Notes"
                                )
                            )
                        }
                    )


                    DashboardTool(
                        title = "PYQs",
                        icon = Icons.Rounded.HistoryEdu,
                        color = Color(0xFF10B981),
                        onClick = {

                            onNavigate(
                                AppDestination.DriveExplorer(
                                    folderId = PYQS_FOLDER_ID,
                                    title = "PYQs"
                                )
                            )
                        }
                    )


                    DashboardTool(
                        title = "Syllabus",
                        icon = Icons.Rounded.Assignment,
                        color = Color(0xFFF59E0B),
                        onClick = {

                            onNavigate(
                                AppDestination.HtmlViewer(
                                    url = SYLLABUS_URL,
                                    title = "Syllabus Hub"
                                )
                            )
                        }
                    )
                }


                Spacer(
                    modifier = Modifier.height(38.dp)
                )


                // =================================================
                // ROW 2
                // =================================================

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    DashboardTool(
                        title = "Downloads",
                        icon = Icons.Rounded.CloudDone,
                        color = Color(0xFF3B82F6),
                        onClick = {

                            onNavigate(
                                AppDestination.DownloadedFiles
                            )
                        }
                    )


                    DashboardTool(
                        title = "WhatsApp",
                        icon = Icons.Default.Chat,
                        color = Color(0xFF25D366),
                        onClick = {

                            showWhatsappDialog = true
                        }
                    )


                    DashboardTool(
                        title = "App Info",
                        icon = Icons.Default.Info,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {

                            showInfoDialog = true
                        }
                    )
                }
            }
        }
    }


    // =============================================================
    // WhatsApp Dialog
    // =============================================================

    if (showWhatsappDialog) {

        WhatsappDirectDialog(
            onDismiss = {
                showWhatsappDialog = false
            }
        )
    }


    // =============================================================
    // App Info Dialog
    // =============================================================

    if (showInfoDialog) {

        AppInfoDialog(
            onDismiss = {
                showInfoDialog = false
            }
        )
    }
}


// =============================================================
// Dashboard Tool
// =============================================================

@Composable
private fun DashboardTool(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier.width(92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(
                    elevation = 5.dp,
                    shape = CircleShape
                )
                .background(
                    color = color,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            IconButton(
                onClick = onClick,
                modifier = Modifier.fillMaxSize()
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}


// =============================================================
// WhatsApp Direct Dialog
// =============================================================

@Composable
private fun WhatsappDirectDialog(
    onDismiss: () -> Unit
) {

    val context = androidx.compose.ui.platform.LocalContext.current

    var phoneNumber by remember {
        mutableStateOf("")
    }

    val focusRequester =
        remember {
            FocusRequester()
        }

    val scope =
        rememberCoroutineScope()


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                // =================================================
                // WhatsApp Icon
                // =================================================

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFF25D366)
                                .copy(alpha = 0.12f)
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(34.dp)
                    )
                }


                Spacer(
                    Modifier.height(14.dp)
                )


                Text(
                    text = "Direct WhatsApp",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )


                Text(
                    text = "Start chat without saving contact",
                    fontSize = 12.sp,
                    color = MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                )


                Spacer(
                    Modifier.height(20.dp)
                )


                ModernDialogPhoneField(
                    value = phoneNumber,
                    onValueChange = { input ->

                        if (input.length <= 10) {

                            phoneNumber =
                                input.filter {
                                    it.isDigit()
                                }
                        }
                    },
                    focusRequester = focusRequester
                )


                Spacer(
                    Modifier.height(22.dp)
                )


                Button(
                    onClick = {

                        openWhatsAppWithFallback(
                            context,
                            phoneNumber
                        )

                        onDismiss()
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    enabled =
                        phoneNumber.length == 10,

                    shape =
                        RoundedCornerShape(16.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF25D366),
                            contentColor =
                                Color.White
                        )
                ) {

                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        "OPEN CHAT",
                        fontWeight = FontWeight.Black
                    )
                }


                Spacer(
                    Modifier.height(8.dp)
                )


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    if (phoneNumber.isNotEmpty()) {

                        TextButton(
                            onClick = {

                                phoneNumber = ""

                                scope.launch {
                                    focusRequester
                                        .requestFocus()
                                }
                            }
                        ) {

                            Text(
                                "Clear",
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                    } else {

                        Spacer(
                            Modifier.width(10.dp)
                        )
                    }


                    TextButton(
                        onClick = onDismiss
                    ) {

                        Text(
                            "Close",
                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


// =============================================================
// Phone Number Field
// =============================================================

@Composable
private fun ModernDialogPhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {

    var isFocused by remember {
        mutableStateOf(false)
    }


    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                "Phone Number",
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


            Spacer(
                Modifier.weight(1f)
            )


            Text(
                "${value.length}/10",
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,

                color =
                    if (value.length == 10)
                        Color(0xFF25D366)
                    else
                        MaterialTheme
                            .colorScheme
                            .outline,

                fontWeight =
                    FontWeight.Bold
            )
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(14.dp)
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(alpha = 0.4f)
                )
                .border(
                    width = 2.dp,
                    color =
                        if (isFocused)
                            Color(0xFF25D366)
                        else
                            Color.Transparent,
                    shape =
                        RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "+91 ",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .outline
                    )
                )


                BasicTextField(
                    value = value,

                    onValueChange =
                        onValueChange,

                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Black,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    ),

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number,
                            imeAction =
                                ImeAction.Done
                        ),

                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(
                            focusRequester
                        )
                        .onFocusChanged {
                            isFocused =
                                it.isFocused
                        }
                )
            }
        }
    }
}


// =============================================================
// Open WhatsApp
// =============================================================

private fun openWhatsAppWithFallback(
    context: Context,
    phoneNumber: String
) {

    if (phoneNumber.length != 10) {
        return
    }


    val uri =
        Uri.parse(
            "https://wa.me/91$phoneNumber"
        )


    val intent =
        Intent(
            Intent.ACTION_VIEW,
            uri
        ).apply {
            setPackage("com.whatsapp")
        }


    try {

        context.startActivity(intent)

    } catch (_: Exception) {

        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                uri
            )
        )
    }
}


// =============================================================
// App Info Dialog
// =============================================================

@Composable
private fun AppInfoDialog(
    onDismiss: () -> Unit
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current


    val versionName =
        remember {

            try {

                val packageInfo =
                    context.packageManager
                        .getPackageInfo(
                            context.packageName,
                            0
                        )

                packageInfo.versionName
                    ?: "1.0.0"

            } catch (_: Exception) {

                "1.0.0"
            }
        }


    val shareApp: () -> Unit = {

        val sendIntent =
            Intent().apply {

                action =
                    Intent.ACTION_SEND

                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out BHUMESS - Academic Resource Hub: " +
                            "https://play.google.com/store/apps/details?id=${context.packageName}"
                )

                type =
                    "text/plain"
            }


        val shareIntent =
            Intent.createChooser(
                sendIntent,
                "Share BHUMESS App"
            )


        context.startActivity(
            shareIntent
        )
    }


    AlertDialog(

        onDismissRequest = onDismiss,


        // =========================================================
        // Icon
        // =========================================================

        icon = {

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,
                modifier =
                    Modifier.size(36.dp)
            )
        },


        // =========================================================
        // Title
        // =========================================================

        title = {

            Text(
                text = "BHUMESS",
                fontWeight =
                    FontWeight.Black,
                fontSize = 20.sp
            )
        },


        // =========================================================
        // Content
        // =========================================================

        text = {

            Column(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text =
                        "Version $versionName",

                    fontSize = 14.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical = 4.dp
                        )
                )


                Text(
                    text = "Developer",

                    fontSize = 12.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )


                Text(
                    text = "CVAM Dignity Tech",

                    fontSize = 14.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                OutlinedButton(
                    onClick = shareApp,

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(12.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Share,

                        contentDescription =
                            "Share",

                        modifier =
                            Modifier.size(18.dp)
                    )


                    Spacer(
                        Modifier.width(8.dp)
                    )


                    Text(
                        "Share App Link",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },


        // =========================================================
        // Close
        // =========================================================

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Close")
            }
        }
    )
}