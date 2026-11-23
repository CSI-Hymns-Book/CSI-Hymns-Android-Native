package com.reyzie.hymns.interop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarolInteropTest {
    private val own = "com.reyzie.hymns"
    private val ykc = "com.reyzie.oneykc"
    private val allowed = setOf(own, ykc)

    @Test
    fun `no saved setting means the built-in pair is allowed`() {
        assertEquals(setOf(own, ykc), CarolInterop.parseAllowlist(null))
        assertEquals(setOf(own, ykc), CarolInterop.parseAllowlist("   "))
    }

    @Test
    fun `allowlist reads a pretty-printed JSON array`() {
        val raw = "[\n  \"com.reyzie.hymns\",\n  \"com.reyzie.oneykc\",\n  \"org.church.whitelabel\"\n]"

        assertEquals(setOf(own, ykc, "org.church.whitelabel"), CarolInterop.parseAllowlist(raw))
    }

    @Test
    fun `allowlist also reads a comma separated list and drops junk`() {
        val parsed = CarolInterop.parseAllowlist("com.reyzie.hymns, com.reyzie.oneykc; not a package, ..bad")

        assertEquals(setOf(own, ykc), parsed)
    }

    @Test
    fun `an empty list allows nobody`() {
        assertTrue(CarolInterop.parseAllowlist("[]").isEmpty())
    }

    @Test
    fun `referrer gives the calling package`() {
        assertEquals(ykc, CarolInterop.packageFromReferrer("android-app://com.reyzie.oneykc"))
        assertEquals(ykc, CarolInterop.packageFromReferrer("android-app://com.reyzie.oneykc/https/example.com"))
    }

    @Test
    fun `web and malformed referrers are not packages`() {
        assertNull(CarolInterop.packageFromReferrer(null))
        assertNull(CarolInterop.packageFromReferrer("https://evil.example.com"))
        assertNull(CarolInterop.packageFromReferrer("android-app://"))
        assertNull(CarolInterop.packageFromReferrer("android-app://not a package"))
    }

    @Test
    fun `an allowed caller is accepted`() {
        assertEquals(ykc, CarolInterop.resolveCaller("android-app://com.reyzie.oneykc", false, allowed, own))
    }

    @Test
    fun `an app that is not on the list is refused`() {
        assertNull(CarolInterop.resolveCaller("android-app://com.evil.app", false, allowed, own))
    }

    @Test
    fun `a referrer the caller wrote itself is refused`() {
        assertNull(CarolInterop.resolveCaller("android-app://com.reyzie.oneykc", true, allowed, own))
    }

    @Test
    fun `nothing is accepted when this app is not on the list either`() {
        assertNull(CarolInterop.resolveCaller("android-app://com.reyzie.oneykc", false, setOf(ykc), own))
    }

    @Test
    fun `the app never accepts a call from itself`() {
        assertNull(CarolInterop.resolveCaller("android-app://com.reyzie.hymns", false, allowed, own))
    }

    @Test
    fun `a whitelabel app is accepted once it is added to the list`() {
        val withWhitelabel = allowed + "org.church.whitelabel"

        assertEquals(
            "org.church.whitelabel",
            CarolInterop.resolveCaller("android-app://org.church.whitelabel", false, withWhitelabel, own),
        )
        assertNull(CarolInterop.resolveCaller("android-app://org.church.whitelabel", false, allowed, own))
    }

    @Test
    fun `only the carols link is recognised`() {
        assertTrue(CarolInterop.isCarolsLink("csihymns", "carols"))
        assertFalse(CarolInterop.isCarolsLink("csihymns", "donation_result"))
        assertFalse(CarolInterop.isCarolsLink("https", "carols"))
        assertFalse(CarolInterop.isCarolsLink(null, null))
    }

    @Test
    fun `returning is only allowed to listed apps`() {
        assertTrue(CarolInterop.canReturnTo(ykc, allowed, own))
        assertFalse(CarolInterop.canReturnTo("com.evil.app", allowed, own))
        assertFalse(CarolInterop.canReturnTo(ykc, setOf(ykc), own))
    }
}
