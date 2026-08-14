package cvam.dignity.bhumess.screens.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvam.dignity.bhumess.data.LocalDataManager

/**
 * Updated Student Profile Screen with Unified App Design.
 * Includes status bar handling and premium visual identifiers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Fetching data using existing logic
    val name by remember { mutableStateOf(LocalDataManager.getProfileName(context)) }
    val rollNo by remember { mutableStateOf(LocalDataManager.getUserId(context)) }
    val faculty by remember { mutableStateOf(LocalDataManager.getUserFaculty(context)) }
    val phone by remember { mutableStateOf(LocalDataManager.getProfilePhone(context)) }

    Scaffold(
        topBar = {
            // Box wrapper handles the system status bar area
            Box(Modifier.background(Color.White).statusBarsPadding()) {
                CenterAlignedTopAppBar(
                    title = { Text("My Profile", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null) }
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- PREMIUM AVATAR HEADER ---
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase().ifEmpty { "U" },
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(name.ifEmpty { "BHU Student" }, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)

            // Verified Badge
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("VERIFIED STUDENT", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- ACADEMIC INFORMATION SECTION ---
            ProfileSectionCard(title = "Academic Details") {
                ProfileDisplayRow(label = "Roll Number", value = rollNo, icon = Icons.Rounded.Badge)
                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))
                ProfileDisplayRow(label = "Faculty", value = faculty, icon = Icons.Rounded.School)
                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))
                ProfileDisplayRow(label = "Mobile", value = phone, icon = Icons.Rounded.PhoneAndroid)
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Return to Dashboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ProfileDisplayRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(value.ifEmpty { "Not Provided" }, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * SHARED UTILITY COMPONENT
 * Added back to resolve import errors in other files.
 */
@Composable
fun ProfileInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color(0xFFE2E8F0)
        )
    )
    Spacer(Modifier.height(8.dp))
}