#!/usr/bin/env python3
"""Portable SQL-asset and resource checks. Does not compile Kotlin or emulate Android."""
from pathlib import Path
import re
import json
import sqlite3
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
SCHEMA = (MAIN / "assets/schema.sql").read_text()
MIGRATION = (MAIN / "assets/migration_1_2.sql").read_text()
ANDROID = "{http://schemas.android.com/apk/res/android}"
DB_SOURCE = (MAIN / "java/com/example/study_boost/DatabaseHelper.kt").read_text()
TASK_QUERY = re.search(r'private val TASK_QUERY = """(.*?)"""', DB_SOURCE, re.S).group(1)

def statements(connection, sql):
    # Same statement splitting used by DatabaseHelper.executeAsset.
    for statement in sql.split(";"):
        if statement.strip():
            connection.execute(statement)

def database(path=":memory:"):
    db = sqlite3.connect(path)
    db.execute("PRAGMA foreign_keys = ON")
    statements(db, SCHEMA)
    return db

def seed(db):
    db.execute("INSERT INTO subjects VALUES(1, 'CS101', 'Computing', '#2563EB', 3)")
    db.execute("INSERT INTO tasks VALUES(1, 1, 'Laboratory 100%', '', '2099-12-31', 2.0, 'PENDING')")
    db.execute("INSERT INTO study_sessions(task_id, duration_mins, duration_seconds, notes, session_token) "
               "VALUES(1, 2, 61, 'Focus notes', 'token')")

