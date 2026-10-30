package com.applify.securepass

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.autofill.Field
import android.service.autofill.Presentations
import android.view.ViewGroup
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.net.toUri
import com.applify.securepass.data.VaultManager

class AutofillUnlockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pin = EditText(this).apply {
            hint = "Six-digit vault PIN"
            inputType = 18
            setSingleLine()
        }
        val unlock = Button(this).apply { text = "Unlock and fill" }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(pin, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(unlock, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        unlock.setOnClickListener {
            try {
                val manager = VaultManager(this)
                manager.unlock(pin.text.toString())
                try {
                    val entries = manager.loadEntries()
                    val host = intent.getStringExtra("host").orEmpty()
                    val target = intent.getStringExtra("target").orEmpty()
                    val userId = intent.autofillIdExtra<AutofillId>("userId")
                    val passwordId = intent.autofillIdExtra<AutofillId>("passwordId")
                    val response = FillResponse.Builder()
                    entries.filter { item ->
                        val saved = runCatching {
                            (if (item.website.contains("://")) item.website else "https://${item.website}").toUri().host
                        }.getOrNull()
                        saved.equals(host, true) || item.website.equals(target, true)
                    }.forEach { item ->
                        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                            setTextViewText(android.R.id.text1, "${item.website} · ${item.username}")
                        }
                        response.addDataset(loginDataset(presentation, userId, passwordId, item.username, item.password))
                    }
                    val result = android.content.Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response.build())
                    setResult(RESULT_OK, result)
                    finish()
                } finally {
                    pin.text?.clear()
                    VaultManager.clearGlobalKey()
                }
            } catch (_: Exception) {
                Toast.makeText(this, "Could not unlock vault. Check your PIN.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginDataset(
        presentation: RemoteViews,
        userId: AutofillId?,
        passwordId: AutofillId?,
        username: String,
        password: String
    ): Dataset {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            return Dataset.Builder(presentation).apply {
                userId?.let { setValue(it, AutofillValue.forText(username)) }
                passwordId?.let { setValue(it, AutofillValue.forText(password)) }
            }.build()
        }
        val presentations = Presentations.Builder().setMenuPresentation(presentation).build()
        return Dataset.Builder(presentations).apply {
            userId?.let { setField(it, Field.Builder().setValue(AutofillValue.forText(username)).setPresentations(presentations).build()) }
            passwordId?.let { setField(it, Field.Builder().setValue(AutofillValue.forText(password)).setPresentations(presentations).build()) }
        }.build()
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T : Parcelable> Intent.autofillIdExtra(name: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableExtra(name, T::class.java)
        else getParcelableExtra(name)
}
