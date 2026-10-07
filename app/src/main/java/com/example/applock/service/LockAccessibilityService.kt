package com.example.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.applock.data.AppDatabase
import com.example.applock.ui.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Watches for foreground-app changes. When the app that just came to the
 * front is in the locked-apps list (and isn't already unlocked for this
 * "session"), it launches the lock overlay on top of it.
 */
class LockAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Packages unlocked since the screen last turned off / service restarted.
    private val unlockedThisSession = mutableSetOf<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg == packageName) return // don't lock ourselves
        if (unlockedThisSession.contains(pkg)) return

        scope.launch {
            val lockedPackages = AppDatabase.get(applicationContext)
                .lockedAppDao()
                .allPackageNamesOnce()

            if (lockedPackages.contains(pkg)) {
                val intent = Intent(applicationContext, LockOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(LockOverlayActivity.EXTRA_TARGET_PACKAGE, pkg)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() { /* no-op */ }

    fun markUnlocked(pkg: String) {
        unlockedThisSession.add(pkg)
    }

    companion object {
        // Simple static bridge so the overlay activity can mark a package
        // unlocked without needing a bound service connection.
        var instance: LockAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
