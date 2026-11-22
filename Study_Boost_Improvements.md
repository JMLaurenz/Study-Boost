# Study Boost - Improvements and Proposal Coverage

**Prepared:** 22 November 2026  
**Basis:** Supplied application archive and Study Boost: Study Tracker PDF, plus the subsequent UI and navigation requests  
**Reviewed source:** Latest shared Study_Boost_updated.zip, SHA-256 `2BD040F88AE61CF6D9BCB2E34B3DD77C71FCE675C9AF3519B3A6F8869F6739C3`

## 1. Improvements implemented

| Area | Original progress | Updated implementation | Benefit |
| --- | --- | --- | --- |
| Dashboard | Placeholder launcher | Pending/completed counts, total saved hours, top five priorities, quick focus and long-press priority information | Connects planning and focus logging in one starting screen |
| Subjects | Data model, insert helper, disconnected layout | Create/read/update/delete screens, a six-color dropdown and difficulty buttons 1–5 | Makes the original subject work usable with simpler selection |
| Tasks | Dummy task data and partial search listener | Full SQLite CRUD, descriptions, due dates, estimated hours, completion controls | Provides persistent coursework management |
| Finding work | In-memory filtering | Parameterized SQL search over title/subject/code plus combined filters; Pending is the initial status | Searches saved records and safely handles literal % and _ characters |
| Prioritization | Absent | Pending-only urgency sorting and minute refresh | Uses deadline, difficulty and workload instead of manual ordering |
| Task input | Partial or absent | Non-typing calendar and preset estimated-time dropdown | Reduces manual entry; the task-editor end-icon wiring still needs correction and device retest (D20) |
| Focus sessions | Absent | Circular countdown with duration presets, pause/resume and pause-gated Finish & save | Captures actual study time against the selected task; notes, discard and history controls are not shown |
| Timer recovery | Absent | Monotonic timestamps, durable checkpoints, reboot detection and save tokens | Supports interrupted sessions and protects against duplicate session saves |
| Data integrity | One unconstrained subject table | Three related tables, constraints, indexes and enabled foreign keys | Blocks invalid/orphan records and enforces deletion rules |
| Upgrades | Drop/recreate subjects | Migration preserves IDs and valid values; normalizes bad defaults | Retains existing subject progress |
| Feedback | No connected save/delete flow | Field errors, saved/error Toasts, confirmation dialogs, empty states | Makes the result of an action visible |
| Responsiveness | Minimal code | Database operations serialized on a worker executor; cursors closed with use | Keeps routine DB work away from the UI thread |
| Resources/accessibility | Hardcoded text and default illustration | Externalized strings, custom vector, labels, action descriptions, light/dark colors | Improves consistency and prepares the app for accessibility review |
| Bottom navigation | Separate layouts and activity transitions moved the bar | Shared shell layout, selected-tab reapplication, directional content animation and adjusted bottom insets | Intended to reduce jumping and the system-navigation gap; latest behavior requires device retesting |
| Launcher identity | Default Android icon | Manifest uses the school vector for icon and round icon; label is Study Boost | Provides a recognizable launcher entry |
| Testability | Template example tests | SQL assets, domain-rule separation, meaningful automated suites and a manual plan | Makes core behavior easier to inspect and verify |

These are source-level changes, not a claim that every screen has passed device testing. The app still uses separate Activities for the three tabs; the shell is shared as a layout, but the same view instance is not retained across tabs. The user reported an earlier version working and reported tab-transition/gap issues. Check the latest version using [Testing](Study_Boost_Testing.md).

## 2. Coverage against the PDF

| Proposal requirement | Implementation location | Coverage / interpretation |
| --- | --- | --- |
| Subject CRUD, colors, difficulty 1-5 | SubjectListActivity, AddEditSubjectActivity, DatabaseHelper | CRUD and six preset colors are implemented; arbitrary new custom-color entry is not exposed. Existing custom values can be retained. |
| Task CRUD, descriptions, estimates and dates | TaskListActivity, AddEditTaskActivity, DatabaseHelper | Implemented with preset estimates and calendar dates; task-editor picker wiring needs device retest and correction |
| Required fields, positive estimates, future dates | StudyLogic.Validation and editor forms | Implemented; unchanged overdue dates are allowed during edits |
| SQLite persistence | schema.sql and DatabaseHelper | Implemented; portable committed-file persistence check passed |
| Search by title or subject; subject/status filtering | TaskListActivity and DatabaseHelper.getTasks | Implemented; subject dropdown and status chips |
| Algorithmic urgency matrix | Priority.score and DashboardActivity | Implemented with a documented formula because the PDF supplies no equation |
| Focus logger | StudyTimerActivity and study_sessions | Countdown with selectable presets and pause-gated save; UI deliberately excludes session notes, discard and session history |
| Confirmation and immediate feedback | ScreenActivity and editor/list actions | Implemented using dialogs and Toasts |
| Useful empty states and vector graphics | view_empty.xml and ic_empty.xml | Implemented |
| Externalized strings, screen-reader descriptors, themes | res/values, res/values-night, layouts and adapters | Implemented in source; accessibility testing pending |
| Explicit intents and result refresh | Extras and ScreenActivity child result launcher | Implemented; task ID/title extras and result callbacks supplied |
| Rotation-safe timer state | FocusClock and StudyTimerActivity checkpoints | Implemented; actual lifecycle tests pending |
| Cascading category deletion | Both foreign-key links, onConfigure | Portable SQL cascade checks passed; Android verification pending |

