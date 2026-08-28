
package cvam.dignity.bhumess.drive

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val thumbnailLink: String? = null,
    val webContentLink: String? = null
)

data class FolderEntry(
    val id: String,
    val name: String
)
