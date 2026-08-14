package cvam.dignity.bhumess.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ErrorTracker {
    var lastErrorMessage by mutableStateOf<String?>(null)
    var lastStackTrace by mutableStateOf<String?>(null)

    fun logError(e: Throwable) {
        lastErrorMessage = e.localizedMessage ?: e.message ?: "Unknown Error"
        lastStackTrace = e.stackTraceToString()
    }
}