package cvam.dignity.bhumess.screens.students

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvam.dignity.bhumess.navigation.SubView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Data class representing a remote ad banner model loaded dynamically from ads.json.
 */
data class RemoteAdBanner(
    val id: String,
    val badgeText: String,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val gradientColors: List<String>,
    val targetUrl: String
)

private val defaultAdBanners = listOf(
    RemoteAdBanner(
        id = "ad1",
        badgeText = "FEATURED UTILITY",
        title = "CUET Score Calculator",
        subtitle = "Calculate your PG marks & percentile instantly",
        iconName = "Analytics",
        gradientColors = listOf("#2563EB", "#7C3AED"),
        targetUrl = "https://kumarhui.github.io/BHU_JI_v2/ad1.html"
    ),
    RemoteAdBanner(
        id = "ad2",
        badgeText = "ADMISSION PORTAL",
        title = "BHU Cutoff & Rank Predictor",
        subtitle = "Check category-wise seat probabilities",
        iconName = "School",
        gradientColors = listOf("#059669", "#10B981"),
        targetUrl = "https://kumarhui.github.io/BHU_JI_v2/ad2.html"
    ),
    RemoteAdBanner(
        id = "ad3",
        badgeText = "STUDENT",
        title = "Hostel & Campus Updates",
        subtitle = "Latest notifications & hostel allocation news",
        iconName = "Campaign",
        gradientColors = listOf("#D97706", "#EA580C"),
        targetUrl = "https://kumarhui.github.io/BHU_JI_v2/ad3.html"
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudyResourcesScreen(onNavigate: (SubView) -> Unit) {
    var adBannerList by remember { mutableStateOf(defaultAdBanners) }

    suspend fun updateAds() {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

                val timestamp = System.currentTimeMillis()

                val primaryUrl =
                    "https://kumarhui.github.io/BHU_JI_v2/ads.json?t=$timestamp"

                val fallbackUrl =
                    "https://raw.githubusercontent.com/kumarhui/BHU_JI_v2/main/ads.json?t=$timestamp"

                var responseString: String? = null

                // Primary URL
                try {
                    val request = Request.Builder()
                        .url(primaryUrl)
                        .header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            responseString = response.body?.string()?.trim()
                        }
                    }
                } catch (_: Exception) {
                    // Try fallback
                }

                // Fallback URL
                if (responseString.isNullOrEmpty() ||
                    !responseString!!.startsWith("[")
                ) {
                    try {
                        val request = Request.Builder()
                            .url(fallbackUrl)
                            .header("Cache-Control", "no-cache, no-store, must-revalidate")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                responseString = response.body?.string()?.trim()
                            }
                        }
                    } catch (_: Exception) {
                        // Keep existing ads
                    }
                }

                // Parse and update UI
                if (!responseString.isNullOrEmpty() &&
                    responseString!!.startsWith("[")
                ) {
                    val fetchedAds = parseAdsJson(responseString!!)

                    if (fetchedAds.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            adBannerList = fetchedAds
                        }
                    }
                }

            } catch (_: Exception) {
                // Keep current/default ads if refresh fails
            }
        }
    }

    LaunchedEffect(Unit) {
        updateAds()
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { adBannerList.size })

    // Auto-scroll through ad banners every 4 seconds
    LaunchedEffect(adBannerList) {
        if (adBannerList.size > 1) {
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % adBannerList.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                updateAds()
                isRefreshing = false
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dynamic Ad Carousel Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                ) { page ->
                    val ad = adBannerList[page % adBannerList.size]
                    val color1 = parseColor(ad.gradientColors.getOrNull(0) ?: "#2563EB")
                    val color2 = parseColor(ad.gradientColors.getOrNull(1) ?: "#7C3AED")

                    Card(
                        onClick = { onNavigate(SubView.HtmlViewer(ad.targetUrl, ad.title)) },
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(color1, color2)))
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxWidth(0.8f)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = ad.badgeText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = ad.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = ad.subtitle,
                                    color = Color.White.copy(0.85f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = getIconByName(ad.iconName),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .align(Alignment.CenterEnd)
                                    .offset(x = 10.dp),
                                tint = Color.White.copy(alpha = 0.18f)
                            )
                        }
                    }
                }

                // Scrollable Page Indicator Bar for up to 20+ ads
                if (adBannerList.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(adBannerList.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.LightGray.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
            }

            Text("ACADEMIC RESOURCES", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)

            // Grid Layout for Resources
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ResourceGridItem(
                    title = "Study Notes",
                    subtitle = "Handwritten",
                    icon = Icons.Rounded.AutoStories,
                    color = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(SubView.DriveExplorer("116XFzMyxgGi6TwMffEyU1wC31768v-Eu", "Study Notes"))
                }
                ResourceGridItem(
                    title = "PYQs",
                    subtitle = "Exam Papers",
                    icon = Icons.Rounded.HistoryEdu,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(SubView.DriveExplorer("1YITLhxtuu8mh4HlBGEe8XlIp-XuctbCq", "Question Bank"))
                }
            }

            ResourceWideItem(
                title = "Syllabus Hub",
                subtitle = "Access latest course structures",
                icon = Icons.Rounded.Assignment,
                color = Color(0xFFF59E0B)
            ) {
                onNavigate(SubView.HtmlViewer("https://zodax.gamer.gd/BHU_JI_NEO/index.html?i=1", "Syllabus Hub"))
            }

            Spacer(Modifier.height(8.dp))

            Text("UTILITIES", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)

            ResourceWideItem(
                title = "Downloads",
                subtitle = "Access your downloaded PDFs",
                icon = Icons.Rounded.CloudDone,
                color = Color(0xFF3B82F6)
            ) {
                onNavigate(SubView.DownloadedFiles)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun ResourceGridItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ResourceWideItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

/**
 * Parses remote JSON array string into list of RemoteAdBanner.
 * Supports any number of ad entries (1 to 20+).
 */
fun parseAdsJson(jsonStr: String): List<RemoteAdBanner> {
    val result = mutableListOf<RemoteAdBanner>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val colorsList = mutableListOf<String>()
            val colorsArr = obj.optJSONArray("gradientColors")
            if (colorsArr != null) {
                for (j in 0 until colorsArr.length()) {
                    colorsList.add(colorsArr.getString(j))
                }
            }

            result.add(
                RemoteAdBanner(
                    id = obj.optString("id", "ad_$i"),
                    badgeText = obj.optString("badgeText", "FEATURED"),
                    title = obj.optString("title", "BHU Resource"),
                    subtitle = obj.optString("subtitle", "Click to explore"),
                    iconName = obj.optString("iconName", "Analytics"),
                    gradientColors = if (colorsList.isNotEmpty()) colorsList else listOf("#2563EB", "#7C3AED"),
                    targetUrl = obj.optString("targetUrl", "https://kumarhui.github.io/BHU_JI_v2/ad1.html")
                )
            )
        }
    } catch (_: Exception) {
        // Fallback to defaults if JSON parsing fails
    }
    return result
}

/**
 * Maps icon name strings to Material Icons vector objects.
 */
fun getIconByName(name: String): ImageVector {
    return when (name.lowercase()) {
        "school" -> Icons.Rounded.School
        "campaign" -> Icons.Rounded.Campaign
        "autostories" -> Icons.Rounded.AutoStories
        "assignment" -> Icons.Rounded.Assignment
        "star" -> Icons.Rounded.Star
        else -> Icons.Rounded.Analytics
    }
}

/**
 * Safely parses hex color strings (e.g., "#2563EB") into Compose Color objects.
 */
fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Color(0xFF2563EB)
    }
}