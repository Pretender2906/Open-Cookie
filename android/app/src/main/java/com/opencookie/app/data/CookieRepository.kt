package com.opencookie.app.data

import android.content.Context
import com.opencookie.app.R
import com.opencookie.app.data.local.AppLocaleManager
import com.opencookie.app.domain.model.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val appLocaleManager: AppLocaleManager,
) {
    private var cachedMessages: Map<AppLanguage, List<String>> = emptyMap()

    fun cookieMessage(index: Int): String {
        val lang = appLocaleManager.getCurrentLanguage()
        val messages = loadMessages(lang)
        if (messages.isEmpty()) return context.getString(R.string.cookie_fallback_message)
        return messages[index % messages.size]
    }

    private fun loadMessages(language: AppLanguage): List<String> {
        cachedMessages[language]?.let { return it }

        val fileName = when (language) {
            AppLanguage.English -> "messages_en.json"
            AppLanguage.Ukrainian -> "messages_uk.json"
            AppLanguage.Spanish -> "messages_es.json"
            AppLanguage.ChineseSimplified -> "messages_zh.json"
            AppLanguage.SystemDefault -> "messages_en.json" // Should be resolved by getCurrentLanguage()
        }

        val messages = runCatching {
            context.assets.open(fileName).bufferedReader().use { reader ->
                json.decodeFromString<List<String>>(reader.readText())
            }
        }.getOrElse {
            if (language != AppLanguage.English) {
                loadMessages(AppLanguage.English)
            } else {
                emptyList()
            }
        }

        if (messages.isNotEmpty()) {
            cachedMessages = cachedMessages + (language to messages)
        }
        return messages
    }
}
