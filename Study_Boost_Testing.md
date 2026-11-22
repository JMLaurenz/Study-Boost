# Study Boost - Testing Plan and Results

**Review date:** 22 November 2026  
**Baseline:** Supplied Study_Boost.zip and the subsequent UI and navigation changes  
**Requirements:** Supplied Study Boost: Study Tracker PDF, especially TC01-TC12, together with the later UI instructions  
**Source under review:** Latest shared Study_Boost_updated.zip; database version 2  
**Archive SHA-256:** `2BD040F88AE61CF6D9BCB2E34B3DD77C71FCE675C9AF3519B3A6F8869F6739C3`

## 1. Result summary

| Verification layer | Result | What it establishes |
| --- | --- | --- |
| Python portable checks | **16 passed** in this review | Shipped SQL assets and selected static Android resource and screen-binding checks |
| Kotlin/JVM logic tests | **16 passed** in this review | Domain validation, priority calculation and timer arithmetic, compiled and run directly with Kotlin and JUnit |
| Android database tests | **7 provided; not run** | DatabaseHelper and Android SQLite migration still need a device/emulator run |
| Android build | **Partial evidence** | The user reported that an earlier version worked, and a debug APK was found in the local Android Studio project's build intermediates. A complete build log and an exact source-to-APK match were not verified. |
| Android lint | **Not run** in this review | No clean lint result is claimed |
| Manual UI and device tests | **Not formally run** in this review | User screenshots and video establish reported UI problems, but do not establish all test outcomes or verify the latest changes |

An earlier build attempt in the review environment used:

~~~sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
~~~

It stopped while downloading the Gradle 9.6.0 distribution:

~~~text
java.net.SocketException: Network is unreachable
~~~

That attempt preceded later changes and is historical evidence, not the result of the user's subsequent Android Studio build. This review did not run an Android Gradle build, lint or connected tests. The source files inspected in the user's local Android Studio project match the corresponding reviewed navigation, task-editor, timer, shell and manifest files; this limited comparison does not prove the APK matches the entire archive.

## 2. Checks actually executed

Run from the project root using Python 3:

~~~sh
python3 tools/check_project.py
~~~

This uses Python's SQLite engine and reads the exact SQL assets shipped in the app. The task projection and search predicate are extracted from DatabaseHelper.kt; the test decodes Kotlin string escaping before executing the predicate. It does not execute Kotlin query construction or Android's SQLiteOpenHelper.

| ID | Check | Actual result |
| --- | --- | --- |
| P01 | Create subjects, tasks, and study_sessions tables | Passed |
| P02 | Reject blank subject fields, malformed colors, and invalid/noninteger difficulty | Passed |
| P03 | Enforce task parent, nonblank title, positive hours, and valid status | Passed |
| P04 | Delete a subject and cascade through tasks to sessions | Passed |
| P05 | Delete a task, retain its subject, and remove its sessions | Passed |
| P06 | Reject orphan/invalid sessions and duplicate session tokens | Passed |
| P07 | Run the shipped task join and retrieve exact accumulated study seconds | Passed |
| P08 | Execute search SQL with a literal percent sign and combined subject/status criteria | Passed |
| P09 | Commit a file database, reopen it, and retrieve saved session seconds | Passed |
| P10 | Execute the version 1-to-2 SQL migration; preserve IDs/valid values and normalize invalid legacy fields | Passed |
| P11 | Parse all main Android XML files | Passed |
| P12 | Resolve referenced local resources from XML and Kotlin | Passed |
| P13 | Verify declared activity source files, one launcher, and nonexported internal screens | Passed |
| P14 | Check activity view bindings against their selected content layouts and included views | Passed; does not execute the shared shell or check Android lifecycle behavior |
| P15 | Check visible layout text, hints, and descriptions use resources | Passed |
| P16 | Confirm the launcher placeholder and dummy-task implementation were removed | Passed |

The source project's `docs/evidence/` contains earlier portable-check and Gradle-attempt logs. The 16 portable checks were rerun successfully on the source under review; those historical files were not rewritten for this review.

Python SQLite checks cannot establish Android lifecycle behavior, widget rendering, actual Kotlin validation, TalkBack behavior, or compatibility with every Android SQLite version.

## 3. Automated Android/JVM tests to run

### Local JVM tests

File: `app/src/test/java/com/example/study_boost/StudyLogicTest.kt`.

The 16 tests cover blank subject inputs, difficulty limits, color format, invalid/leap dates, missing task fields, nonfinite/nonpositive hours, future-date rules, unchanged overdue dates, urgency response to deadlines/effort/difficulty, the overdue divisor floor, pause exclusion, repeated Start, checkpoint accounting, duration caps, and negative elapsed-time protection. In this review, `StudyLogic.kt` and `StudyLogicTest.kt` were compiled with the installed Kotlin compiler and run with JUnit 4.13.2: **OK (16 tests)**. This was a direct JVM run, not the Android Gradle test task.

