package com.reyzie.hymns.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminRolesAndCacheTest {
    @Test
    fun `media_control is a known role`() {
        assertEquals(AdminPrefs.AdminRole.MEDIA_CONTROL, AdminPrefs.AdminRole.fromKey(" Media_Control "))
    }

    @Test
    fun `an admin can be given only media_control`() {
        val roles = AdminPrefs.parseAdminRoles("""{"a@x.com": ["media_control"], "b@x.com": ["lyrics"]}""")

        assertEquals(setOf(AdminPrefs.AdminRole.MEDIA_CONTROL), roles["a@x.com"])
        assertFalse(AdminPrefs.AdminRole.MEDIA_CONTROL in roles.getValue("b@x.com"))
    }

    @Test
    fun `the full admin role is kept separate from media_control`() {
        val roles = AdminPrefs.parseAdminRoles("""{"boss@x.com": "admin"}""")

        assertTrue(AdminPrefs.AdminRole.ADMIN in roles.getValue("boss@x.com"))
    }

    @Test
    fun `a new refresh version changes the MIDI cache fingerprint`() {
        val before = MidiFileCache.configFingerprint(RemoteAppConfig(midiRefreshVersion = "1"))
        val after = MidiFileCache.configFingerprint(RemoteAppConfig(midiRefreshVersion = "2"))

        assertTrue(before != after)
        assertEquals(before, MidiFileCache.configFingerprint(RemoteAppConfig(midiRefreshVersion = "1")))
    }

    @Test
    fun `cache sizes read naturally`() {
        assertEquals("512 B", CacheCleaner.format(512))
        assertEquals("2 KB", CacheCleaner.format(2 * 1024))
        assertEquals("3.5 MB", CacheCleaner.format((3.5 * 1024 * 1024).toLong()))
    }
}
