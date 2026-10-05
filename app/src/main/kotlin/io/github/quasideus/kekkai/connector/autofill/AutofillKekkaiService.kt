package io.github.quasideus.kekkai.connector.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.widget.RemoteViews

/**
 * One generic "Search kekkai" suggestion per field, no automatic domain/
 * package matching (same fallback Bitwarden/1Password/Google use).
 * [onSaveRequest] is a no-op; saving credentials back isn't in scope.
 */
class AutofillKekkaiService : AutofillService() {

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val context = request.fillContexts.last()
        val fields = collectAutofillFields(context.structure)
        if (fields.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val focusedId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.focusedId
        } else {
            null
        }

        val pickerIntent = Intent(this, AutofillPickerActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_FIELD_IDS, ArrayList(fields.map { it.first }))
            putStringArrayListExtra(EXTRA_FIELD_HINTS, ArrayList(fields.map { it.second ?: "" }))
            if (focusedId != null) putExtra(EXTRA_FOCUSED_ID, focusedId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            pickerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Search kekkai…")
        }

        val dataset = Dataset.Builder().apply {
            setAuthentication(pendingIntent.intentSender)
            for ((id, _) in fields) {
                setValue(id, null, presentation)
            }
        }.build()

        callback.onSuccess(FillResponse.Builder().addDataset(dataset).build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    /** Walks the screen's view tree for every autofillable field + its first hint. */
    private fun collectAutofillFields(structure: AssistStructure): List<Pair<AutofillId, String?>> {
        val results = mutableListOf<Pair<AutofillId, String?>>()
        for (i in 0 until structure.windowNodeCount) {
            collectFromViewNode(structure.getWindowNodeAt(i).rootViewNode, results)
        }
        return results
    }

    private fun collectFromViewNode(
        node: AssistStructure.ViewNode,
        results: MutableList<Pair<AutofillId, String?>>,
    ) {
        val id = node.autofillId
        if (id != null && node.autofillType != android.view.View.AUTOFILL_TYPE_NONE) {
            results.add(id to node.autofillHints?.firstOrNull())
        }
        for (i in 0 until node.childCount) {
            collectFromViewNode(node.getChildAt(i), results)
        }
    }

    companion object {
        const val EXTRA_FIELD_IDS = "io.github.quasideus.kekkai.extra.FIELD_IDS"
        const val EXTRA_FIELD_HINTS = "io.github.quasideus.kekkai.extra.FIELD_HINTS"
        const val EXTRA_FOCUSED_ID = "io.github.quasideus.kekkai.extra.FOCUSED_ID"
    }
}
