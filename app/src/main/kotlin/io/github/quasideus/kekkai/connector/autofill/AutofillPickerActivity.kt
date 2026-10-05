package io.github.quasideus.kekkai.connector.autofill

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import io.github.quasideus.kekkai.repository.Entry
import io.github.quasideus.kekkai.repository.Repository
import io.github.quasideus.kekkai.repository.pass.OpenPgpDecryptor
import io.github.quasideus.kekkai.repository.pass.PassRepository
import io.github.quasideus.kekkai.repository.pass.UserInteractionRequiredException
import io.github.quasideus.kekkai.ui.EntryPickerScreen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Launched via the Dataset's auth IntentSender; fills fields by Autofill
 * hint, always filling the focused field too as a fallback for unlabeled
 * ones. Cancels (no inline retry) if OpenKeychain needs first-use interaction.
 */
class AutofillPickerActivity : ComponentActivity() {

    private lateinit var repository: Repository
    private var fieldIds: List<AutofillId> = emptyList()
    private var fieldHints: List<String> = emptyList()
    private var focusedId: AutofillId? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        fieldIds = intent.getParcelableArrayListExtra<AutofillId>(
            AutofillKekkaiService.EXTRA_FIELD_IDS,
        ) ?: emptyList()
        fieldHints = intent.getStringArrayListExtra(AutofillKekkaiService.EXTRA_FIELD_HINTS)
            ?: emptyList()
        @Suppress("DEPRECATION")
        focusedId = intent.getParcelableExtra(AutofillKekkaiService.EXTRA_FOCUSED_ID)

        val storeDir = File(filesDir, "store")
        repository = PassRepository(storeDir, OpenPgpDecryptor(this))

        setContent {
            EntryPickerScreen(repository = repository, onEntrySelected = ::handleEntrySelected)
        }
    }

    private fun handleEntrySelected(entryName: String) {
        lifecycleScope.launch {
            try {
                val entry = withContext(Dispatchers.IO) { repository.getEntry(entryName) }
                finishWithDataset(entry)
            } catch (e: UserInteractionRequiredException) {
                cancel()
            } catch (e: Exception) {
                cancel()
            }
        }
    }

    private fun finishWithDataset(entry: Entry) {
        val datasetBuilder = android.service.autofill.Dataset.Builder()
        var filledAny = false

        for (i in fieldIds.indices) {
            val id = fieldIds[i]
            val hint = fieldHints.getOrNull(i)?.ifBlank { null }
            val value = valueForHint(hint, entry) ?: if (id == focusedId) entry.password else null
            if (value != null) {
                datasetBuilder.setValue(id, AutofillValue.forText(value))
                filledAny = true
            }
        }

        if (!filledAny) {
            cancel()
            return
        }

        val replyIntent = Intent().putExtra(
            AutofillManager.EXTRA_AUTHENTICATION_RESULT,
            datasetBuilder.build(),
        )
        setResult(Activity.RESULT_OK, replyIntent)
        finish()
    }

    private fun valueForHint(hint: String?, entry: Entry): String? = when (hint) {
        // newUsername/newPassword have no View constants; spelled out literally.
        View.AUTOFILL_HINT_USERNAME,
        View.AUTOFILL_HINT_EMAIL_ADDRESS,
        "newUsername",
        -> entry.fields["login"] ?: entry.fields["username"]

        View.AUTOFILL_HINT_PASSWORD,
        "newPassword",
        -> entry.password

        null -> null
        else -> entry.fields[hint]
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}
