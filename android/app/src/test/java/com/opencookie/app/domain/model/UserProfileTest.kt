package com.opencookie.app.domain.model

import com.opencookie.app.util.PublicKey
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileTest {

    @Test
    fun effectiveCallsToday_returnsCallsTodayWhenDayMatches() {
        val currentTimeMs = 1_700_000_000_000L
        val currentDay = (currentTimeMs / 1000 / 86400).toInt()
        val profile = UserProfile(
            owner = PublicKey(ByteArray(32)),
            totalCalls = 10,
            lastDay = currentDay,
            callsToday = 3,
            bump = 255,
        )

        assertEquals(3, profile.effectiveCallsToday(currentTimeMs))
    }

    @Test
    fun effectiveCallsToday_returnsZeroWhenDayIsDifferent() {
        val currentTimeMs = 1_700_000_000_000L
        val currentDay = (currentTimeMs / 1000 / 86400).toInt()
        val yesterdayDay = currentDay - 1
        val profile = UserProfile(
            owner = PublicKey(ByteArray(32)),
            totalCalls = 10,
            lastDay = yesterdayDay,
            callsToday = 3,
            bump = 255,
        )

        assertEquals(0, profile.effectiveCallsToday(currentTimeMs))
    }
}
