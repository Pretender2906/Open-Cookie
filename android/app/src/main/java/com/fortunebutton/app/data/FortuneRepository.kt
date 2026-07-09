package com.fortunebutton.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FortuneRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private var cachedLocale: String? = null
    private var cachedFortunes: List<String>? = null

    fun fortuneMessage(index: Int): String {
        val fortunes = loadFortunes()
        if (fortunes.isEmpty()) return "Your fortune awaits."
        return fortunes[index % fortunes.size]
    }

    private fun loadFortunes(): List<String> {
        val localeTag = Locale.getDefault().language
        if (cachedFortunes != null && cachedLocale == localeTag) return cachedFortunes!!

        val assetName = when (localeTag) {
            "uk" -> "fortunes_ua.json"
            "ru" -> "fortunes_ru.json"
            else -> "fortunes_en.json"
        }
        val fortunes = runCatching {
            context.assets.open(assetName).bufferedReader().use { reader ->
                json.decodeFromString<List<String>>(reader.readText())
            }
        }.getOrElse {
            context.assets.open("fortunes_en.json").bufferedReader().use { reader ->
                json.decodeFromString<List<String>>(reader.readText())
            }
        }
        cachedLocale = localeTag
        cachedFortunes = fortunes
        return fortunes
    }
}
