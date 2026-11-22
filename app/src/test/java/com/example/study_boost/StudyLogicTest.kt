package com.example.study_boost

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StudyLogicTest {
    private fun utcInstant(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test fun datePickerStartsOnLocalTodayEastOfUtc() {
        val instant = utcInstant(2026, 11, 22, 16, 30)
        assertEquals(utcInstant(2026, 11, 23, 0),
            DateRules.localTodayAsPickerUtc(instant, TimeZone.getTimeZone("Asia/Manila")))
    }

    @Test fun datePickerStartsOnLocalTodayWestOfUtc() {
        val instant = utcInstant(2026, 11, 23, 7, 30)
        assertEquals(utcInstant(2026, 11, 22, 0),
            DateRules.localTodayAsPickerUtc(instant, TimeZone.getTimeZone("America/Los_Angeles")))
    }
    @Test fun blankSubjectFieldsAreRejected() {
        assertEquals(setOf("code", "name"), Validation.subject(" ", "", "#2563EB", 3))
    }
    @Test fun difficultyMustBeBetweenOneAndFive() {
        assertTrue("difficulty" in Validation.subject("CS", "Computing", "#2563EB", 0))
        assertTrue("difficulty" in Validation.subject("CS", "Computing", "#2563EB", 6))
    }
    @Test fun colorRequiresSixHexDigits() {
        assertTrue("color" in Validation.subject("CS", "Computing", "#2563", 3))
        assertTrue(Validation.subject("CS", "Computing", "#abcdef", 3).isEmpty())
    }
    @Test fun invalidCalendarDatesAreRejected() {
        assertFalse(DateRules.valid("2026-02-29"))
        assertFalse(DateRules.valid("2026-13-01"))
        assertFalse(DateRules.valid("2026-1-01"))
        assertTrue(DateRules.valid("2028-02-29"))
    }
    @Test fun taskRequiresTitleDateAndHours() {
        assertEquals(setOf("title", "date", "hours"),
            Validation.task(" ", "", Double.NaN, today = "2026-11-22"))
    }
    @Test fun nonpositiveAndNonfiniteHoursAreRejected() {
        for (hours in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 10001.0)) {
            assertTrue("hours" in Validation.task("Essay", "2026-12-01", hours, today = "2026-11-22"))
        }
    }
    @Test fun newDueDateMustBeAfterToday() {
        assertTrue("date" in Validation.task("Essay", "2026-11-22", 1.0, today = "2026-11-22"))
        assertTrue(Validation.task("Essay", "2026-11-23", 1.0, today = "2026-11-22").isEmpty())
    }
    @Test fun editingCanKeepAnExistingOverdueDate() {
        assertTrue(Validation.task("Essay", "2026-11-01", 1.0, "2026-11-01", "2026-11-22").isEmpty())
        assertTrue("date" in Validation.task("Essay", "2026-11-02", 1.0, "2026-11-01", "2026-11-22"))
    }
    @Test fun nearerDeadlineIncreasesPriority() {
        assertTrue(Priority.score(3, 2.0, 3_600_000, 0) > Priority.score(3, 2.0, 86_400_000, 0))
    }
    @Test fun effortAndDifficultyIncreasePriority() {
        assertEquals(0.5, Priority.score(3, 4.0, 86_400_000, 0), 0.00001)
        assertTrue(Priority.score(5, 4.0, 86_400_000, 0) > Priority.score(1, 4.0, 86_400_000, 0))
    }
    @Test fun overduePriorityHasAFiniteFloor() {
        assertEquals(6.0, Priority.score(3, 2.0, -1, 0), 0.00001)
    }
    @Test fun pausedTimeIsExcluded() {
        val clock = FocusClock(60_000)
        clock.start(1000); clock.pause(6000)
        assertEquals(5000L, clock.elapsed(30_000))
        clock.start(30_000)
        assertEquals(6000L, clock.elapsed(31_000))
    }
    @Test fun repeatedStartDoesNotResetTheBaseline() {
        val clock = FocusClock(60_000)
        clock.start(1000); clock.start(4000)
        assertEquals(5000L, clock.elapsed(6000))
    }
    @Test fun checkpointsDoNotDoubleCountElapsedTime() {
        val clock = FocusClock(60_000)
        clock.start(1000); clock.checkpoint(6000)
        assertEquals(6000L, clock.elapsed(7000))
    }
    @Test fun timerCapsAtThePlannedDuration() {
        val clock = FocusClock(60_000)
        clock.start(0)
        assertEquals(60_000L, clock.elapsed(70_000))
    }
    @Test fun timerCannotAccumulateNegativeTime() {
        val clock = FocusClock(60_000)
        clock.start(5000)
        assertEquals(0L, clock.elapsed(1000))
    }
}
