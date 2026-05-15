package com.reprush.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.reprush.app.R

class RestTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_REMAINING = "remaining"

        const val ACTION_TICK = "com.reprush.app.REST_TICK"
        const val ACTION_FINISH = "com.reprush.app.REST_FINISH"
        const val ACTION_ADD_THIRTY = "com.reprush.app.REST_ADD_THIRTY"
        const val ACTION_SKIP = "com.reprush.app.REST_SKIP"
    }

    private var countDownTimer: CountDownTimer? = null
    private var remainingSeconds = 0
    private var exerciseName = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD_THIRTY -> {
                remainingSeconds += 30
                restartTimer()
                return START_NOT_STICKY
            }
            ACTION_SKIP -> {
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        exerciseName = intent?.getStringExtra(EXTRA_EXERCISE_NAME) ?: ""
        val duration = intent?.getIntExtra(EXTRA_DURATION, 90) ?: 90
        remainingSeconds = duration

        startForeground(NOTIFICATION_ID, buildNotification(remainingSeconds))
        startTimer()

        return START_NOT_STICKY
    }

    private fun startTimer() {
        stopTimer()
        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                broadcastTick(remainingSeconds)
                updateNotification(remainingSeconds)
            }

            override fun onFinish() {
                remainingSeconds = 0
                broadcastFinish()
                vibrate()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    private fun restartTimer() {
        startTimer()
        updateNotification(remainingSeconds)
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun broadcastTick(remaining: Int) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(ACTION_TICK).putExtra(EXTRA_REMAINING, remaining)
        )
    }

    private fun broadcastFinish() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_FINISH))
    }

    private fun vibrate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            // vibration not critical
        }
    }

    private fun buildNotification(remainingSeconds: Int): Notification {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        val timeStr = "%02d:%02d".format(mins, secs)

        val addThirtyIntent = PendingIntent.getService(
            this, 0,
            Intent(this, RestTimerService::class.java).apply { action = ACTION_ADD_THIRTY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RestTimerService::class.java).apply { action = ACTION_SKIP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Rest Timer — $exerciseName")
            .setContentText(timeStr)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "+30s", addThirtyIntent)
            .addAction(0, "Skip", skipIntent)
            .build()
    }

    private fun updateNotification(remainingSeconds: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(remainingSeconds))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rest Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows rest timer countdown between sets"
            setSound(null, null)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }
}