~~~powershell
.\gradlew.bat :app:testDebugUnitTest
~~~

### Android database tests

File: `app/src/androidTest/java/com/example/study_boost/DatabaseHelperTest.kt`.

The seven tests exercise subject CRUD; task CRUD/status; text/subject/status filtering; unique-token logging and exact totals; two-level cascades; reopen persistence; and the actual SQLiteOpenHelper version-1 migration.

Each test uses a unique temporary database name and removes that database afterward.

~~~powershell
.\gradlew.bat :app:connectedDebugAndroidTest
~~~

Start an emulator or attach a debug-enabled phone before the connected-test command. The Gradle unit-test task and the connected-test results have **not** been observed in this review.

## 4. Manual tests from the PDF

**Setup:** Build/install the updated app. Create **CS101 / Computing** using Ocean blue and difficulty **3**, and **MATH / Mathematics** using another available color and difficulty **2**. Use dates relative to the device's local current date. The editor offers a fixed color palette, rather than arbitrary hex entry.

Every row below has **formal runtime status: Not run** for the reviewed source. The implementation column describes source review and any relevant portable/JVM result, not a passed UI test.

| ID | Procedure / input | Expected result | Implementation and current evidence |
| --- | --- | --- | --- |
| TC01 | Subjects > Add subject; leave code/name blank and save | Inline errors; no subject inserted | Subject form and Validation.subject implement checks; SQL blank-field rejection passed in P02 |
| TC02 | Add a task with a title/subject but do not select a due date; save | A date error appears and no task is saved | Estimated time defaults to 1h 30m; it is not an empty typed input. Date validation is covered by the passing JVM tests; UI pending |
| TC03 | Save a valid task with tomorrow's due date and 1.5 hours | Record appears, saved Toast appears, record survives reopening | Save/query paths implemented; SQL persistence passed in P09; UI pending |
| TC04 | Open Tasks with pending and completed fixtures, then select All | Pending is selected initially; All displays both statuses and the task count matches | SQLite-backed adapter and status chips implemented; no archive field exists in supplied schema |
| TC05 | Edit estimated hours from 1.5 to 3; save and reopen | Updated value persists and list refreshes | Update/result-refresh implemented; Android database test pending |
| TC06 | Delete a task; first Cancel, then confirm | Cancel keeps it; confirm removes task and linked sessions | Dialog implemented; task/session SQL cascade passed in P05 |
| TC07 | Search Computing, then CS101; combine with subject/status filters | Only records matching all criteria remain | Search/filter implementation present; underlying SQL checks passed in P08 |
| TC08 | Search a value that matches no task or subject | Task list hides and empty-state view appears | Empty-state visibility implemented; widget behavior pending |
| TC09 | Tap a pending dashboard task or Focus button | Timer opens for that task; explicit extras carry task ID/title | Intent extras implemented; timer loads authoritative title from DB; runtime pending |
| TC10 | Start a timer, study for at least one second, pause, then tap Finish & save | One linked session is stored and the total rises by actual focused time | Pause-gated save and logger/token implemented; notes were intentionally removed from the UI; SQL P06-P07 and timer JVM tests passed; Android timing pending |
| TC11 | Create otherwise identical tasks due tomorrow and one week later | Nearer task has higher urgency and appears ahead | Formula and minute refresh implemented; relevant JVM priority tests passed; UI pending |
| TC12 | Delete a subject with tasks and saved sessions; confirm | Subject, linked tasks, and linked sessions disappear | Foreign keys enabled in onConfigure; SQL cascade passed in P04; Android verification pending |

## 5. Additional regression tests

