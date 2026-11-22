package com.example.study_boost

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/** Android-independent domain rules for JVM tests. */
object DateRules {
    private fun formatter() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    fun today(now: Long = System.currentTimeMillis()): String = formatter().format(Date(now))
    fun valid(date: String): Boolean = Regex("\\d{4}-\\d{2}-\\d{2}").matches(date) &&
        runCatching { formatter().parse(date)?.let { formatter().format(it) == date } == true }.getOrDefault(false)
    fun localTodayAsPickerUtc(now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = now }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    fun endOfDay(date: String): Long {
        require(valid(date))
        return Calendar.getInstance().apply {
            time = requireNotNull(formatter().parse(date)); add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }
}

object Validation {
    fun subject(code: String, name: String, color: String, difficulty: Int): Set<String> = buildSet {
        if (code.isBlank()) add("code")
        if (name.isBlank()) add("name")
        if (!Regex("#[0-9a-fA-F]{6}").matches(color)) add("color")
        if (difficulty !in 1..5) add("difficulty")
    }
    fun task(title: String, date: String, hours: Double, originalDate: String? = null,
             today: String = DateRules.today()): Set<String> = buildSet {
        if (title.isBlank()) add("title")
        if (!DateRules.valid(date) || (date <= today && date != originalDate)) add("date")
        if (!hours.isFinite() || hours <= 0.0 || hours > 10000.0) add("hours")
    }
}

object Priority {
    /** The PDF has no formula. Higher effort/difficulty and nearer deadlines raise the score. */
    fun score(difficulty: Int, hours: Double, deadline: Long, now: Long): Double {
        require(difficulty in 1..5 && hours.isFinite() && hours > 0)
        return difficulty * hours / max(1.0, (deadline - now) / 3_600_000.0)
    }
}

data class FocusClock(
    val plannedMillis: Long,
    var accumulatedMillis: Long = 0,
    var startedAt: Long? = null
) {
    fun elapsed(now: Long): Long = (accumulatedMillis +
        (startedAt?.let { (now - it).coerceAtLeast(0) } ?: 0)).coerceIn(0, plannedMillis)
    fun start(now: Long) { if (startedAt == null && accumulatedMillis < plannedMillis) startedAt = now }
    fun pause(now: Long) { accumulatedMillis = elapsed(now); startedAt = null }
    fun checkpoint(now: Long) {
        val running = startedAt != null
        accumulatedMillis = elapsed(now)
        startedAt = if (running && accumulatedMillis < plannedMillis) now else null
    }
}
