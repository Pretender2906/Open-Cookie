package com.opencookie.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private var cachedLocale: String? = null
    private var cachedMessages: List<String>? = null

    fun cookieMessage(index: Int): String {
        val messages = loadMessages()
        if (messages.isEmpty()) return "Your cookie is waiting."
        return messages[index % messages.size]
    }

    private fun loadMessages(): List<String> {
        val localeTag = Locale.getDefault().language
        if (cachedMessages != null && cachedLocale == localeTag) return cachedMessages!!

        val assetName = when (localeTag) {
            "uk" -> "messages_ua.json"
            "ru" -> "messages_ru.json"
            else -> "messages_en.json"
        }
        val messages = runCatching {
            context.assets.open(assetName).bufferedReader().use { reader ->
                json.decodeFromString<List<String>>(reader.readText())
            }
        }.getOrElse {
            context.assets.open("messages_en.json").bufferedReader().use { reader ->
                json.decodeFromString<List<String>>(reader.readText())
            }
        }
        cachedLocale = localeTag
        cachedMessages = messages
        return messages
    }
}