class SqlAssetTests(unittest.TestCase):
    def setUp(self):
        self.db = database()
    def tearDown(self):
        self.db.close()
    def test_01_all_three_tables_exist(self):
        names = {x[0] for x in self.db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        self.assertTrue({"subjects", "tasks", "study_sessions"} <= names)
    def test_02_subject_constraints(self):
        for code, name, color, difficulty in [
            ("", "Name", "#2563EB", 3), ("CS", " ", "#2563EB", 3),
            ("CS", "Name", "#2563", 3), ("CS", "Name", "#GGGGGG", 3),
            ("CS", "Name", "#2563EB", 0), ("CS", "Name", "#2563EB", 6),
            ("CS", "Name", "#2563EB", 2.5),
        ]:
            with self.subTest(code=code, name=name, color=color, difficulty=difficulty):
                with self.assertRaises(sqlite3.IntegrityError):
                    self.db.execute("INSERT INTO subjects(subject_code, subject_name, color_hex, difficulty) "
                                    "VALUES(?,?,?,?)", (code, name, color, difficulty))
    def test_03_task_constraints_and_foreign_key(self):
        self.db.execute("INSERT INTO subjects VALUES(1, 'CS', 'Computing', '#2563EB', 3)")
        for parent, title, hours, status in [(99, "Task", 1, "PENDING"), (1, "", 1, "PENDING"),
                                             (1, "Task", 0, "PENDING"), (1, "Task", -2, "PENDING"),
                                             (1, "Task", 2, "INVALID")]:
            with self.subTest(parent=parent, title=title, hours=hours, status=status):
                with self.assertRaises(sqlite3.IntegrityError):
                    self.db.execute("INSERT INTO tasks(subject_id,title,due_date,hours,status) VALUES(?,?,?,?,?)",
                                    (parent, title, "2099-12-31", hours, status))
    def test_04_subject_deletion_cascades_two_levels(self):
        seed(self.db)
        self.db.execute("DELETE FROM subjects WHERE subject_id=1")
        self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM tasks").fetchone()[0])
        self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM study_sessions").fetchone()[0])
    def test_05_task_deletion_keeps_subject_and_removes_sessions(self):
        seed(self.db)
        self.db.execute("DELETE FROM tasks WHERE task_id=1")
        self.assertEqual(1, self.db.execute("SELECT COUNT(*) FROM subjects").fetchone()[0])
        self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM study_sessions").fetchone()[0])
    def test_06_session_constraints_and_unique_token(self):
        seed(self.db)
        for task, minutes, seconds, token in [(99, 1, 10, "orphan"), (1, 0, 10, "zero-min"),
                                             (1, 1, 0, "zero-sec"), (1, 1, 10, "token"),
                                             (1, 1, 10, "")]:
            with self.subTest(task=task, seconds=seconds, token=token):
                with self.assertRaises(sqlite3.IntegrityError):
                    self.db.execute("INSERT INTO study_sessions(task_id,duration_mins,duration_seconds,session_token)"
                                    " VALUES(?,?,?,?)", (task, minutes, seconds, token))
    def test_07_join_query_includes_exact_study_seconds(self):
        seed(self.db)
        self.db.row_factory = sqlite3.Row
        row = self.db.execute(TASK_QUERY + " WHERE t.task_id = ?", (1,)).fetchone()
        self.assertEqual("Computing", row["subject_name"])
        self.assertEqual(61, row["studied_seconds"])
    def test_08_search_sql_can_match_literal_percent_and_combine_filters(self):
        seed(self.db)
        predicate = re.search(r'clauses \+= "(\(t\.title LIKE.*?\))"', DB_SOURCE).group(1)
        predicate = json.loads('"' + predicate + '"')
        # Kotlin's source contains a single literal backslash inside this SQL string.
        query = TASK_QUERY + " WHERE " + predicate + " AND t.subject_id=? AND t.status=?"
        rows = self.db.execute(query, ("%100\\%%", "%100\\%%", "%100\\%%", 1, "PENDING")).fetchall()
        self.assertEqual(1, len(rows))
        self.assertEqual([], self.db.execute(query, ("%missing%",)*3 + (1, "PENDING")).fetchall())
    def test_09_committed_records_persist_on_reopen(self):
        with tempfile.TemporaryDirectory() as folder:
            file = str(Path(folder) / "study.db")
            with database(file) as db:
                seed(db)
            db.close()
            with sqlite3.connect(file) as db:
                total = db.execute("SELECT SUM(duration_seconds) FROM study_sessions").fetchone()[0]
            db.close()
            self.assertEqual(61, total)
    def test_10_migration_preserves_valid_subjects_and_normalizes_bad_defaults(self):
        db = sqlite3.connect(":memory:")
        self.addCleanup(db.close)
        db.execute("PRAGMA foreign_keys=ON")
        db.execute("CREATE TABLE subjects(subject_id INTEGER PRIMARY KEY AUTOINCREMENT, subject_code TEXT NOT NULL,"
                   " subject_name TEXT NOT NULL, color_hex TEXT DEFAULT '#2563', difficulty INTEGER)")
        db.execute("INSERT INTO subjects VALUES(7,' CS101 ',' Computing ','#2563',99)")
        db.execute("INSERT INTO subjects VALUES(9,'MATH','Calculus','#00aa66',5)")
        db.execute("INSERT INTO subjects VALUES(12,' ',' ',NULL,NULL)")
        statements(db, MIGRATION)
        statements(db, SCHEMA)
        self.assertEqual((7, "CS101", "Computing", "#2563EB", 3),
                         db.execute("SELECT * FROM subjects WHERE subject_id=7").fetchone())
        self.assertEqual((9, "MATH", "Calculus", "#00AA66", 5),
                         db.execute("SELECT * FROM subjects WHERE subject_id=9").fetchone())
        self.assertEqual((12, "SUB-12", "Imported subject 12", "#2563EB", 3),
                         db.execute("SELECT * FROM subjects WHERE subject_id=12").fetchone())
        db.execute("INSERT INTO subjects(subject_code,subject_name) VALUES('NEW','New')")
        self.assertGreater(db.execute("SELECT MAX(subject_id) FROM subjects").fetchone()[0], 12)
        self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())

