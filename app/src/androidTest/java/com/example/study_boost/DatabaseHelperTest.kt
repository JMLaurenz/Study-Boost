package com.example.study_boost

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseHelperTest {
    private lateinit var context: Context
    private lateinit var db: DatabaseHelper
    private lateinit var name: String
    private val future: String get() = DateRules.today(Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 7)
    }.timeInMillis)

    @Before fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        name = "studyboost-test-" + UUID.randomUUID().toString() + ".db"
        db = DatabaseHelper(context, name)
    }
    @After fun tearDown() { db.close(); context.deleteDatabase(name) }
    private fun subject() = db.saveSubject(null, "CS101", "Computing", "#2563EB", 3).toInt()
    private fun task(subject: Int, title: String = "Laboratory 100%") =
        db.saveTask(null, subject, title, "Description", future, 2.0, TaskModel.PENDING).toInt()

    @Test fun subjectCanBeCreatedReadUpdatedAndDeleted() {
        val id = subject()
        assertEquals("Computing", db.getSubject(id)?.name)
        db.saveSubject(id, "CS102", "Algorithms", "#00AA66", 5)
        assertEquals(5, db.getSubject(id)?.difficulty)
        assertTrue(db.deleteSubject(id))
        assertNull(db.getSubject(id))
    }
    @Test fun taskCanBeCreatedEditedCompletedAndDeleted() {
        val id = task(subject())
        db.saveTask(id, requireNotNull(db.getTask(id)).subjectId, "Edited", "", future, 3.5, TaskModel.PENDING)
        assertEquals(3.5, requireNotNull(db.getTask(id)).hours, 0.001)
        assertTrue(db.setTaskStatus(id, TaskModel.COMPLETED))
        assertEquals(TaskModel.COMPLETED, db.getTask(id)?.status)
        assertTrue(db.deleteTask(id))
        assertNull(db.getTask(id))
    }
    @Test fun searchCombinesTextSubjectAndStatusAndTreatsWildcardsLiterally() {
        val cs = subject(); val target = task(cs)
        task(cs, "Ordinary task")
        val math = db.saveSubject(null, "MATH", "Calculus", "#AA3366", 2).toInt()
        task(math, "Different course")
        assertEquals(listOf(target), db.getTasks("100%", cs, TaskModel.PENDING).map { it.id })
        assertEquals(2, db.getTasks("COMPUTING", cs, TaskModel.PENDING).size)
        assertTrue(db.getTasks("not present", cs, TaskModel.PENDING).isEmpty())
        db.setTaskStatus(target, TaskModel.COMPLETED)
        assertEquals(listOf(target), db.getTasks("", cs, TaskModel.COMPLETED).map { it.id })
    }
    @Test fun sessionsAreLoggedOnlyOnceAndUseExactSecondsForTotals() {
        val task = task(subject())
        assertTrue(db.logSession(task, 61, "First session", "unique-token"))
        assertFalse(db.logSession(task, 61, "Retry", "unique-token"))
        assertEquals(1, db.getSessions(task).size)
        assertEquals(61L, db.getTotalStudySeconds())
    }
    @Test fun subjectDeletionCascadesToTasksAndSessions() {
        val subject = subject(); val task = task(subject)
        db.logSession(task, 300, "", "cascade")
        db.deleteSubject(subject)
        assertTrue(db.getTasks().isEmpty())
        assertTrue(db.getSessions(task).isEmpty())
        assertEquals(0L, db.getTotalStudySeconds())
    }
    @Test fun recordsPersistWhenDatabaseReopens() {
        val task = task(subject()); db.logSession(task, 300, "Persistent", "reopen")
        db.close(); db = DatabaseHelper(context, name)
        assertNotNull(db.getTask(task))
        assertEquals(300L, db.getTotalStudySeconds())
    }
    @Test fun versionOneMigrationKeepsIdsAndNormalizesInvalidDefaults() {
        db.close()
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { legacy ->
            legacy.execSQL("CREATE TABLE subjects(subject_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject_code TEXT NOT NULL, subject_name TEXT NOT NULL, color_hex TEXT DEFAULT '#2563', difficulty INTEGER)")
            legacy.execSQL("INSERT INTO subjects VALUES(42, ' CS101 ', ' Computing ', '#2563', 99)")
            legacy.version = 1
        }
        db = DatabaseHelper(context, name)
        val migrated = requireNotNull(db.getSubject(42))
        assertEquals("CS101", migrated.code)
        assertEquals("#2563EB", migrated.colorHex)
        assertEquals(3, migrated.difficulty)
        assertTrue(db.getTasks().isEmpty())
        task(42)
    }
}
