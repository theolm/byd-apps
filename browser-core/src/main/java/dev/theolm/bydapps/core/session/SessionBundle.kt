package dev.theolm.bydapps.core.session

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * A cookie as stored by Gecko. Every attribute is preserved because a Google
 * session depends on HttpOnly, Secure, SameSite and partitioned cookies being
 * restored exactly as they were read.
 */
internal data class SessionCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val sameSite: String?,
    val expirationDate: Double?,
    val firstPartyDomain: String?,
    val partitionKey: String?,
    val storeId: String?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_NAME, name)
        put(KEY_VALUE, value)
        put(KEY_DOMAIN, domain)
        put(KEY_PATH, path)
        put(KEY_SECURE, secure)
        put(KEY_HTTP_ONLY, httpOnly)
        sameSite?.let { put(KEY_SAME_SITE, it) }
        expirationDate?.let { put(KEY_EXPIRATION, it) }
        firstPartyDomain?.let { put(KEY_FIRST_PARTY, it) }
        // The extension API expects a PartitionKey object, not its JSON text.
        partitionKey?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()?.let { put(KEY_PARTITION, it) }
        }
        storeId?.let { put(KEY_STORE, it) }
    }

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_VALUE = "value"
        private const val KEY_DOMAIN = "domain"
        private const val KEY_PATH = "path"
        private const val KEY_SECURE = "secure"
        private const val KEY_HTTP_ONLY = "httpOnly"
        private const val KEY_SAME_SITE = "sameSite"
        private const val KEY_EXPIRATION = "expirationDate"
        private const val KEY_FIRST_PARTY = "firstPartyDomain"
        private const val KEY_PARTITION = "partitionKey"
        private const val KEY_STORE = "storeId"

        fun fromJson(json: JSONObject): SessionCookie? {
            val name = json.optString(KEY_NAME).takeIf { it.isNotEmpty() } ?: return null
            val domain = json.optString(KEY_DOMAIN).takeIf { it.isNotEmpty() } ?: return null
            return SessionCookie(
                name = name,
                value = json.optString(KEY_VALUE),
                domain = domain,
                path = json.optString(KEY_PATH, "/").ifEmpty { "/" },
                secure = json.optBoolean(KEY_SECURE),
                httpOnly = json.optBoolean(KEY_HTTP_ONLY),
                sameSite = json.optString(KEY_SAME_SITE).takeIf { it.isNotEmpty() },
                expirationDate = if (json.has(KEY_EXPIRATION)) {
                    json.optDouble(KEY_EXPIRATION).takeIf { !it.isNaN() }
                } else {
                    null
                },
                firstPartyDomain = json.optString(KEY_FIRST_PARTY).takeIf { it.isNotEmpty() },
                partitionKey = if (json.has(KEY_PARTITION)) {
                    json.opt(KEY_PARTITION)?.toString()
                } else {
                    null
                },
                storeId = json.optString(KEY_STORE).takeIf { it.isNotEmpty() },
            )
        }
    }
}

/**
 * Serializable snapshot of the cookies that make up a signed-in session.
 *
 * The bundle travels between apps only through a signature-protected
 * provider, and never leaves the device.
 */
internal data class SessionBundle(
    val createdAt: Long,
    val signedIn: Boolean,
    val cookies: List<SessionCookie>,
) {
    /** Stable fingerprint used to decide whether a bundle actually changed. */
    val hash: String by lazy { sha256(canonical(cookies)) }

    fun toJson(): String = JSONObject().apply {
        put(KEY_VERSION, VERSION)
        put(KEY_CREATED_AT, createdAt)
        put(KEY_SIGNED_IN, signedIn)
        put(KEY_HASH, hash)
        put(KEY_COOKIES, JSONArray().apply { cookies.forEach { put(it.toJson()) } })
    }.toString()

    companion object {
        const val VERSION = 1
        private const val KEY_VERSION = "version"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_SIGNED_IN = "signedIn"
        private const val KEY_HASH = "hash"
        private const val KEY_COOKIES = "cookies"

        /** Returns null when the payload is malformed or from a newer schema. */
        fun fromJson(raw: String): SessionBundle? = runCatching {
            val json = JSONObject(raw)
            if (json.optInt(KEY_VERSION) != VERSION) return null
            val cookies = json.optJSONArray(KEY_COOKIES) ?: JSONArray()
            val entries = (0 until cookies.length()).mapNotNull { index ->
                cookies.optJSONObject(index)?.let(SessionCookie::fromJson)
            }
            SessionBundle(
                createdAt = json.optLong(KEY_CREATED_AT),
                signedIn = json.optBoolean(KEY_SIGNED_IN),
                cookies = entries,
            )
        }.getOrNull()

        private fun canonical(cookies: List<SessionCookie>): String = cookies
            .sortedWith(
                compareBy(
                    { it.domain },
                    { it.path },
                    { it.name },
                    { it.storeId },
                    { it.firstPartyDomain },
                    { it.partitionKey },
                )
            )
            .joinToString("\n") { cookie ->
                listOf(
                    cookie.domain,
                    cookie.path,
                    cookie.name,
                    cookie.value,
                    cookie.secure,
                    cookie.httpOnly,
                    cookie.sameSite,
                    cookie.expirationDate,
                    cookie.firstPartyDomain,
                    cookie.partitionKey,
                    cookie.storeId,
                ).joinToString("\t")
            }

        private fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