class ResourceChecks(unittest.TestCase):
    def test_11_all_xml_files_parse(self):
        for file in MAIN.rglob("*.xml"):
            with self.subTest(file=file.relative_to(MAIN)):
                ET.parse(file)
    def test_12_local_resource_references_resolve(self):
        known = {kind: set() for kind in ("string", "color", "style", "id", "layout", "drawable", "xml", "mipmap", "font")}
        for file in (MAIN/"res").rglob("*"):
            if not file.is_file(): continue
            kind = file.parent.name.split("-")[0]
            if kind in known: known[kind].add(file.stem)
            if file.suffix == ".xml":
                for e in ET.parse(file).iter():
                    if e.tag in ("string", "color", "style") and e.get("name"):
                        known[e.tag].add(e.get("name"))
                    for value in e.attrib.values():
                        m = re.fullmatch(r"@\+id/(\w+)", value)
                        if m: known["id"].add(m.group(1))
        for file in list((MAIN/"res").rglob("*.xml")) + list((MAIN/"java").rglob("*.kt")) + [MAIN/"AndroidManifest.xml"]:
            source = file.read_text()
            refs = re.findall(r"@(?:\+)?(string|color|id|layout|drawable|xml|mipmap|font)/([\w.]+)", source)
            # Qualified library R references belong to their dependency, not this app's resources.
            refs += re.findall(r"(?<![\w.])R\.(string|color|id|layout|drawable|xml|mipmap|font)\.(\w+)", source)
            for kind, name in refs:
                with self.subTest(file=file.name, resource=kind+"/"+name):
                    self.assertIn(name, known[kind])
    def test_13_declared_activities_have_source_and_one_launcher(self):
        tree = ET.parse(MAIN/"AndroidManifest.xml")
        activities = tree.findall(".//activity")
        for e in activities:
            name = e.get(ANDROID+"name").split(".")[-1]
            self.assertTrue((MAIN/("java/com/example/study_boost/"+name+".kt")).is_file(), name)
        launchers = tree.findall(".//category[@"+ANDROID+"name='android.intent.category.LAUNCHER']")
        self.assertEqual(1, len(launchers))
        for e in activities:
            if e.get(ANDROID+"name") != ".MainActivity":
                self.assertEqual("false", e.get(ANDROID+"exported"))
    def test_14_screen_bindings_exist_in_selected_layout(self):
        def ids(layout):
            result = set()
            for e in ET.parse(MAIN/("res/layout/"+layout+".xml")).iter():
                value = e.get(ANDROID+"id", "")
                if value.startswith("@+id/"): result.add(value.split("/")[1])
                if e.tag == "include": result |= ids(e.get("layout").split("/")[1])
            return result
        for file in (MAIN/"java").rglob("*Activity.kt"):
            source = file.read_text().split("\nprivate class ", 1)[0]
            match = re.search(r"screen\(R\.layout\.(\w+)", source)
            if not match: continue
            available = ids(match.group(1))
            for name in re.findall(r"findViewById(?:<[^>]+>)?\(R.id.(\w+)\)", source):
                with self.subTest(file=file.name, id=name):
                    self.assertIn(name, available)
    def test_15_visible_layout_strings_are_externalized(self):
        for file in (MAIN/"res/layout").glob("*.xml"):
            for e in ET.parse(file).iter():
                for key in ("text", "hint", "contentDescription"):
                    value = e.get(ANDROID+key, "")
                    with self.subTest(file=file.name, attribute=key, value=value):
                        self.assertTrue(not value or value.startswith("@"))
    def test_16_no_placeholder_launcher_or_dummy_tasks(self):
        sources = "\n".join(p.read_text() for p in (MAIN/"java").rglob("*.kt"))
        self.assertNotIn("Dummy list", sources)
        self.assertNotIn("TODO", sources)
        self.assertNotIn("Hello World", "\n".join(p.read_text() for p in (MAIN/"res/layout").glob("*.xml")))

if __name__ == "__main__":
    print("Portable checks: SQL assets and static Android resources only.", flush=True)
    unittest.main(verbosity=2)
