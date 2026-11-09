package com.applify.securepass

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.Toast
import java.util.concurrent.atomic.AtomicBoolean

object ClipboardUtil {

    /**
     * Copies text to clipboard and schedules a clear after the given seconds.
     * If the same text is still in the clipboard when the timer fires, it gets wiped.
     */
    @JvmStatic
    fun copyAndClear(context: Context, label: String?, text: String, seconds: Int) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val sensitiveFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClipDescription.EXTRA_IS_SENSITIVE
        } else {
            "android.content.extra.IS_SENSITIVE"
        }
        val clip = ClipData.newPlainText(label, text).apply {
            description.extras = PersistableBundle().apply { putBoolean(sensitiveFlag, true) }
        }
        val firstChange = AtomicBoolean(true)
        val replaced = AtomicBoolean(false)
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            if (!firstChange.getAndSet(false)) replaced.set(true)
        }
        clipboard.addPrimaryClipChangedListener(listener)
        clipboard.setPrimaryClip(clip)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // shortcut: process death cancels this timer; persistent expiry needs a safe replacement-clip check.
        Handler(Looper.getMainLooper()).postDelayed({
            clipboard.removePrimaryClipChangedListener(listener)
            if (!replaced.get()) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) clipboard.clearPrimaryClip()
                    else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, "Clipboard cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }, seconds * 1000L)
    }
}
