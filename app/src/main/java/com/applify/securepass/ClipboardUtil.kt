package com.applify.securepass

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object ClipboardUtil {

    /**
     * Copies text to clipboard and schedules a clear after the given seconds.
     * If the same text is still in the clipboard when the timer fires, it gets wiped.
     */
    @JvmStatic
    fun copyAndClear(context: Context, label: String?, text: String, seconds: Int) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            // Only clear if the content is still what we copied
            val currentClip = clipboard.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                val currentText = currentClip.getItemAt(0).text.toString()
                if (text == currentText) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    Toast.makeText(context, "Clipboard cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }, seconds * 1000L)
    }
}