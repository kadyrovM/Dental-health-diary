package com.example.lab3

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3.data.DentalDbHelper
import com.example.lab3.data.DentalRecord
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: DentalAdapter
    private lateinit var dbHelper: DentalDbHelper

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "Уведомления отключены", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DentalDbHelper(this)
        adapter = DentalAdapter()

        adapter.onLongClickListener = { record ->
            val options = arrayOf("Редактировать", "Удалить")
            AlertDialog.Builder(this)
                .setTitle("Действие с записью")
                .setItems(options) { _, which ->
                    if (which == 0) showEditDialog(record)
                    else showDeleteDialog(record)
                }.show()
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener {
            val options = arrayOf("Таймер", "Настройки", "О приложении")
            AlertDialog.Builder(this).setTitle("Меню").setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, TimerActivity::class.java))
                    1 -> Toast.makeText(this, "В разработке", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Лабораторная работа №3", Toast.LENGTH_SHORT).show()
                }
            }.show()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadRecords()

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener { showAddDialog() }
        findViewById<FloatingActionButton>(R.id.fabReminder).setOnClickListener { showReminderDialog() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun loadRecords() {
        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) { dbHelper.getAllRecords() }
            adapter.submitList(records)
        }
    }

    private fun showDeleteDialog(record: DentalRecord) {
        AlertDialog.Builder(this)
            .setTitle("Удалить запись?")
            .setPositiveButton("Да") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dbHelper.deleteRecord(record.id) }
                    loadRecords()
                }
            }.setNegativeButton("Нет", null).show()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_add_record, null
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("Новая запись")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val tvDate = dialogView.findViewById<TextView>(R.id.etDate)
                    val cbMorning = dialogView.findViewById<CheckBox>(R.id.cbMorning)
                    val cbEvening = dialogView.findViewById<CheckBox>(R.id.cbEvening)
                    val cbFloss = dialogView.findViewById<CheckBox>(R.id.cbFloss)
                    val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

                    val date = tvDate.text.toString()

                    if (date.isBlank()) {
                        Toast.makeText(
                            this, "Выберите дату", Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    val record = DentalRecord(
                        id = 0,
                        date = date,
                        brushedMorning = cbMorning.isChecked,
                        brushedEvening = cbEvening.isChecked,
                        flossed = cbFloss.isChecked,
                        notes = etNotes.text.toString().ifBlank { null }
                    )

                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                dbHelper.insertRecord(record)
                            }
                            loadRecords()
                            dialog.dismiss()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Ошибка сохранения: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
        }

        setupDateAndChecks(dialogView) { }
        dialog.show()
    }

    private fun showEditDialog(oldRecord: DentalRecord) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_record, null)
        val tvDate = dialogView.findViewById<TextView>(R.id.etDate)
        val cbMorning = dialogView.findViewById<CheckBox>(R.id.cbMorning)
        val cbEvening = dialogView.findViewById<CheckBox>(R.id.cbEvening)
        val cbFloss = dialogView.findViewById<CheckBox>(R.id.cbFloss)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        tvDate.text = oldRecord.date
        cbMorning.isChecked = oldRecord.brushedMorning
        cbEvening.isChecked = oldRecord.brushedEvening
        cbFloss.isChecked = oldRecord.flossed
        etNotes.setText(oldRecord.notes)

        setupDateAndChecks(dialogView, oldRecord) { updatedRecord ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    dbHelper.deleteRecord(oldRecord.id) // Удаляем старую
                    dbHelper.insertRecord(updatedRecord) // Вставляем обновленную
                }
                loadRecords()
            }
        }

        AlertDialog.Builder(this).setTitle("Редактировать").setView(dialogView)
            .setPositiveButton("Обновить") { _, _ -> /* Логика внутри setupDateAndChecks */ }
            .setNegativeButton("Отмена", null).show()
    }

    private fun setupDateAndChecks(
        dialogView: android.view.View,
        oldRecord: DentalRecord? = null,
        onSave: (DentalRecord) -> Unit
    ) {
        val tvDate = dialogView.findViewById<TextView>(R.id.etDate)
        val cbMorning = dialogView.findViewById<CheckBox>(R.id.cbMorning)
        val cbEvening = dialogView.findViewById<CheckBox>(R.id.cbEvening)
        val cbFloss = dialogView.findViewById<CheckBox>(R.id.cbFloss)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        val calendar = Calendar.getInstance()
        tvDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                tvDate.text = String.format(Locale.getDefault(), "%02d.%02d.%d", dayOfMonth, month + 1, year)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogView.post {
            val dialog = (dialogView.parent as? android.widget.FrameLayout)?.parent as? AlertDialog
            dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val date = tvDate.text.toString()
                if (date.isBlank()) {
                    Toast.makeText(this, "Выберите дату", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val record = DentalRecord(
                    id = oldRecord?.id ?: 0,
                    date = date,
                    brushedMorning = cbMorning.isChecked,
                    brushedEvening = cbEvening.isChecked,
                    flossed = cbFloss.isChecked,
                    notes = etNotes.text.toString().ifBlank { null }
                )
                onSave(record)
                dialog.dismiss()
            }
        }
    }

    private fun showReminderDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute, 0)
                setAlarm(calendar.timeInMillis)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setAlarm(timeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Нет разрешения на точные будильники", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            Toast.makeText(this, "Напоминание установлено!", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Ошибка установки будильника", Toast.LENGTH_SHORT).show()
        }
    }
}