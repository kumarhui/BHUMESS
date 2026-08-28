package cvam.dignity.bhumess

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Utility to share the "Location" of a resource within the app
 * to encourage app downloads instead of sharing the raw file.
 */
object PdfViewerUtils {

    private const val APP_LINK = "https://play.google.com/store/apps/details?id=cvam.dignity.bhumess"

    /**
     * Shares a text message containing the file title and its location.
     */
    fun shareResourceLocation(context: Context, fileTitle: String) {
        // Construct a compelling message that forces the recipient to use the app
        val shareMessage = """
            ðŸ“š I found the resource: "$fileTitle"
            
            You can access this PDF, along with all other BHU Study Notes, PYQs, and Syllabus directly in the "Study Resources" section of the BHU Ji App.
            
            Download the BHU Ji App here to open it:
            $APP_LINK
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            putExtra(Intent.EXTRA_SUBJECT, "BHU Academic Resource Found")
        }

        try {
            val chooser = Intent.createChooser(shareIntent, "Share Resource Location")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed", Toast.LENGTH_SHORT).show()
        }
    }
}
