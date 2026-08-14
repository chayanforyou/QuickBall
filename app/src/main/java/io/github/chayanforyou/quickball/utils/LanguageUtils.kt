package io.github.chayanforyou.quickball.utils

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import io.github.chayanforyou.quickball.domain.AppPreference
import java.util.Locale

object LanguageUtils {

    enum class Language(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        HINDI("hi", "हिंदी"),
        CHINESE("zh", "中文"),
        ITALIAN("it", "Italiano"),
        PORTUGUESE("pt", "Português"),
        SPANISH("es", "Español"),
        GERMAN("de", "Deutsch");

        companion object {
            private val codeMap = entries.associateBy { it.code }

            fun fromCode(code: String?): Language = codeMap[code] ?: ENGLISH
        }
    }

    fun getLocalizedContext(context: Context): Context {
        val languageCode = AppPreference.getInstance(context).language
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        return context.createConfigurationContext(config)
    }

    fun setLanguage(context: Context, language: Language) {
        AppPreference.getInstance(context).language = language.code

        val locale = Locale.forLanguageTag(language.code)
        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.forLanguageTags(language.code)
        } else {
            val config = Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }

        context.findActivity()?.recreate()
    }

    fun getCurrentLanguage(context: Context): Language {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val tag = context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales?.takeIf { !it.isEmpty }?.get(0)?.language
            if (!tag.isNullOrEmpty()) {
                return Language.fromCode(tag)
            }
        }
        return Language.fromCode(AppPreference.getInstance(context).language)
    }

    fun getAllLanguages(): List<Language> = Language.entries

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
