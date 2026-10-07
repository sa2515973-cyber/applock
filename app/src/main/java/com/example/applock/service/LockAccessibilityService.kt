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
 * "session"), it launches the lock overlay on top of it. A package's
 * unlocked session ends as soon as a different foreground app is seen,
 * so returning to the locked app always re-prompts for the PIN.
 */
class LockAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Packages unlocked since they were last in the foreground.
    private val unlockedThisSession = mutableSetOf<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg == packageName) return // don't lock ourselves

        // Any foreground app switch away from a previously-unlocked package
        // ends its unlocked session, so it locks again next time it's opened.
        if (pkg != lastUnlockedPackage) {
            unlockedThisSession.clear()
        }

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

    private var lastUnlockedPackage: String? = null

    fun markUnlocked(pkg: String) {
        unlockedThisSession.add(pkg)
        lastUnlockedPackage = pkg
    }

    /**
     * Disables this accessibility service entirely (equivalent to the user
     * switching it off in Settings > Accessibility). After this call, no more
     * foreground-app monitoring or lock overlays happen until the user
     * manually re-enables the service from system Settings.
     */
    fun selfDisable() {
        disableSelf()
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
