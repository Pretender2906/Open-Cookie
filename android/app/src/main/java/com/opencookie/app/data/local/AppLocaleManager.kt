package com.opencookie.app.data.local

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.opencookie.app.domain.model.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLocaleManager @Inject constructor(
    private val preferencesStore: PreferencesStore,
) {

    fun getCurrentLanguage(): AppLanguage = runBlocking {
        val language = preferencesStore.appLanguageFlow().first()
        if (language == AppLanguage.SystemDefault) {
            getEffectiveSystemLanguage()
        } else {
            language
        }
    }

    fun getEffectiveSystemLanguage(): AppLanguage {
        val locales = LocaleListCompat.getAdjustedDefault()
        return fromLocaleList(locales)
    }

    suspend fun applyLanguage(language: AppLanguage) {
        preferencesStore.saveAppLanguage(language)
        withContext(Dispatchers.Main) {
            AppCompatDelegate.setApplicationLocales(language.toLocaleList())
        }
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
        val first = locales[0] ?: return AppLanguage.SystemDefault
        return when (first.language) {
            "en" -> AppLanguage.English
            "uk" -> AppLanguage.Ukrainian
            "es" -> AppLanguage.Spanish
            "zh" -> AppLanguage.ChineseSimplified
            else -> AppLanguage.SystemDefault
        }
    }
}
