package com.example.study_boost

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.concurrent.Executors

/** UI access is serialized on StudyStore.executor, away from the main thread. */
class DatabaseHelper(context: Context, name: String = DATABASE_NAME) :
    SQLiteOpenHelper(context.applicationContext, name, null, DATABASE_VERSION) {
    private val assets = context.applicationContext.assets

    companion object {
        const val DATABASE_NAME = "StudyBoost.db"
        const val DATABASE_VERSION = 2
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) = executeAsset(db, "schema.sql")

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            executeAsset(db, "migration_1_2.sql")
            executeAsset(db, "schema.sql")
        }
    }

    private fun executeAsset(db: SQLiteDatabase, filename: String) {
        val sql = assets.open(filename).bufferedReader().use { it.readText() }
        sql.split(';').map(String::trim).filter(String::isNotEmpty).forEach { db.execSQL(it) }
    }

    fun getSubjects(): List<Subject> = readableDatabase.rawQuery(
        "SELECT * FROM subjects ORDER BY subject_code COLLATE NOCASE, subject_id", null
    ).use { c -> buildList { while (c.moveToNext()) add(c.subject()) } }

    fun getSubject(id: Int): Subject? = readableDatabase.rawQuery(
        "SELECT * FROM subjects WHERE subject_id = ?", arrayOf(id.toString())
    ).use { if (it.moveToFirst()) it.subject() else null }

    fun saveSubject(id: Int?, code: String, name: String, color: String, difficulty: Int): Long {
        require(Validation.subject(code, name, color, difficulty).isEmpty())
        val values = ContentValues().apply {
            put("subject_code", code.trim()); put("subject_name", name.trim())
            put("color_hex", color.uppercase()); put("difficulty", difficulty)
        }
        return if (id == null) writableDatabase.insertOrThrow("subjects", null, values) else {
            check(writableDatabase.update("subjects", values, "subject_id = ?", arrayOf(id.toString())) == 1)
            id.toLong()
        }
    }

    fun deleteSubject(id: Int): Boolean =
        writableDatabase.delete("subjects", "subject_id = ?", arrayOf(id.toString())) == 1

    fun getTasks(query: String = "", subjectId: Int? = null, status: String? = null): List<TaskModel> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (query.isNotBlank()) {
            clauses += "(t.title LIKE ? ESCAPE '\\' OR s.subject_name LIKE ? ESCAPE '\\' OR s.subject_code LIKE ? ESCAPE '\\')"
            val pattern = "%" + query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
            repeat(3) { args += pattern }
        }
        if (subjectId != null) { clauses += "t.subject_id = ?"; args += subjectId.toString() }
        if (status != null) {
            require(status == TaskModel.PENDING || status == TaskModel.COMPLETED)
            clauses += "t.status = ?"; args += status
        }
        val where = if (clauses.isEmpty()) "" else " WHERE " + clauses.joinToString(" AND ")
        return readableDatabase.rawQuery(TASK_QUERY + where + " ORDER BY t.due_date, t.task_id",
            args.toTypedArray()).use { c -> buildList { while (c.moveToNext()) add(c.task()) } }
    }

    fun getTask(id: Int): TaskModel? = readableDatabase.rawQuery(
        TASK_QUERY + " WHERE t.task_id = ?", arrayOf(id.toString())
    ).use { if (it.moveToFirst()) it.task() else null }

    fun saveTask(id: Int?, subjectId: Int, title: String, description: String, dueDate: String,
                 hours: Double, status: String): Long {
        val original = id?.let(::getTask)
        if (id != null) check(original != null)
        require(Validation.task(title, dueDate, hours, original?.dueDate).isEmpty())
        require(status == TaskModel.PENDING || status == TaskModel.COMPLETED)
        check(getSubject(subjectId) != null)
        val values = ContentValues().apply {
            put("subject_id", subjectId); put("title", title.trim())
            put("description", description.trim()); put("due_date", dueDate)
            put("hours", hours); put("status", status)
        }
        return if (id == null) writableDatabase.insertOrThrow("tasks", null, values) else {
            check(writableDatabase.update("tasks", values, "task_id = ?", arrayOf(id.toString())) == 1)
            id.toLong()
        }
    }

    fun setTaskStatus(id: Int, status: String): Boolean {
        require(status == TaskModel.PENDING || status == TaskModel.COMPLETED)
        return writableDatabase.update("tasks", ContentValues().apply { put("status", status) },
            "task_id = ?", arrayOf(id.toString())) == 1
    }

    fun deleteTask(id: Int): Boolean =
        writableDatabase.delete("tasks", "task_id = ?", arrayOf(id.toString())) == 1

    /** Durable tokens make a retry after process death safe. */
    fun logSession(taskId: Int, durationSeconds: Int, notes: String, token: String): Boolean {
        require(durationSeconds > 0 && token.isNotBlank())
        val db = writableDatabase
        db.beginTransaction()
        try {
            val exists = db.rawQuery("SELECT 1 FROM study_sessions WHERE session_token = ?",
                arrayOf(token)).use { it.moveToFirst() }
            if (!exists) {
                val values = ContentValues().apply {
                    put("task_id", taskId); put("duration_seconds", durationSeconds)
                    put("duration_mins", (durationSeconds + 59) / 60)
                    put("notes", notes.trim()); put("session_token", token)
                }
                db.insertOrThrow("study_sessions", null, values)
            }
            db.setTransactionSuccessful()
            return !exists
        } finally { db.endTransaction() }
    }

    fun getSessions(taskId: Int): List<StudySession> = readableDatabase.rawQuery(
        "SELECT session_id, task_id, duration_seconds, session_date, notes FROM study_sessions " +
            "WHERE task_id = ? ORDER BY session_id DESC", arrayOf(taskId.toString())
    ).use { c -> buildList {
        while (c.moveToNext()) add(StudySession(c.int("session_id"), c.int("task_id"),
            c.int("duration_seconds"), c.text("session_date"), c.text("notes")))
    } }

    fun getTotalStudySeconds(): Long = readableDatabase.rawQuery(
        "SELECT COALESCE(SUM(duration_seconds), 0) FROM study_sessions", null
    ).use { it.moveToFirst(); it.getLong(0) }

    private fun Cursor.subject() = Subject(int("subject_id"), text("subject_code"),
        text("subject_name"), text("color_hex"), int("difficulty"))

    private fun Cursor.task() = TaskModel(int("task_id"), int("subject_id"), text("title"),
        text("description"), text("due_date"), getDouble(getColumnIndexOrThrow("hours")),
        text("status"), text("subject_code"), text("subject_name"), text("color_hex"),
        int("difficulty"), getLong(getColumnIndexOrThrow("studied_seconds")))

    private fun Cursor.int(column: String) = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.text(column: String) = getString(getColumnIndexOrThrow(column)).orEmpty()

    private val TASK_QUERY = """
        SELECT t.*, s.subject_code, s.subject_name, s.color_hex, s.difficulty,
            COALESCE((SELECT SUM(ss.duration_seconds) FROM study_sessions ss
                      WHERE ss.task_id = t.task_id), 0) AS studied_seconds
        FROM tasks t JOIN subjects s ON s.subject_id = t.subject_id
    """.trimIndent()
}

object StudyStore {
    val executor = Executors.newSingleThreadExecutor()
    @Volatile private var instance: DatabaseHelper? = null
    fun database(context: Context): DatabaseHelper = instance ?: synchronized(this) {
        instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
    }
}
