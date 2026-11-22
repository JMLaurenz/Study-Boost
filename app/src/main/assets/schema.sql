CREATE TABLE IF NOT EXISTS subjects (
    subject_id INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_code TEXT NOT NULL CHECK(length(trim(subject_code)) > 0),
    subject_name TEXT NOT NULL CHECK(length(trim(subject_name)) > 0),
    color_hex TEXT NOT NULL DEFAULT '#2563EB'
        CHECK(length(color_hex) = 7 AND substr(color_hex, 1, 1) = '#'
              AND substr(color_hex, 2) NOT GLOB '*[^0-9A-Fa-f]*'),
    difficulty INTEGER NOT NULL DEFAULT 3 CHECK(typeof(difficulty) = 'integer' AND difficulty BETWEEN 1 AND 5)
);
CREATE TABLE IF NOT EXISTS tasks (
    task_id INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_id INTEGER NOT NULL REFERENCES subjects(subject_id) ON DELETE CASCADE,
    title TEXT NOT NULL CHECK(length(trim(title)) > 0),
    description TEXT NOT NULL DEFAULT '',
    due_date TEXT NOT NULL CHECK(length(due_date) = 10),
    hours REAL NOT NULL DEFAULT 1.0 CHECK(hours > 0 AND hours <= 10000),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'COMPLETED'))
);
CREATE TABLE IF NOT EXISTS study_sessions (
    session_id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    duration_mins INTEGER NOT NULL CHECK(duration_mins > 0),
    duration_seconds INTEGER NOT NULL CHECK(duration_seconds > 0),
    session_date TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    notes TEXT NOT NULL DEFAULT '',
    session_token TEXT NOT NULL UNIQUE CHECK(length(session_token) > 0)
);
CREATE INDEX IF NOT EXISTS idx_tasks_subject ON tasks(subject_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status_due ON tasks(status, due_date);
CREATE INDEX IF NOT EXISTS idx_sessions_task ON study_sessions(task_id);
