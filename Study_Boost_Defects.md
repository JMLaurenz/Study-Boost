# Study Boost - Defect Register

**Review date:** 22 November 2026  
**Baseline:** Supplied Study_Boost.zip and subsequent UI/navigation revisions  
**Reference:** Supplied Study Boost: Study Tracker PDF and later UI requests  
**Reviewed source:** `Study_Boost_editor_fix.zip` and its editor UI revision  
**Archive SHA-256:** `5D65BDCEDBFF004DD2ED2E30ED13B18FC94E561728BAF0675732D964B5A0FA19`

This register separates original defects, later issues reported by the user, and new risks identified by source review. A source observation is not presented as a device reproduction.

**Severity:** High blocks a core workflow or risks saved data; Medium affects validation, feedback, or usability.  
**Status:** Addressed in source means a correction is present. Open/source review means a likely issue was identified in code but still needs reproduction. Neither status alone establishes a passing Android device retest.

## 1. Original defects and corrections

| ID | Severity | Reproduction / evidence in original project | Expected behavior | Correction in updated source | Status and available verification |
| --- | --- | --- | --- | --- | --- |
| D01 | High | Launch MainActivity: activity_main.xml contains only Hello World; MainActivity has no navigation actions | A usable dashboard and routes to task/subject/timer screens | MainActivity now uses DashboardActivity with summary, task cards, navigation, and quick focus | Addressed in source; P13/P16 passed; runtime TC09 pending |
| D02 | High | TaskListActivity is absent from AndroidManifest.xml | Explicit navigation can open the task screen | All internal activities declared and nonexported | Addressed in source; P13 passed; Android launch pending |
| D03 | High | TaskListActivity contains three hardcoded sample tasks and no RecyclerView adapter assignment | Display saved task records and support real edits | SQLite query plus TaskAdapter; fixtures removed | Addressed in source; P07/P16 passed; TC03-TC05 pending |
| D04 | High | DatabaseHelper creates only subjects; no tasks or study_sessions tables/methods | Save coursework and linked focus sessions persistently | Version 2 schema and CRUD/session API | SQL assets verified by P01/P03/P06/P09; Android DB tests pending |
| D05 | High | activity_subject_add.xml has no Activity/controller or navigation route | Add/view/edit/delete subjects through the UI | Subject list and editor activities wired to DB | Addressed in source; resource checks passed; TC01 pending |
| D06 | Medium | Original insertSubject rejects blank code/name but accepts invalid difficulty and color; direct SQL accepted difficulty 99 and not-a-color during baseline review | Enforce difficulty 1-5 and valid color tags | Form/domain validation and DB constraints; the current UI uses preset colors | P02 and direct JVM validation tests passed; UI verification pending |
| D07 | High | Original onUpgrade drops subjects and recreates the table | Upgrade without discarding saved subjects | Version 1-to-2 migration retains IDs, normalizes invalid values, creates related tables | Exact SQL migration passed P10; SQLiteOpenHelper upgrade test pending |
| D08 | High | No task editor, due-date/hour validation, status update, or delete controls | Complete task CRUD with required fields and confirmations | Task editor, date picker, status controls, deletion dialogs | Addressed in source; TC02-TC06 and TC13-TC14 pending |
| D09 | High | No task/session foreign keys or cascade path exists | Category/task deletion removes dependent records after confirmation | Foreign keys enabled; both relationship levels use ON DELETE CASCADE | SQL cascades passed P04/P05; Android TC12 pending |
| D10 | High | No dashboard priority query/sorting or urgency calculation exists | Recommend pending work using deadline, effort, and difficulty | SQL-backed data and documented Kotlin urgency calculation; timed refresh | Relevant direct JVM priority tests passed; UI ordering TC11 pending |
| D11 | High | No timer activity, session table, or lifecycle recovery exists | Start/pause/finish focus sessions and persist them | Countdown, monotonic checkpoints, unique save tokens and totals; notes and history are not shown in the current timer UI | SQL token/total checks and direct JVM clock tests passed; TC10/TC15-TC18/TC22 pending |
| D12 | Medium | Visible text is mostly hardcoded; original empty-state ImageView has no accessibility treatment | Externalized text, useful empty state, accessible controls | String resources, vector empty illustration, labels/action descriptions, spoken countdown, day/night colors | XML/string checks passed P11/P12/P15; TalkBack/contrast/device checks pending |
| D13 | Medium | Original tests only assert 2+2=4 and the package name | Verify domain rules, data operations, migration, and lifecycle scenarios | Added 18 JVM tests, 7 Android DB tests, portable checks, and manual regression cases | 16 portable and 18 direct JVM tests passed; Android DB and Gradle test tasks not run in this review |

Original source paths mentioned above are under `app/src/main/`. In particular, the original database's invalid input acceptance was checked in an isolated Python SQLite database; no existing user database was modified.

## 2. Issues reported after the original review

