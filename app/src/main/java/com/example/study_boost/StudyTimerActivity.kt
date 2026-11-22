package com.example.study_boost

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.Locale
import java.util.UUID

class StudyTimerActivity : ScreenActivity() {
    companion object { const val PREFS = "active_focus_session" }
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: FocusClock
    private lateinit var minutes: AutoCompleteTextView
    private var taskId = -1
    private var token = ""
    private var selectedMinutes = 25
    private var begun = false
    private var ready = false
    private var saving = false
    private var closed = false
    private var finalizing = false
    private var rebooted = false
    private val bootCount: Int get() = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
    private val focusLengths by lazy {
        listOf(
            15 to getString(R.string.time_15_min),
            25 to getString(R.string.focus_25_min),
            30 to getString(R.string.time_30_min),
            45 to getString(R.string.time_45_min),
            60 to getString(R.string.time_1_hour),
            90 to getString(R.string.time_1_30),
            120 to getString(R.string.time_2_hours)
        )
    }
    private val tick = object : Runnable {
        override fun run() {
            if (ready && !closed) {
                render()
                if (clock.elapsed(SystemClock.elapsedRealtime()) >= clock.plannedMillis && !saving && !finalizing) saveSession()
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen(R.layout.activity_study_timer)
        minutes = findViewById(R.id.etMinutes)
        configureLengthMenu()
        val requestedId = intent.getIntExtra(Extras.TASK_ID, -1)
        if (prefs.contains("token")) {
            taskId = prefs.getInt("task", -1)
            token = prefs.getString("token", "").orEmpty()
            begun = prefs.getBoolean("begun", false)
            finalizing = prefs.getBoolean("finalizing", false)
            val now = SystemClock.elapsedRealtime()
            val started = prefs.getLong("started", -1).takeIf { it >= 0 }
            val savedBoot = prefs.getInt("boot", -2)
            rebooted = started != null && (savedBoot != bootCount || started > now || savedBoot < 0)
            clock = FocusClock(prefs.getLong("planned", 25 * 60_000L),
                prefs.getLong("elapsed", 0), if (rebooted) null else started)
            if (requestedId > 0 && requestedId != taskId) toast(R.string.resuming_existing)
            if (rebooted) toast(R.string.timer_rebooted)
        } else {
            taskId = requestedId
            token = UUID.randomUUID().toString()
            clock = FocusClock(25 * 60_000L)
        }
        selectedMinutes = (clock.plannedMillis / 60_000L).toInt().coerceIn(1, 180)
        minutes.setText(lengthLabel(selectedMinutes), false)
        if (taskId <= 0) { toast(R.string.task_missing); finish(); return }

        database({ it.getTask(taskId) }) { task ->
            if (task == null) {
                closed = true
                prefs.edit().clear().commit()
                toast(R.string.task_missing)
                finish()
            } else {
                findViewById<TextView>(R.id.tvTimerSubjectCode).text = task.subjectCode
                findViewById<TextView>(R.id.tvTimerTask).text = task.title
                findViewById<TextView>(R.id.tvTimerSubject).text = task.subjectName
                ready = true
                persist()
                render()
                if (finalizing) saveSession()
            }
        }
        findViewById<View>(R.id.btnStart).setOnClickListener {
            if (!ready || saving || finalizing) return@setOnClickListener
            if (!begun) {
                clock = FocusClock(selectedMinutes * 60_000L)
                begun = true
            }
            clock.start(SystemClock.elapsedRealtime())
            persist()
            render()
        }
        findViewById<View>(R.id.btnPause).setOnClickListener {
            if (ready && !saving) {
                clock.pause(SystemClock.elapsedRealtime())
                persist()
                render()
            }
        }
        findViewById<View>(R.id.btnFinish).setOnClickListener { if (ready && !saving) saveSession() }
    }

    private fun configureLengthMenu() {
        minutes.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            focusLengths.map { it.second }))
        minutes.setOnClickListener { if (!begun) minutes.showDropDown() }
        minutes.setOnItemClickListener { _, _, position, _ -> selectedMinutes = focusLengths[position].first }
    }

    private fun lengthLabel(value: Int): String = focusLengths.firstOrNull { it.first == value }?.second ?: "$value min"

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getIntExtra(Extras.TASK_ID, taskId) != taskId) toast(R.string.resuming_existing)
    }

    override fun onResume() { super.onResume(); handler.post(tick) }

    override fun onPause() {
        handler.removeCallbacks(tick)
        if (ready && !closed) persist()
        super.onPause()
    }

    private fun persist() {
        clock.checkpoint(SystemClock.elapsedRealtime())
        prefs.edit().putString("token", token).putInt("task", taskId)
            .putLong("planned", clock.plannedMillis).putLong("elapsed", clock.accumulatedMillis)
            .putLong("started", clock.startedAt ?: -1).putInt("boot", bootCount)
            .putBoolean("begun", begun).putBoolean("finalizing", finalizing).commit()
    }

    private fun render() {
        val elapsed = clock.elapsed(SystemClock.elapsedRealtime())
        val secondsLeft = (clock.plannedMillis - elapsed + 999) / 1000
        findViewById<TextView>(R.id.tvCountdown).apply {
            text = String.format(Locale.getDefault(), "%02d:%02d", secondsLeft / 60, secondsLeft % 60)
            contentDescription = getString(R.string.countdown_remaining, secondsLeft / 60, secondsLeft % 60)
        }
        findViewById<CircularProgressIndicator>(R.id.timerProgress).progress =
            (elapsed * 1000 / clock.plannedMillis).toInt()
        minutes.isEnabled = !begun && !saving
        val start = findViewById<Button>(R.id.btnStart)
        start.setText(if (begun) R.string.resume_focus else R.string.start_focus)
        start.isEnabled = ready && !saving && !finalizing && clock.startedAt == null && elapsed < clock.plannedMillis
        findViewById<Button>(R.id.btnPause).isEnabled = ready && !saving && clock.startedAt != null
        findViewById<Button>(R.id.btnFinish).isEnabled = ready && !saving && begun && clock.startedAt == null && elapsed > 0
        findViewById<TextView>(R.id.tvElapsedDetail).apply {
            visibility = if (begun && elapsed > 0) TextView.VISIBLE else TextView.GONE
            text = getString(R.string.focused_duration, elapsed / 60_000, (elapsed / 1000) % 60)
        }
        findViewById<TextView>(R.id.tvTimerState).setText(when {
            saving -> R.string.saving_session
            finalizing -> R.string.retry_session
            clock.startedAt != null -> R.string.timer_running
            begun -> R.string.timer_paused
            else -> R.string.timer_ready
        })
    }

    private fun saveSession() {
        if (!ready || saving || closed) return
        clock.pause(SystemClock.elapsedRealtime())
        val seconds = (clock.accumulatedMillis / 1000).toInt()
        if (seconds < 1) {
            toast(R.string.session_too_short)
            persist()
            render()
            return
        }
        finalizing = true
        saving = true
        persist()
        render()
        database({ it.logSession(taskId, seconds, "", token) }, {
            saving = false
            render()
        }) {
            closed = true
            prefs.edit().clear().commit()
            toast(R.string.session_saved)
            setResult(RESULT_OK, Intent().putExtra(Extras.TASK_ID, taskId))
            finish()
        }
    }
}
