package com.example.lab8

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer

class MainActivity : AppCompatActivity() {

    private lateinit var editTextTime: EditText
    private lateinit var textViewTimer: TextView
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnResume: Button // Кнопка для відновлення таймера

    private var timerService: TimerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true
            observeTimer()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Перевірка дозволу на сповіщення для Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        editTextTime = findViewById(R.id.editTextTime)
        textViewTimer = findViewById(R.id.textViewTimer)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        btnResume = findViewById(R.id.btnResume) // Ініціалізація кнопки Resume

        btnStart.setOnClickListener {
            val time = editTextTime.text.toString().toLongOrNull()
            if (time != null && time > 0) {
                timerService?.startTimer(time)
                btnResume.visibility = Button.GONE // Сховати кнопку Resume
            }
        }

        btnPause.setOnClickListener {
            timerService?.pauseTimer()
            btnResume.visibility = Button.VISIBLE
        }

        btnStop.setOnClickListener {
            timerService?.stopTimer()
            btnResume.visibility = Button.GONE
        }

        btnResume.setOnClickListener {
            timerService?.resumeTimer()
            btnResume.visibility = Button.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun observeTimer() {
        timerService?.timeLeft?.observe(this, Observer { timeLeft ->
            textViewTimer.text = formatTime(timeLeft)
            if (timeLeft == 0L) {
                showCompletionNotification()
            }
        })
    }

    private fun showCompletionNotification() {
        timerService?.showNotification()
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}
