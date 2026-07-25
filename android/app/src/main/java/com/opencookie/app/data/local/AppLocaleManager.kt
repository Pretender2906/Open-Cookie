package com.opencookie.app.data.local

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.opencookie.app.domain.model.AppLanguage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLocaleManager @Inject constructor() {

    fun getCurrentLanguage(): AppLanguage =
        fromLocaleList(AppCompatDelegate.getApplicationLocales())

    fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(language.toLocaleList())
    }

    private fun AppLanguage.toLocaleList(): LocaleListCompat = when (this) {
        AppLanguage.SystemDefault -> LocaleListCompat.getEmptyLocaleList()
        AppLanguage.English -> LocaleListCompat.forLanguageTags("en")
        AppLanguage.Ukrainian -> LocaleListCompat.forLanguageTags("uk")
        AppLanguage.Spanish -> LocaleListCompat.forLanguageTags("es")
        AppLanguage.ChineseSimplified -> LocaleListCompat.forLanguageTags("zh-CN")
    }

    private fun fromLocaleList(locales: LocaleListCompat): AppLanguage {
        if (locales.isEmpty) return AppLanguage.SystemDefault
        return when (locales[0]?.toLanguageTag()) {
            "en" -> AppLanguage.English
            "uk" -> AppLanguage.Ukrainian
            "es" -> AppLanguage.Spanish
            "zh-CN" -> AppLanguage.ChineseSimplified
            else -> AppLanguage.SystemDefault
        }
    }
}
