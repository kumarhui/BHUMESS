package cvam.dignity.bhumess.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {
    /**
     * Points to a dedicated folder inside the app's internal storage.
     * Ensures files persist across re-installs and stay organized.
     */
    fun getAcademicFolder(context: Context): File {
        val folder = File(context.getExternalFilesDir(null), "AcademicResources")
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    /**
     * Updated signature to accept Context, resolving the build errors.
     */
    fun getLocalFile(context: Context, fileName: String): File {
        return File(getAcademicFolder(context), fileName)
    }

    fun openWithDriveApp(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val mimeType = when {
                file.name.endsWith(".pdf", true) -> "application/pdf"
                file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true) -> "image/*"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Attempt to use Google Drive app as the primary handler
                setPackage("com.google.android.apps.docs")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to system-wide viewer if Drive app is not available
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                setDataAndType(uri, if (file.name.endsWith(".pdf", true)) "application/pdf" else "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }
    }
}