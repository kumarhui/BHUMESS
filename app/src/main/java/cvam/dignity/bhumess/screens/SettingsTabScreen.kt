package cvam.dignity.bhumess.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium Settings Screen for Bhu Ji.
 * Displays preferences, update triggers, WhatsApp community link, and highlighted app version details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen(
    onLogout: () -> Unit = {}, // Default empty action to make it optional
    onBack: () -> Unit         // Last parameter to support trailing lambda
) {
    val context = LocalContext.current

    val appVersion = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )
            }

            packageInfo.versionName ?: "1.1.1.37"
        } catch (e: Exception) {
            "1.1.1.37"
        }
    }

    /**
     * Opens the Play Store directly using the web share link.
     */
    fun openPlayStore() {
        val updateUrl = "https://play.google.com/store/apps/details?id=cvam.dignity.bhumess&pcampaignid=web_share"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
            context.startActivity(fallbackIntent)
        }
    }

    Scaffold(
        topBar = {
            Box(Modifier.background(Color.White).statusBarsPadding()) {
                CenterAlignedTopAppBar(
                    title = { Text("App Settings", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 1.sp
            )

            SettingsActionCard(
                title = "Check for Updates",
                subtitle = "Version $appVersion (Latest)",
                icon = Icons.Rounded.SystemUpdate,
                color = Color(0xFF10B981),
                onClick = { openPlayStore() }
            )

            SettingsActionCard(
                title = "Join Community",
                subtitle = "Request features on WhatsApp",
                icon = Icons.Rounded.Groups,
                color = Color(0xFF25D366),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chat.whatsapp.com/HNf3YvlEUzCLZOssSZH7PJ"))
                    context.startActivity(intent)
                }
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "App Version $appVersion",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Bhu Ji is designed for BHU Students.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Made with ❤️ in Varanasi",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}