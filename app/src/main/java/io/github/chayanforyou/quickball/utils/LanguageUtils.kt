package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import io.github.chayanforyou.quickball.domain.PreferenceManager
import java.util.Locale

object LanguageUtils {

    enum class Language(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        HINDI("hi", "हिंदी"),
        CHINESE("zh", "中文")
    }

    fun applyLanguage(context: Context): Context {
        val languageCode = PreferenceManager.getLanguage(context)
        return updateContextLocale(context, languageCode)
    }

    private fun updateContextLocale(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }

    fun getCurrentLanguage(context: Context): Language {
        val languageCode = PreferenceManager.getLanguage(context)
        return Language.entries.find { it.code == languageCode } ?: Language.ENGLISH
    }

    fun getAllLanguages(): List<Language> {
        return Language.entries
    }
}