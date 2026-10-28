package com.example.lab3

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.util.Locale

class TimerActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null
    private var totalTimeInMillis: Long = 0L
    private var isTimerRunning = false

    private lateinit var tvDisplay: TextView
    private lateinit var btnSetTime: Button
    private lateinit var btnStart: Button
    private lateinit var btnReset: Button

    private val historyList = mutableListOf<Int>()
    private lateinit var historyAdapter: TimerHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarTimer)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Меню")
                .setItems(arrayOf("На главную", "О приложении")) { _, which ->
                    if (which == 0) finish() else Toast.makeText(this, "Lab 3", Toast.LENGTH_SHORT).show()
                }.show()
        }

        tvDisplay = findViewById(R.id.tvTimerDisplay)
        btnSetTime = findViewById(R.id.btnSetTime)
        btnStart = findViewById(R.id.btnStart)
        btnReset = findViewById(R.id.btnReset)

        historyAdapter = TimerHistoryAdapter(historyList,
            onRepeatClick = { totalSeconds ->
                totalTimeInMillis = totalSeconds * 1000L
                updateTimerDisplay()
                btnReset.isEnabled = true
                Toast.makeText(this, "Время установлено", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { totalSeconds ->
                historyList.remove(totalSeconds)
                historyAdapter.notifyDataSetChanged()
            }
        )

        val rv = findViewById<RecyclerView>(R.id.rvTimerHistory)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = historyAdapter

        btnSetTime.setOnClickListener {
            if (isTimerRunning) return@setOnClickListener
            val timePicker = TimePicker(this).apply { setIs24HourView(true) }
            AlertDialog.Builder(this).setTitle("Время (Мин : Сек)").setView(timePicker)
                .setPositiveButton("ОК") { _, _ ->
                    val min = timePicker.hour
                    val sec = timePicker.minute
                    totalTimeInMillis = (min * 60L + sec) * 1000L
                    updateTimerDisplay()
                    btnReset.isEnabled = true
                }.show()
        }

        btnStart.setOnClickListener {
            if (totalTimeInMillis <= 0 || isTimerRunning) return@setOnClickListener
            isTimerRunning = true
            btnStart.isEnabled = false
            btnSetTime.isEnabled = false
            btnReset.isEnabled = true

            val totalSeconds = (totalTimeInMillis / 1000).toInt()
            if (totalSeconds > 0 && !historyList.contains(totalSeconds)) {
                historyList.add(totalSeconds)
                historyAdapter.notifyDataSetChanged()
            }

            val intent = Intent(this, TimerService::class.java).apply {
                putExtra("TIMER_MINUTES", totalSeconds / 60)
                putExtra("TIMER_SECONDS", totalSeconds % 60)
            }
            startForegroundService(intent)

            countDownTimer = object : CountDownTimer(totalTimeInMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    totalTimeInMillis = millisUntilFinished
                    updateTimerDisplay()
                }
                override fun onFinish() {
                    isTimerRunning = false
                    totalTimeInMillis = 0L
                    tvDisplay.text = "00:00"
                    btnStart.isEnabled = true
                    btnSetTime.isEnabled = true
                    btnReset.isEnabled = false
                    stopService(Intent(this@TimerActivity, TimerService::class.java))
                    Toast.makeText(this@TimerActivity, "Таймер завершен!", Toast.LENGTH_LONG).show()
                }
            }.start()
        }

        btnReset.setOnClickListener {
            if (isTimerRunning) {
                countDownTimer?.cancel()
                isTimerRunning = false
                stopService(Intent(this, TimerService::class.java))
            }
            totalTimeInMillis = 0L
            tvDisplay.text = "00:00"
            btnStart.isEnabled = true
            btnSetTime.isEnabled = true
            btnReset.isEnabled = false
        }
    }

    private fun updateTimerDisplay() {
        val minutes = (totalTimeInMillis / 1000) / 60
        val seconds = (totalTimeInMillis / 1000) % 60
        tvDisplay.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}