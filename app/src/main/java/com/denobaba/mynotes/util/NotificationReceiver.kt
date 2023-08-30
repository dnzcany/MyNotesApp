package com.denobaba.mynotes.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.denobaba.mynotes.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "NotesChannel", "Notes", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notes reminders" }

            notificationManager.createNotificationChannel(channel)
        }

        val noteId = intent?.getIntExtra("noteId", -1)
        val noteTitle = intent?.getStringExtra("noteTitle")

        val notification = NotificationCompat.Builder(context, "NotesChannel")
            .setContentTitle("Note Reminder")
            .setContentText("$noteTitle, you have a reminder about this note")
            .setSmallIcon(R.drawable.baseline_alarm_add_24)
            .build()

        notificationManager.notify(noteId ?: 0, notification)
    }


}