The supplied PDF and older documentation describe session notes. The later UI request explicitly removed notes from the focus screen, so that is a deliberate interface change rather than an implemented notes feature. The database still has a notes field, and the current timer saves an empty value.

The PDF's TC04 says “nonarchived,” but its schema has no archive flag. This version interprets that as all nondeleted tasks. Archiving is a possible later extension.

The PDF also contains incomplete SQL-like notation. This update uses a valid six-digit default color, an explicit PENDING/COMPLETED status constraint, and a UTC timestamp default. It retains integer duration_mins while adding exact seconds and a unique token.

## 3. Priority formula and timing decisions

~~~text
urgency = difficulty × estimated hours / max(1, remaining hours)
remaining hours = (end of local due date - current time) / 3,600,000
~~~

Completed tasks are excluded. Ties use due date, then task ID. The formula is a documented project choice, not a clinically or scientifically validated measure of student workload.

Study totals use exact logged seconds. The compatibility duration_mins column rounds up, so totals must not be recomputed by summing that rounded column. Logged time does not automatically change the task's entered estimate.

The countdown defaults to 25 minutes; the visible choices are 15, 25, 30, 45, 60, 90 and 120 minutes. It excludes pauses, caps recorded time at the planned duration, retains one active session, and uses idempotent save tokens. Reboot recovery is intended to pause at the last checkpoint rather than guess elapsed time across a new device boot; device verification is pending. A ready-but-unstarted visit may reserve the active session (D22).

## 4. Recommended next improvements

| Priority | Improvement | Reason | Acceptance criterion |
| --- | --- | --- | --- |
| P0 | Fix and retest task-editor icon wiring, All-filter restoration and unstarted focus recovery | Source review found paths that may obstruct or change these workflows (D20–D22) | Device tests TC32, TC34 and TC35 pass after corrections; no unintended data changes |
| P0 | Complete Android build/lint and device verification | The latest APK build/lint and UI behavior have not been fully verified in this review | Android Studio debug build, lint and connected tests succeed; TC01–TC37 have recorded outcomes |
| P0 | Stress-test writes during rotation/process interruption | Async operations and navigation need device evidence | A rapid Save/rotate/relaunch sequence leaves exactly one intended record and recoverable UI state |
| P1 | Verify and refine tab transitions and system insets | The latest approach still uses separate Activities and may not feel like a persistent navigation host | TC26–TC28 pass on the target phone with gesture and three-button navigation; if needed, use one persistent host for the tabs |
| P1 | Fix local-date initialization in the picker | UTC-based initial selection can differ from the phone's local date around midnight (D23) | TC36 selects the correct local date in tested time zones |
| P1 | Add an opt-in completion notification | Students can miss a timer ending while away from the screen | Supported Android background behavior is implemented and tested; one timely notice is delivered with no duplicate session |
| P1 | Add session-history browsing and export if desired | Sessions are stored, but no history screen is currently displayed | All sessions can be filtered/viewed; exported totals match exact seconds in SQLite |
| P1 | Add explicit backup/export and restore | Local records need a user-controlled recovery route | A tested round trip restores subjects, tasks and sessions without ID/link corruption |
| P1 | Improve save/error recovery UX after device testing | Generic errors offer limited recovery detail | Failed writes preserve input; retry avoids duplicate records and explains actionable failures |
| P2 | Expand color selection if custom colors are needed | The current dropdown supplies six preset colors, not an arbitrary color picker | An optional picker stores valid #RRGGBB and remains understandable without relying on color alone |
| P2 | Consider remaining-work estimates | Logged study time does not reduce the priority numerator | A documented user-adjustable remaining-effort model is unit-tested and explained in the UI |
| P2 | Add archive/undo workflows | Current confirmed deletions are permanent | Archived records are filtered consistently; undo restores relationships within a defined period |
| P2 | Expand localization and accessibility testing | Resource extraction alone does not prove usability | TalkBack, larger text, dark mode, narrow screens and a second language pass documented checks |
| P2 | Consider a repository/ViewModel or Room refactor as complexity grows | More features will increase lifecycle and migration complexity | Existing CRUD/migration tests remain green and UI state survives configuration changes |

P0 means complete before claiming the updated app is verified. P1/P2 are proposed enhancements, not features already delivered. The supplied user guide was left unchanged as requested; its typed-entry and timer notes/history instructions do not match the current UI and should not be treated as a current walkthrough.

## 5. Suggested implementation order

1. Fix the source-level issues in the defect register, then repeat the associated device tests.
2. Verify the latest navigation and system insets on the target phone, and finish the Android build, lint and connected-test checks.
3. Consider completion notifications, history/export and user-controlled recovery if those features are wanted.
4. Keep the testing and defect records synchronized with actual results; revise the separately supplied guide only within the user's requested scope.
