package io.github.chayanforyou.quickball.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.chayanforyou.quickball.domain.PreferenceManager

object LanguageUtils {

    enum class Language(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        HINDI("hi", "हिंदी"),
        CHINESE("zh", "中文"),
        ITALIAN("it", "Italiano"),
        PORTUGUESE("pt", "Português"),
        SPANISH("es", "Español"),
        GERMAN("de", "Deutsch");
    }

    fun applyLanguage(context: Context) {
        val language = getCurrentLanguage(context)
        applyLocale(language.code)
    }

    fun setLanguage(context: Context, language: Language) {
        PreferenceManager.setLanguage(context, language.code)
        applyLocale(language.code)
    }

    fun getCurrentLanguage(context: Context): Language {
        val languageCode = PreferenceManager.getLanguage(context)
        return Language.entries.find { it.code == languageCode } ?: Language.ENGLISH
    }

    fun getAllLanguages(): List<Language> {
        return Language.entries
    }

    private fun applyLocale(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }
}
