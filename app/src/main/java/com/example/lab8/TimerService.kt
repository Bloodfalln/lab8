package com.example.lab8

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData

class TimerService : Service() {

    private val binder = TimerBinder()
    private var timer: CountDownTimer? = null
    private var remainingTime: Long = 0
    val timeLeft = MutableLiveData<Long>()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startTimer(duration: Long) {
        remainingTime = duration
        timer?.cancel()
        timer = object : CountDownTimer(duration * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft.postValue(millisUntilFinished / 1000)  // Оновлюємо значення часу кожну секунду
            }

            override fun onFinish() {
                timeLeft.postValue(0)
                showNotification() // Викликаємо сповіщення, коли таймер завершується
            }
        }.start()
    }

    fun stopTimer() {
        timer?.cancel()
        timeLeft.postValue(0)
    }

    fun pauseTimer() {
        timer?.cancel()
        remainingTime = timeLeft.value ?: remainingTime // Якщо timeLeft порожнє, залишаємо попередній час
    }

    fun resumeTimer() {
        if (remainingTime > 0) {
            startTimer(remainingTime)
        } else {
            timeLeft.postValue(0)
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    fun showNotification() {
        val channelId = "timer_notification_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                return
            }
        }

        // Створення каналу для сповіщень
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Таймер завершився",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }


        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Створення сповіщення
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Таймер завершився!")
            .setContentText("Час вашого таймера завершився.")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}

