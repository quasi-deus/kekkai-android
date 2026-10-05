package io.github.quasideus.kekkai.repository

/**
 * Lists and reads entries from a secret-store backend, independent of which
 * one provides them (pass today; other backends can implement this later).
 * Mirrors the shape of cli/internal/repository.Repository on the Go side,
 * extended to the multi-field [Entry] instead of a bare password string.
 */
interface Repository {
    suspend fun listEntries(): List<String>
    suspend fun getEntry(name: String): Entry
}