| ID | Severity | Reported issue / expected behavior | Current source and verification status |
| --- | --- | --- | --- |
| D14 | High | Subject editor reported unresolved `TextView` references and related errors. It should compile. | The import and related color/return-type changes are present; the user later reported the app working. Exact device/build regression evidence was not recorded. |
| D15 | High | TaskListActivity reported `val cannot be reassigned` for its subject adapter. It should compile and populate the filter. | Uses `setAdapter(...)` for the dropdown; source correction present. User later reported the app working; individual filter retest not recorded. |
| D16 | Medium | On an empty Tasks page the bottom bar appeared above the bottom of the screen. It should remain aligned across tabs. | A weighted content area and shared shell layout were added. Verify empty/nonempty lists and screen sizes with TC26–TC28. |
| D17 | Medium | The bar highlighted Home while Tasks or Subjects was visible. It should highlight the visible screen. | Selected tab is reapplied after resume in source. Device retest TC27 is still required. |
| D18 | High | Android resource linking failed on `itemActiveIndicatorEnabled`. The app should link successfully. | Unsupported XML attribute was removed; the user subsequently reported the app working. The latest source still needs a recorded full build. |
| D19 | Medium | Tab screens and navigation bar appeared to pop, with a gap above the phone navigation area. Switching tabs should feel smooth and the areas should meet. | Content-only animation, disabled window transition and revised insets were added. Tabs remain separate Activities, not one persistent navigation view; the latest behavior has not been confirmed on the phone. Retest TC26–TC28. |
| D24 | Medium | Open Add Subject or Add Task: screenshots show overlapping field labels/hints, compressed difficulty numbers, and Edit headings for new records. | Addressed in source: labels are separate from hints, fields are rounded, difficulty uses five numbered circles, headings reflect Add/Edit, and Inter is bundled. AAPT2 resource compilation and an isolated editor Kotlin compile passed; Android Studio build and visual/device retest TC38 remain pending. |

## 3. Source-review findings and retests

These findings are based on the reviewed source, not a recorded phone test. Reproduce before marking their runtime behavior confirmed.

| ID | Severity | Evidence and reproduction to try | Expected result / next action |
| --- | --- | --- | --- |
| D20 | Medium | Previously `dueDateLayout` identified the subject field, so the calendar listener was attached to the wrong end icon. | Addressed in source: the ID and calendar listener now belong to the due-date field; the subject dropdown retains its own control. Retest both controls with TC32. |
| D21 | Medium | Tasks saves All as a null status, but restoration uses `getString("status") ?: PENDING`. Select All and rotate. | All should remain selected. Distinguish a saved All value from a missing state and retest TC35. |
| D22 | Medium | The timer persists its token immediately after loading a task, even before Start. A later timer opening uses any stored token. Open task A's timer, go back without starting, then open task B's timer. | A ready-but-unstarted visit should not lock the user to A. Avoid retaining an unstarted token or provide a safe way to switch; retest TC34. |
| D23 | Medium | The blank date picker previously initialized from UTC's date while validation used the local date. | Addressed in source using the phone's local date; two JVM checks covering time zones east and west of UTC passed. Device retest TC36 is pending. |

## 4. Current verification gaps and limitations

| ID | Type | Current condition | Impact / next action |
| --- | --- | --- | --- |
| V01 | Verification gap | An earlier review-environment Gradle download failed. The user later reported the app working and a local debug APK was found, but its full build log and exact source match were not verified. | Run and record the latest Android Studio build and lint; do not treat the historical network error as an active user-device failure. |
| V02 | Verification gap | The latest UI, Activity lifecycle and navigation revisions have no recorded complete device run. | Complete TC01–TC38, including save-time rotation, process recovery and API/device coverage. |
| L01 | Known limitation | No background completion notification/service | Countdown reconciles and logs when reopened; a background expiry does not notify the student |
| L02 | Known limitation | The current timer screen has no session history, notes or discard controls | History/export would require a future UI feature; saving uses an empty notes value. |
| L03 | Known limitation | No in-app backup/import or cloud synchronization | Data management relies on local storage and any Android-managed backup; cross-device workflows are not provided |
| L04 | Known limitation | Subject color selection offers six presets; arbitrary new hex colors are not offered by the editor | Decide whether a custom picker is wanted; existing saved custom colors can be retained. |
| L05 | Verification gap | The date selector uses Android's Material calendar; its month/year controls may differ from the supplied mockup. | Check month and year navigation against the reference on a device in TC32 before declaring the interaction complete. |
| DOC01 | Documentation mismatch | The supplied user guide describes typed color/date/estimate inputs and timer notes, discard and history, which do not match the current screen. | The guide was left unchanged at the user's request. Do not rely on those sections as current operating instructions. |

For the editor revision, 16 portable checks and 18 direct JVM logic tests passed. AAPT2 compiled the resources, and the two editor controllers compiled against cached libraries using temporary resource stubs. These partial checks do not replace an Android Gradle build, resource linking, or a visual/device test. The user reported an earlier version working, but that does not verify every scenario or the latest revision's complete build. D21 and D22 remain open source-review findings.

## 5. Defect closure criteria

Close a defect after recording:

1. The exact source/build revision and device/API level.
2. A repeat of its original reproduction steps.
3. The expected result and observed result.
4. A passing regression test or clear manual evidence.
5. Any related persistence, cascade, or lifecycle checks.

For a newly discovered issue, use:

~~~text
Defect ID:
Title:
Severity:
Build / device / API:
Preconditions and exact steps:
Expected result:
Actual result:
Evidence:
Correction:
Retest result / date:
Status:
~~~
