package com.reyzie.hymns.interop

/**
 * Rules for the hand-off between CSI Hymns Book and a partner app such as YKC One: the
 * partner opens the Carols church list, and a button here sends the user back.
 *
 * The two apps talk only if both are on the allowlist (kept in app_config, so a whitelabeled
 * partner can be added without a new release). Callers are identified by the system, never
 * by anything the caller writes in the link, and nothing is sent between the apps.
 */
object CarolInterop {
    /** An action a partner app declares on its main screen so this app can bring it forward. */
    const val ACTION_RETURN_TO_CALLER = "com.reyzie.hymns.action.RETURN_TO_CALLER"

    const val LINK_SCHEME = "csihymns"
    const val LINK_HOST = "carols"

    /** Used until the first app_config sync, or if it has never been reachable. */
    val DEFAULT_ALLOWED: Set<String> = setOf("com.reyzie.hymns", "com.reyzie.oneykc")

    private const val REFERRER_PREFIX = "android-app://"
    private val PACKAGE_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val SEPARATORS = Regex("[\\s,;\\[\\]\"']+")

    /**
     * Reads the allowlist saved in app_config (a JSON array or a comma separated list).
     * No saved value means the built-in pair; an empty list means nobody is allowed.
     */
    fun parseAllowlist(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return DEFAULT_ALLOWED
        return raw.split(SEPARATORS).filter { PACKAGE_NAME.matches(it) }.toSet()
    }

    /** The package named by an `android-app://<package>` referrer, or null for anything else. */
    fun packageFromReferrer(referrer: String?): String? {
        if (referrer == null || !referrer.startsWith(REFERRER_PREFIX)) return null
        val name = referrer.removePrefix(REFERRER_PREFIX).substringBefore('/')
        return name.takeIf { PACKAGE_NAME.matches(it) }
    }

    fun isCarolsLink(scheme: String?, host: String?): Boolean =
        scheme == LINK_SCHEME && host == LINK_HOST

    /**
     * The partner app to accept a call from, or null to ignore the call. The system's referrer
     * is trusted only when the caller did not supply one itself (a caller can write its own
     * referrer into the intent, which would let any app pretend to be a partner).
     */
    fun resolveCaller(
        referrer: String?,
        referrerWrittenByCaller: Boolean,
        allowed: Set<String>,
        ownPackage: String,
    ): String? {
        if (referrerWrittenByCaller || ownPackage !in allowed) return null
        val caller = packageFromReferrer(referrer) ?: return null
        return caller.takeIf { it != ownPackage && it in allowed }
    }

    /** Whether the "back" button may take the user to [packageName]. */
    fun canReturnTo(packageName: String, allowed: Set<String>, ownPackage: String): Boolean =
        ownPackage in allowed && packageName != ownPackage && packageName in allowed
}
