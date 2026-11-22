ALTER TABLE subjects RENAME TO subjects_v1;
CREATE TABLE subjects (
    subject_id INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_code TEXT NOT NULL CHECK(length(trim(subject_code)) > 0),
    subject_name TEXT NOT NULL CHECK(length(trim(subject_name)) > 0),
    color_hex TEXT NOT NULL DEFAULT '#2563EB'
        CHECK(length(color_hex) = 7 AND substr(color_hex, 1, 1) = '#'
              AND substr(color_hex, 2) NOT GLOB '*[^0-9A-Fa-f]*'),
    difficulty INTEGER NOT NULL DEFAULT 3 CHECK(typeof(difficulty) = 'integer' AND difficulty BETWEEN 1 AND 5)
);
INSERT INTO subjects(subject_id, subject_code, subject_name, color_hex, difficulty)
SELECT subject_id,
       CASE WHEN length(trim(subject_code)) > 0 THEN trim(subject_code)
            ELSE 'SUB-' || subject_id END,
       CASE WHEN length(trim(subject_name)) > 0 THEN trim(subject_name)
            ELSE 'Imported subject ' || subject_id END,
       CASE WHEN length(color_hex) = 7 AND substr(color_hex, 1, 1) = '#'
                 AND substr(color_hex, 2) NOT GLOB '*[^0-9A-Fa-f]*'
            THEN upper(color_hex) ELSE '#2563EB' END,
       CASE WHEN typeof(difficulty) = 'integer' AND difficulty BETWEEN 1 AND 5 THEN difficulty ELSE 3 END
FROM subjects_v1;
DROP TABLE subjects_v1;
