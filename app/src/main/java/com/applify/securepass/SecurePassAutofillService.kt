package com.applify.securepass

import android.service.autofill.*
import android.app.assist.AssistStructure
import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.os.Build
import android.service.autofill.Presentations
import android.view.autofill.AutofillValue
import android.text.InputType
import android.view.View
import android.widget.RemoteViews

class SecurePassAutofillService : AutofillService() {
    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        try {
            val structure = request.fillContexts.lastOrNull()?.structure
            if (structure == null) return callback.onSuccess(null)
            val ids = mutableListOf<android.view.autofill.AutofillId>()
            var userId: android.view.autofill.AutofillId? = null
            var passwordId: android.view.autofill.AutofillId? = null
            val passwordFields = mutableListOf<android.view.autofill.AutofillId>()
            val emailFields = mutableListOf<android.view.autofill.AutofillId>()
            val textFields = mutableListOf<android.view.autofill.AutofillId>()
            fun visit(node: AssistStructure.ViewNode) {
                val hints = node.autofillHints?.toSet().orEmpty()
                val id = node.autofillId
                val inputClass = node.inputType and InputType.TYPE_MASK_CLASS
                val variation = node.inputType and InputType.TYPE_MASK_VARIATION
                if (id != null && userId == null && hints.any { it in setOf("username", "emailAddress", "email") }) userId = id
                if (id != null && (hints.contains("password") ||
                        (inputClass == InputType.TYPE_CLASS_TEXT && variation in setOf(
                            InputType.TYPE_TEXT_VARIATION_PASSWORD,
                            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        )))) passwordFields.add(id)
                if (id != null && node.autofillType == View.AUTOFILL_TYPE_TEXT && inputClass == InputType.TYPE_CLASS_TEXT) {
                    if (variation in setOf(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)) emailFields.add(id)
                    else if (variation !in setOf(InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) textFields.add(id)
                }
                for (i in 0 until node.childCount) visit(node.getChildAt(i))
            }
            visit(structure.getWindowNodeAt(0).rootViewNode)
            passwordId = passwordFields.singleOrNull()
            if (passwordId == null) return callback.onSuccess(null)
            if (userId == null) {
                userId = emailFields.singleOrNull() ?: textFields.singleOrNull()
            }
            userId?.let(ids::add)
            passwordId?.let(ids::add)
            if (ids.isEmpty()) return callback.onSuccess(null)

            val target = structure.activityComponent?.packageName.orEmpty()
            val webDomain = structure.getWindowNodeAt(0).rootViewNode.webDomain
            val host = webDomain ?: target
            val unlock = Intent(this, AutofillUnlockActivity::class.java)
                .putExtra("host", host).putExtra("target", target)
                .putExtra("userId", userId).putExtra("passwordId", passwordId)
            val pending = PendingIntent.getActivity(this, 0, unlock, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, "Unlock SecurePass")
            }
            val response = FillResponse.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                response.setAuthentication(ids.toTypedArray(), pending.intentSender, Presentations.Builder().setMenuPresentation(presentation).build())
            } else {
                setLegacyAuthentication(response, ids.toTypedArray(), pending.intentSender, presentation)
            }
            callback.onSuccess(response.build())
        } catch (e: Exception) {
            callback.onFailure("SecurePass could not read the unlocked vault")
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) = callback.onSuccess()

    @Suppress("DEPRECATION")
    private fun setLegacyAuthentication(
        response: FillResponse.Builder,
        ids: Array<android.view.autofill.AutofillId>,
        authentication: android.content.IntentSender,
        presentation: RemoteViews
    ) {
        response.setAuthentication(ids, authentication, presentation)
    }
}
