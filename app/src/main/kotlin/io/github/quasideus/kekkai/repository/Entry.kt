package io.github.quasideus.kekkai.repository

/**
 * A decrypted secret-store entry. [password] is the primary secret (the
 * entry's first line, in pass's own convention); [fields] holds every
 * subsequent `key: value` line, e.g. "login", "url", or any custom metadata
 * key such as "account_number".
 */
data class Entry(
    val password: String,
    val fields: Map<String, String> = emptyMap(),
)
