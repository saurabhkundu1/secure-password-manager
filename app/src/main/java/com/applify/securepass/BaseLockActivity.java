package com.applify.securepass;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.applify.securepass.data.VaultManager;

/**
 * Base activity that handles auto-locking the vault after a period of inactivity.
 */
public abstract class BaseLockActivity extends AppCompatActivity {

    private Handler lockHandler = new Handler(Looper.getMainLooper());
    private Runnable lockRunnable = this::lockVault;
    private long autoLockTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadAutoLockTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (VaultManager.getGlobalKey() == null) {
            // Vault is locked -> redirect
            lockVault();
        } else {
            resetTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetTimer();
    }

    private void loadAutoLockTime() {
        SharedPreferences prefs = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE);
        // Default to "Never" (0)
        autoLockTime = prefs.getLong("auto_lock_time", 0);
    }

    private void resetTimer() {
        stopTimer();
        if (autoLockTime > 0) {
            lockHandler.postDelayed(lockRunnable, autoLockTime);
        }
    }

    private void stopTimer() {
        lockHandler.removeCallbacks(lockRunnable);
    }

    protected void lockVault() {
        VaultManager.clearGlobalKey();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
