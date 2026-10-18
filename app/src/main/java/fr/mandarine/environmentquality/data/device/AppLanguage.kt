package fr.mandarine.environmentquality.data.device

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app's own language, French or English, whatever the phone's. Android 13+
 * stores it with the system's per-app language; older versions keep it in
 * preferences and apply it to each activity's context.
 */
object AppLanguage {

    const val FRENCH = "fr"
    const val ENGLISH = "en"

    private const val PREFERENCES = "language"
    private const val KEY = "tag"

    /** The language the app is showing now. */
    fun current(context: Context): String {
        val locale = context.resources.configuration.locales[0]
        return if (locale.language == ENGLISH) ENGLISH else FRENCH
    }

    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The system recreates the activity with the new configuration.
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
            activity.recreate()
        }
    }

    /** Before Android 13, a context in the chosen language; from 13 on, [base] itself. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, null) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(configuration)
    }
}
