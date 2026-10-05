package io.github.quasideus.kekkai.repository.pass

import io.github.quasideus.kekkai.repository.Entry
import io.github.quasideus.kekkai.repository.Repository
import java.io.File

/** [Repository] over a local pass store (git+gpg dir); parses password + trailing `key: value` fields. */
class PassRepository(
    private val localDir: File,
    private val decryptor: OpenPgpDecryptor,
) : Repository {

    override suspend fun listEntries(): List<String> {
        if (!localDir.exists()) return emptyList()

        return localDir.walkTopDown()
            .onEnter { dir -> !dir.name.startsWith(".") }
            .filter { it.isFile && it.name.endsWith(".gpg") && !it.name.startsWith(".") }
            .map { it.relativeTo(localDir).path.removeSuffix(".gpg") }
            .sorted()
            .toList()
    }

    override suspend fun getEntry(name: String): Entry {
        val cleanName = name.removeSuffix(".gpg")
        val file = File(localDir, "$cleanName.gpg")
        require(file.exists()) { "entry does not exist: $cleanName" }

        val plaintext = decryptor.decrypt(file.readBytes())
        return parseEntry(plaintext)
    }

    private fun parseEntry(content: String): Entry {
        val lines = content.lineSequence().iterator()
        if (!lines.hasNext()) return Entry(password = "")

        val password = lines.next().trim()
        val fields = mutableMapOf<String, String>()
        while (lines.hasNext()) {
            val line = lines.next()
            val separator = line.indexOf(':')
            if (separator > 0) {
                val key = line.substring(0, separator).trim()
                val value = line.substring(separator + 1).trim()
                if (key.isNotEmpty()) fields[key] = value
            }
        }
        return Entry(password, fields)
    }
}
