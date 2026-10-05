package io.github.quasideus.kekkai.repository.pass

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import me.msfjarvis.openpgpktx.util.OpenPgpApi
import me.msfjarvis.openpgpktx.util.OpenPgpServiceConnection

/** OpenKeychain needs user action (first-use grant, unlock) first; launch [pendingIntent] and retry. */
class UserInteractionRequiredException(val pendingIntent: PendingIntent) : Exception()

/** Decrypts via OpenKeychain's Remote OpenPGP API — no bundled GPG/pinentry. */
class OpenPgpDecryptor(
    private val context: Context,
    private val providerPackageName: String = "org.sufficientlysecure.keychain",
) {
    private var connection: OpenPgpServiceConnection? = null

    private suspend fun ensureBound(): OpenPgpServiceConnection = suspendCoroutine { cont ->
        val existing = connection
        if (existing != null && existing.isBound) {
            cont.resume(existing)
            return@suspendCoroutine
        }
        val conn = OpenPgpServiceConnection(
            context,
            providerPackageName,
            object : OpenPgpServiceConnection.OnBound {
                override fun onBound(service: org.openintents.openpgp.IOpenPgpService2) {
                    cont.resume(connection!!)
                }

                override fun onError(e: Exception) {
                    cont.resumeWithException(e)
                }
            },
        )
        connection = conn
        conn.bindToService()
    }

    /** Decrypts [cipherText] (raw .gpg file bytes) and returns the plaintext. */
    suspend fun decrypt(cipherText: ByteArray): String {
        val conn = ensureBound()
        val service = conn.service ?: error("OpenKeychain service not bound")
        val api = OpenPgpApi(context, service)

        val input = ByteArrayInputStream(cipherText)
        val output = ByteArrayOutputStream()
        val intent = Intent(OpenPgpApi.ACTION_DECRYPT_VERIFY)

        val result = api.executeApi(intent, input, output)
            ?: error("OpenKeychain returned no result")

        return when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> output.toString(Charsets.UTF_8.name())
            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                val pendingIntent = result.getParcelableExtra<PendingIntent>(OpenPgpApi.RESULT_INTENT)
                    ?: error("OpenKeychain requested user interaction but gave no PendingIntent")
                throw UserInteractionRequiredException(pendingIntent)
            }
            else -> {
                val error = result.getParcelableExtra<org.openintents.openpgp.OpenPgpError>(
                    OpenPgpApi.RESULT_ERROR,
                )
                error("OpenKeychain decrypt failed: ${error?.message ?: "unknown error"}")
            }
        }
    }

    fun unbind() {
        connection?.unbindFromService()
        connection = null
    }
}
