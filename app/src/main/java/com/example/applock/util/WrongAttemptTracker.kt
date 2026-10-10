package com.example.applock.util

import android.content.Context

object WrongAttemptTracker {
    private const val PREFS = "applock_attempt_prefs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getCount(context: Context, pkg: String): Int =
        prefs(context).getInt(pkg, 0)

    fun increment(context: Context, pkg: String): Int {
        val next = getCount(context, pkg) + 1
        prefs(context).edit().putInt(pkg, next).apply()
        return next
    }

    fun reset(context: Context, pkg: String) {
        prefs(context).edit().remove(pkg).apply()
    }
}
