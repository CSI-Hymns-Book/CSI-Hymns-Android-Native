package com.reyzie.hymns.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoteAppConfigCoalesceTest {

    private val lastKnown = RemoteAppConfig(
        isChristmasTime = false,
        forceUpdateEnabled = true,
        forceUpdateMinVersion = "5.1.2",
        forceUpdateMinBuildNumber = 34L,
        forceUpdateMessage = "Please update",
        forceUpdateAndroidStoreUrl = "https://play.google.com/store/apps/details?id=com.reyzie.hymns",
        castEnabled = true,
        adminEmails = """{"admin@example.com":["admin"]}""",
        githubMidiToken = "ghp_cached_token",
        isMangaloreHymnsEnabled = true,
        midiHymnsRanges = "c.m.,l.m.",
        midiKeerthanesRanges = "1-50",
        paymentsEnabled = true
    )

    @Test
    fun failedFetchKeepsLastKnownSecretsAndForceUpdate() {
        val fetched = RemoteAppConfig()

        val merged = fetched.coalesce(lastKnown)

        assertEquals("ghp_cached_token", merged.githubMidiToken)
        assertEquals("""{"admin@example.com":["admin"]}""", merged.adminEmails)
        assertEquals("5.1.2", merged.forceUpdateMinVersion)
        assertEquals(34L, merged.forceUpdateMinBuildNumber)
        assertEquals(true, merged.forceUpdateEnabled)
        assertEquals("c.m.,l.m.", merged.midiHymnsRanges)
        assertEquals(true, merged.isMangaloreHymnsEnabled)
        assertEquals(true, merged.paymentsEnabled)
    }

    @Test
    fun partialFetchUpdatesOnlyReturnedKeys() {
        val fetched = RemoteAppConfig(
            isChristmasTime = true,
            isMangaloreHymnsEnabled = false
        )

        val merged = fetched.coalesce(lastKnown)

        assertEquals(true, merged.isChristmasTime)
        assertEquals(false, merged.isMangaloreHymnsEnabled)
        assertEquals("ghp_cached_token", merged.githubMidiToken)
        assertEquals("5.1.2", merged.forceUpdateMinVersion)
        assertEquals(true, merged.forceUpdateEnabled)
    }

    @Test
    fun explicitFalseAndEmptyWinOverPrevious() {
        val fetched = RemoteAppConfig(
            forceUpdateEnabled = false,
            isMangaloreHymnsEnabled = false,
            midiHymnsRanges = "",
            paymentsEnabled = false
        )

        val merged = fetched.coalesce(lastKnown)

        assertFalse(merged.forceUpdateEnabled!!)
        assertFalse(merged.isMangaloreHymnsEnabled!!)
        assertEquals("", merged.midiHymnsRanges)
        assertFalse(merged.paymentsEnabled!!)
        assertEquals("ghp_cached_token", merged.githubMidiToken)
    }
}
