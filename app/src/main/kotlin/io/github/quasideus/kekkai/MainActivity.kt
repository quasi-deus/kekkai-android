package io.github.quasideus.kekkai

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import io.github.quasideus.kekkai.repository.Repository
import io.github.quasideus.kekkai.repository.pass.GitSync
import io.github.quasideus.kekkai.repository.pass.OpenPgpDecryptor
import io.github.quasideus.kekkai.repository.pass.PassRepository
import io.github.quasideus.kekkai.repository.pass.UserInteractionRequiredException
import io.github.quasideus.kekkai.ui.EntryPickerScreen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CLIPBOARD_CLEAR_DELAY_MS = 45_000L
private const val PREFS_NAME = "kekkai"
private const val PREF_STORE_REMOTE_URL = "store_remote_url"

/**
 * Standalone entry point: picker -> clipboard copy (Autofill's fallback path).
 * No onboarding UI yet; set `store_remote_url` in SharedPreferences manually.
 */
class MainActivity : ComponentActivity() {

    private lateinit var decryptor: OpenPgpDecryptor
    private lateinit var repository: Repository
    private var pendingRetryEntry: String? = null

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        val entryName = pendingRetryEntry
        pendingRetryEntry = null
        if (entryName != null) {
            handleEntrySelected(entryName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storeDir = File(filesDir, "store")
        decryptor = OpenPgpDecryptor(this)
        repository = PassRepository(storeDir, decryptor)

        lifecycleScope.launch {
            val remoteUrl = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_STORE_REMOTE_URL, null)
            if (!remoteUrl.isNullOrBlank()) {
                withContext(Dispatchers.IO) {
                    runCatching { GitSync.cloneOrPull(remoteUrl, storeDir) }
                        .onFailure { e -> showToast("Store sync failed: ${e.message}") }
                }
            }
        }

        setContent {
            EntryPickerScreen(repository = repository, onEntrySelected = ::handleEntrySelected)
        }
    }

    private fun handleEntrySelected(entryName: String) {
        lifecycleScope.launch {
            try {
                val entry = withContext(Dispatchers.IO) { repository.getEntry(entryName) }
                copyToClipboard(entry.password)
            } catch (e: UserInteractionRequiredException) {
                pendingRetryEntry = entryName
                consentLauncher.launch(
                    IntentSenderRequest.Builder(e.pendingIntent.intentSender).build(),
                )
            } catch (e: Exception) {
                showToast("Failed to decrypt $entryName: ${e.message}")
            }
        }
    }

    private fun copyToClipboard(password: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("kekkai", password)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
        showToast("Copied to clipboard")

        lifecycleScope.launch {
            kotlinx.coroutines.delay(CLIPBOARD_CLEAR_DELAY_MS)
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
