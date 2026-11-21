package com.example.study_boost

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "StudyBoost.db"
        private const val DATABASE_VERSION = 1

        // Table & Columns
        const val TABLE_SUBJECTS = "subjects"
        const val COLUMN_SUBJECT_ID = "subject_id"
        const val COLUMN_SUBJECT_CODE = "subject_code"
        const val COLUMN_SUBJECT_NAME = "subject_name"
        const val COLUMN_COLOR_HEX = "color_hex"
        const val COLUMN_DIFFICULTY = "difficulty"

        // SQL Statement to create the subjects table[cite: 3]
        private const val CREATE_TABLE_SUBJECTS = """
            CREATE TABLE $TABLE_SUBJECTS (
                $COLUMN_SUBJECT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SUBJECT_CODE TEXT NOT NULL,
                $COLUMN_SUBJECT_NAME TEXT NOT NULL,
                $COLUMN_COLOR_HEX TEXT DEFAULT '#2563',
                $COLUMN_DIFFICULTY INTEGER
            );
        """
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_SUBJECTS)
        // You can add other table creations here later (like tasks and study_sessions)[cite: 2]
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SUBJECTS")
        onCreate(db)
    }

    fun insertSubject(code: String, name: String, colorHex: String, difficulty: Int): Boolean {
        // TC01: Validate that subject code and name are not blank
        if (code.isBlank() || name.isBlank()) {
            return false
        }

        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SUBJECT_CODE, code.trim())
            put(COLUMN_SUBJECT_NAME, name.trim())
            put(COLUMN_COLOR_HEX, colorHex)
            put(COLUMN_DIFFICULTY, difficulty)
        }

        val result = db.insert(TABLE_SUBJECTS, null, values)
        return result != -1L
    }
}