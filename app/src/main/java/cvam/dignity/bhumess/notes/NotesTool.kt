package cvam.dignity.bhumess.notes

import androidx.compose.runtime.Composable
import cvam.dignity.bhumess.navigation.AppDestination

/**
 * Notes tool entry point.
 * Uses the same DriveExplorer destination as PYQs.
 */
@Composable
fun NotesTool(
    onNavigate: (AppDestination) -> Unit
) {
    onNavigate(
        AppDestination.DriveExplorer(
            folderId = "116XFzMyxgGi6TwMffEyU1wC31768v-Eu",
            title = "Notes"
        )
    )
}
