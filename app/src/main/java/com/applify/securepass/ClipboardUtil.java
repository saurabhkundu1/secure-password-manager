package com.applify.securepass;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ClipboardUtil {

    /**
     * Copies text to clipboard and schedules a clear after the given seconds.
     * If the same text is still in the clipboard when the timer fires, it gets wiped.
     */
    public static void copyAndClear(Context context, String label, String text, int seconds) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Only clear if the content is still what we copied
            ClipData currentClip = clipboard.getPrimaryClip();
            if (currentClip != null && currentClip.getItemCount() > 0) {
                String currentText = currentClip.getItemAt(0).getText().toString();
                if (text.equals(currentText)) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
                    Toast.makeText(context, "Clipboard cleared", Toast.LENGTH_SHORT).show();
                }
            }
        }, seconds * 1000L);
    }
}