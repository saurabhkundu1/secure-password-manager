package com.applify.securepass

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.applify.securepass.data.VaultManager

/**
 * Base activity that handles auto-locking the vault after a period of inactivity.
 */
abstract class BaseLockActivity : AppCompatActivity() {

    private val lockHandler = Handler(Looper.getMainLooper())
    private val lockRunnable = Runnable { lockVault() }
    private var autoLockTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAutoLockTime()
    }

    override fun onResume() {
        super.onResume()
        if (VaultManager.globalKey == null) {
            // Vault is locked -> redirect
            lockVault()
        } else {
            resetTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetTimer()
    }

    private fun loadAutoLockTime() {
        val prefs = getSharedPreferences("secure_pass_prefs", MODE_PRIVATE)
        // Default to "Never" (0)
        autoLockTime = prefs.getLong("auto_lock_time", 0)
    }

    private fun resetTimer() {
        stopTimer()
        if (autoLockTime > 0) {
            lockHandler.postDelayed(lockRunnable, autoLockTime)
        }
    }

    private fun stopTimer() {
        lockHandler.removeCallbacks(lockRunnable)
    }

    protected open fun lockVault() {
        VaultManager.clearGlobalKey()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}