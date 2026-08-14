package cvam.dignity.bhumess.api

import android.content.Context
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import cvam.dignity.bhumess.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val thumbnailLink: String? = null,
    val webContentLink: String? = null
)

class DriveApiService(private val context: Context) {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    private fun getDriveService(): Drive {
        // FIXED: Using Service Account instead of User OAuth for background loading
        val inputStream = context.resources.openRawResource(R.raw.service_account_credentials)
        val credentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf(DriveScopes.DRIVE_READONLY))

        return Drive.Builder(
            httpTransport,
            jsonFactory,
            HttpCredentialsAdapter(credentials)
        ).setApplicationName("BHU Ji Academic").build()
    }

    suspend fun fetchFilesFromFolder(folderId: String): List<DriveFile> = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService()
            val result = service.files().list()
                .setQ("'$folderId' in parents and trashed = false")
                .setFields("files(id, name, mimeType, size, thumbnailLink, webContentLink)")
                .execute()

            result.files?.map { file ->
                DriveFile(
                    id = file.id ?: "",
                    name = file.name ?: "Unnamed",
                    mimeType = file.mimeType ?: "",
                    thumbnailLink = file.thumbnailLink,
                    webContentLink = file.webContentLink
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}