| ID | Procedure | Expected result | Actual result |
| --- | --- | --- | --- |
| TC13 | Exercise invalid difficulty, color, hours and date values in the domain/DB tests; check the UI selection controls separately | Domain and SQL constraints reject invalid values; controls offer valid choices | Relevant direct JVM and portable SQL checks passed; Android UI portion not run |
| TC14 | Edit an overdue task without changing its date, then try another past date | Original date is allowed; replacement past date is rejected | Not run |
| TC15 | Start timer; rotate twice; pause 30 seconds; resume | Elapsed study time survives rotation and excludes pause | Not run |
| TC16 | Start a selectable 15-minute timer, leave the screen, return after expiry | Elapsed time is capped at the planned duration; exactly one session is saved | Clock capping passed as a JVM test; end-to-end timer case not run. A one-minute choice is not exposed in the current UI. |
| TC17 | Start a session, press Home, kill the background process without clearing data, relaunch | Quick focus resumes the same token/task and reconciles elapsed time | Not run |
| TC18 | Start then pause a timer; reboot device; relaunch | Saved records remain; interrupted timer is paused at checkpoint | Not run |
| TC19 | With version 1 fixture installed, update in place without uninstalling | Subject IDs/data retained, invalid defaults normalized, new tables available | SQL migration passed P10; on-device result not run |
| TC20 | Rotate a partly completed subject/task form and a filtered task list; test All, Pending and Completed separately | Entered fields and the chosen status remain | Not run; source review identifies an All-to-Pending restoration risk (D21) |
| TC21 | Enable TalkBack, large font, and dark mode; navigate all screens | Controls are announced, countdown includes remaining time, content is legible/reachable | Not run |
| TC22 | Attempt session save while storage/save fails, then retry | Paused session retained; manual retry creates at most one row | Not run |
| TC23 | Start a session, leave timer, delete its subject, then resume timer | Missing task is handled without crash; obsolete timer cleared | Not run |
| TC24 | Test on API 24 and API 37, including keyboard and screen edges | No startup/widget/resource errors; controls remain visible | Not run |
| TC25 | Rotate immediately after pressing a subject/task Save button | A successful write produces only one record and a usable form/navigation state | Not run |
| TC26 | Switch Home → Subjects → Tasks and back, including rapid taps | Content moves in the intended direction and the navigation bar remains visually stationary | Not run on the latest source; screens are still separate activities, so seamlessness must be observed |
| TC27 | Visit each tab, return with Back, and rotate | The highlighted tab always matches the visible screen | Not run; selection is reapplied in source |
| TC28 | Test gesture and three-button phone navigation, rotation and the Tasks search keyboard | The app bar and phone navigation area meet without an unintended blank gap; controls remain visible | Not run on the latest source; inset handling changed |
| TC29 | Install/update the app and inspect its launcher entry | The label is Study Boost and the school icon is shown | Manifest points both icon variants to the school vector; on-device result not recorded |
| TC30 | Long-press the dashboard priority information icon | Priority explanation opens; urgency calculations are not permanently displayed on task cards | Listener exists in source; UI not run |
| TC31 | Create/edit subjects using all palette options and difficulty buttons 1–5 | The chosen color and difficulty persist and appear on relevant cards | Not run |
| TC32 | In the task editor tap the subject arrow, due-date field and calendar icon; try month/year navigation and estimated-time choices | Subject arrow opens subjects; date controls open the picker; options can be selected without typing | Not run; a source-level icon wiring issue is recorded as D20. Verify the requested month/year selection behavior. |
| TC33 | Check the ready, running and paused focus states | Length is selectable before starting; Finish & save is enabled after pause with elapsed time; notes and discard controls are absent | Not run; source exposes presets 15, 25, 30, 45, 60, 90 and 120 minutes |
| TC34 | Open Focus for task A, go back without starting, then open Focus for task B | A ready-but-unstarted visit should not prevent selecting B | Not run; source review identifies a possible saved-token lock (D22) |
| TC35 | Select All in Tasks, rotate, and return to the list | All remains selected and both statuses remain visible | Not run; source review identifies a restoration risk (D21) |
| TC36 | Open a blank due-date picker around local midnight in a non-UTC time zone | The initial date matches the phone's local date | Not run; source uses UTC for the initial date (D23) |
| TC37 | Update the installed debug app without clearing data and reopen saved subjects, tasks and study totals | Existing data and launcher entry remain usable after update | Not run; do not uninstall or clear storage solely for this check |

Background notifications, archive workflows, cloud sync, and in-app exports are outside the current implementation and have no passing test claim.

## 6. Record final device results

Before submission, run the commands below and complete the manual rows with actual observations. A defect should be closed only after its acceptance test passes. The direct JVM result above does not replace the Gradle task.

~~~powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
~~~

Record the Android Studio version, Gradle JDK, device model/API level, package version, test date, and source revision. Preserve:

- Build output and unit/instrumented test reports.
- Screenshots showing validation, search, empty states, deletion confirmations, and dashboard/timer navigation.
- Before/after row counts for save, cascade, migration, and session-retry checks.
- Measured elapsed/pause time for lifecycle tests.
- Any failure's exact steps, expected/actual behavior, and relevant Logcat output.

Generated reports normally appear under `app/build/reports/` after successful test/lint tasks.

## Technical references

The review used the supplied project and PDF as the requirements source. These official Android references informed the implementation; they do not substitute for running the tests:

- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [Built-in Kotlin support](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [SQLiteOpenHelper configuration and migration](https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper)
- [SystemClock elapsedRealtime](https://developer.android.com/reference/android/os/SystemClock)
- [RecyclerView dependency](https://developer.android.com/jetpack/androidx/releases/recyclerview)
