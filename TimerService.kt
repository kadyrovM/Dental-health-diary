package com.example.lab3

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.Locale

class TimerService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var endTime: Long = 0
    private var totalTime: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val minutes = intent?.getIntExtra("TIMER_MINUTES", 0) ?: 0
        val seconds = intent?.getIntExtra("TIMER_SECONDS", 0) ?: 0
        totalTime = ((minutes * 60) + seconds) * 1000L
        endTime = System.currentTimeMillis() + totalTime

        createNotificationChannel()

        val notification = buildNotification(formatTime(totalTime))
        startForeground(1, notification)

        startTimer()

        return START_NOT_STICKY
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                totalTime = millisUntilFinished
                updateNotification(formatTime(millisUntilFinished))
            }

            override fun onFinish() {
                val notification = NotificationCompat.Builder(this@TimerService, CHANNEL_ID)
                    .setContentTitle("Таймер завершен!")
                    .setContentText("Время вышло.")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setAutoCancel(true)
                    .build()
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(2, notification)
                stopSelf()
            }
        }.start()
    }

    private fun formatTime(millis: Long): String {
        val mins = (millis / 1000) / 60
        val secs = (millis / 1000) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Таймер",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(timeText: String): Notification {
        val intent = Intent(this, TimerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Идет таймер: $timeText")
            .setContentText("Нажмите, чтобы открыть приложение")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(timeText: String) {
        val notification = buildNotification(timeText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
    }
}