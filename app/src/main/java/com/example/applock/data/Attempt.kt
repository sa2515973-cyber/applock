package com.example.applock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One failed-PIN attempt made against a locked app.
 * enteredPin is stored in PLAIN TEXT on purpose — this is a local
 * intruder-log feature, the whole point is for the owner to see
 * exactly what was typed. Do not sync this off-device.
 */
@Entity(tableName = "attempts")
data class Attempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val enteredPin: String,
    val photoPath: String?,
    val timestampMillis: Long
)
