package cvam.dignity.bhumess

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvam.dignity.bhumess.navigation.AppDestination

private const val NOTES_FOLDER_ID = "116XFzMyxgGi6TwMffEyU1wC31768v-Eu"
private const val PYQS_FOLDER_ID = "1YITLhxtuu8mh4HlBGEe8XlIp-XuctbCq"
private const val SYLLABUS_URL =
    "https://zodax.gamer.gd/BHU_JI_NEO/index.html?i=1"

@Composable
fun DashboardScreen(
    onNavigate: (AppDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = 24.dp,
                vertical = 20.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Text(
            text = "BHUMESS",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Academic Resources",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(44.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularTool(
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

            CircularTool(
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
        }

        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularTool(
                title = "Syllabus",
                icon = Icons.Rounded.Assignment,
                color = Color(0xFFF59E0B),
                onClick = {
                    onNavigate(
                        AppDestination.HtmlViewer(
                            url = SYLLABUS_URL,
                            title = "Syllabus"
                        )
                    )
                }
            )

            CircularTool(
                title = "Downloads",
                icon = Icons.Rounded.CloudDone,
                color = Color(0xFF3B82F6),
                onClick = {
                    onNavigate(AppDestination.DownloadedFiles)
                }
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CircularTool(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(5.dp, CircleShape)
                .background(color, CircleShape),
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
                    modifier = Modifier.size(29.dp)
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
