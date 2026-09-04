package dev.chiraitori.mizuki

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLocaleController {
    private const val PREFS_NAME = "mizuki_preferences"
    private const val KEY_LANGUAGE = "pref_language"

    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return context

        val language = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "SYSTEM")
            .orEmpty()
        if (language.isBlank() || language == "SYSTEM") return context

        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    fun apply(activity: Activity, language: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = if (language == "SYSTEM") {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(language)
            }
            activity.getSystemService(LocaleManager::class.java).applicationLocales = locales
        } else {
            activity.recreate()
        }
    }
}
