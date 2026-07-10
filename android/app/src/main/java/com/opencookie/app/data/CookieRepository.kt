package com.opencookie.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private var cachedMessages: List<String>? = null

    fun cookieMessage(index: Int): String {
        val messages = loadMessages()
        if (messages.isEmpty()) return "Your cookie is waiting."
        return messages[index % messages.size]
    }

    private fun loadMessages(): List<String> {
        cachedMessages?.let { return it }

        val messages = runCatching {
            context.assets.open("messages_en.json").bufferedReader().use { reader ->
                json.decodeFromString<List<String>>(reader.readText())
            }
        }.getOrDefault(emptyList())

        cachedMessages = messages
        return messages
    }
}
