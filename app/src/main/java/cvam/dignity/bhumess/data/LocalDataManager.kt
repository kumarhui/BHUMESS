package cvam.dignity.bhumess.data

import android.content.Context

object LocalDataManager {

    private const val PREFS_NAME = "bhumess_prefs"

    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_FACULTY = "user_faculty"
    private const val KEY_USER_ROLE = "user_role"

    fun getProfileName(context: Context): String =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(
            KEY_USER_NAME,
            "User"
        ) ?: "User"

    fun getProfilePhone(context: Context): String =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(
            KEY_USER_PHONE,
            ""
        ) ?: ""

    fun getProfileEmail(context: Context): String =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(
            KEY_USER_EMAIL,
            ""
        ) ?: ""

    fun getUserId(context: Context): String =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(
            KEY_USER_ID,
            "Unknown"
        ) ?: "Unknown"

    fun getUserFaculty(context: Context): String =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(
            KEY_USER_FACULTY,
            "General"
        ) ?: "General"

    fun getUserRole(context: Context): String =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(
            KEY_USER_ROLE,
            "STUDENT"
        ) ?: "STUDENT"

    fun saveProfileInfo(
        context: Context,
        name: String,
        phone: String,
        email: String? = null,
        id: String? = null,
        faculty: String? = null,
        role: String? = null
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .apply {

                putString(KEY_USER_NAME, name)
                putString(KEY_USER_PHONE, phone)

                email?.let {
                    putString(KEY_USER_EMAIL, it)
                }

                id?.let {
                    putString(KEY_USER_ID, it)
                }

                faculty?.let {
                    putString(KEY_USER_FACULTY, it)
                }

                role?.let {
                    putString(KEY_USER_ROLE, it)
                }

                apply()
            }
    }
}