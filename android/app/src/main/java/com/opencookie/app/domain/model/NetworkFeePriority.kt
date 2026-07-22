package com.opencookie.app.domain.model

enum class NetworkFeePriority {
    Standard,
    Fast,
    ;

    companion object {
        const val STORED_STANDARD = "standard"
        const val STORED_FAST = "fast"

        fun fromStored(value: String?): NetworkFeePriority = when (value) {
            STORED_FAST -> Fast
            STORED_STANDARD, null -> Standard
            else -> Standard
        }
    }

    fun toStoredValue(): String = when (this) {
        Standard -> STORED_STANDARD
        Fast -> STORED_FAST
    }
}
