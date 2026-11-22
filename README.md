# Study Boost

Android coursework manager implemented in Kotlin with SQLite, based on the supplied **Study Boost: Study Tracker** project proposal.

This update replaces the placeholder launcher and sample task list with subject management, task management, a priority dashboard, and a persistent focus-session timer.

## Open and run

1. Extract the archive and open the **Study_Boost** folder in Android Studio.
2. Install Android SDK Platform **37** in SDK Manager. The project keeps the supplied compile/target SDK 37, Android Gradle Plugin 9.4.1, and Gradle wrapper 9.6.0.
3. Select a compatible Gradle JDK in Android Studio. The original machine-specific JDK 25 provisioning file was removed; use the IDE's compatible bundled JDK.
4. Allow Gradle Sync to download the declared dependencies.
5. Select an emulator or connected Android device running Android 7.0 / API 24 or later and click **Run**.

No account, test credentials, or network permission is needed for the app's features. Initial development setup needs internet access to obtain build tools and dependencies.

To build from PowerShell in the project folder:

~~~powershell
.\gradlew.bat :app:assembleDebug
~~~

On macOS/Linux:

~~~sh
chmod +x gradlew
./gradlew :app:assembleDebug
~~~

A successful debug build produces `app/build/outputs/apk/debug/app-debug.apk`. No APK is supplied because Android compilation could not be completed in this review environment.

## Included documentation

- [User guide](docs/Study_Boost_User_Guide.md)
- [Testing and evidence](docs/Study_Boost_Testing.md)
- [Defect register](docs/Study_Boost_Defects.md)
- [Improvements and proposal coverage](docs/Study_Boost_Improvements.md)

## Verification status

**16 portable checks passed.** These execute the shipped SQL assets using Python SQLite and inspect Android XML, resource references, navigation declarations, and screen bindings. They do not compile Kotlin or execute Android activities.

**16 JVM tests and 7 Android database tests are included but were not run.** The build attempt stopped at the Gradle distribution download with `java.net.SocketException: Network is unreachable`. The environment also has no Android SDK or emulator.

~~~sh
python3 tools/check_project.py
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
~~~

Use `.\gradlew.bat` instead of `./gradlew` on Windows. Connected tests require a device/emulator. See the testing document for the manual TC01-TC12 plan and additional lifecycle checks.

## Implementation map

| File | Responsibility |
| --- | --- |
| MainActivity.kt / DashboardActivity.kt | Launcher, counts, study totals, top five priority tasks, quick focus |
| SubjectListActivity.kt / AddEditSubjectActivity.kt | Subject CRUD, color tags, difficulty, validation |
| TaskListActivity.kt / TaskAdapter.kt | SQLite-backed task rows, search, subject and status filters |
| AddEditTaskActivity.kt | Create/edit form, date picker, validation, task completion |
| StudyTimerActivity.kt / StudyLogic.kt | Countdown, pause/resume, pause-gated save, clock recovery, priority rules |
| DatabaseHelper.kt | CRUD, session logging, parameterized queries, migration, cursor management |
| ScreenActivity.kt | Insets, explicit intents/results, background database work, feedback |
| app/src/main/assets/schema.sql | Database version 2 tables, constraints, indexes, cascades |
| app/src/main/assets/migration_1_2.sql | Preserve and normalize version 1 subjects |
| tools/check_project.py | Portable SQL and static resource checks |

Kotlin files are under `app/src/main/java/com/example/study_boost/`.

## Data and design decisions

- Database name and package ID remain `StudyBoost.db` and `com.example.study_boost`.
- Existing version 1 subject IDs are preserved. Invalid legacy colors become #2563EB, invalid difficulty becomes 3, and blank legacy names/codes receive fallback labels.
- Deleting a subject deletes its tasks and their sessions. Deleting a task deletes its sessions. Both actions require confirmation.
- The proposal leaves the urgency formula unspecified. This implementation uses **difficulty × estimated hours ÷ max(1, hours until the end of the local due date)**. Dashboard ordering refreshes every minute while visible and after returning from edits.
- New/changed due dates must be after today. An existing overdue task can keep its unchanged date when edited.
- Exact session seconds drive totals. The proposal's integer `duration_mins` column is also retained and rounded upward.
- Timer checkpoints use app preferences; saved records use SQLite. Active timer checkpoints are excluded from Android backup to avoid restoring a monotonic timestamp onto another device.
- Subject filtering uses a dropdown; status filtering uses chips. Lists show all nondeleted records because the supplied schema has no archive field.
- The timer is a countdown with a default of 25 minutes and a selectable length of 1-180 minutes. There is no background completion notification.
