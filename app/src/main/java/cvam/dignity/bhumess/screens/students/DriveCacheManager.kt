package cvam.dignity.bhumess.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cvam.dignity.bhumess.api.DriveFile
import java.io.File
import java.lang.reflect.Type

object DriveCacheManager {
    private const val CACHE_DIR = "drive_metadata_cache"
    private val gson = Gson()

    fun saveFolderCache(context: Context, folderId: String, files: List<DriveFile>) {
        try {
            val directory = File(context.cacheDir, CACHE_DIR)
            if (!directory.exists()) directory.mkdirs()

            val cacheFile = File(directory, "cache_$folderId.json")
            val json = gson.toJson(files)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFolderCache(context: Context, folderId: String): List<DriveFile>? {
        return try {
            val cacheFile = File(context.cacheDir, "$CACHE_DIR/cache_$folderId.json")
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                val type: Type = object : TypeToken<List<DriveFile>>() {}.type
                gson.fromJson(json, type)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}