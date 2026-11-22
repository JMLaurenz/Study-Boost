# Study Boost - User Guide

**Prepared:** 22 November 2026  
**Applies to:** Updated Study_Boost project

Study Boost helps students organize coursework by subject, choose priority tasks, and record focused study time.

## 1. Start the app

Install/run the project using the instructions in [README](../README.md). Launch **Study Boost** to open the dashboard.

The dashboard displays:

- Counts of pending and completed tasks.
- Total saved study hours.
- Up to five pending tasks, ordered by urgency.
- **Subjects**, **Tasks**, and **Quick focus / resume** controls.

An informative empty state appears before records are added.

## 2. Add and manage subjects

1. Open **Subjects** and tap **Add subject**.
2. Enter a subject code and name, such as **CS101** and **Data Structures**.
3. Enter a color tag in **#RRGGBB** format, such as **#2563EB**.
4. Enter a whole-number difficulty rating from **1** to **5**.
5. Tap **Save subject**.

Blank codes/names, malformed colors, and difficulty values outside 1-5 show field errors and prevent saving.

To update a subject, tap **Edit**, change its details, and save. Difficulty changes affect the priority of its pending tasks; color changes appear on its task cards.

To delete a subject, tap **Delete** and read the confirmation. Confirming permanently removes the subject, all its tasks, and all linked study sessions. **Cancel** keeps the records.

## 3. Add a task

1. Open **Tasks** and tap **Add task**.
2. Choose a subject. If none exists, tap **Manage subjects** to create one, then return.
3. Enter a task title and an optional description.
4. Tap **Choose date** or enter a date in **YYYY-MM-DD** format.
5. Enter estimated hours, such as **1.5**.
6. Leave **Completed task** unchecked for unfinished work.
7. Tap **Save task**.

New due dates must be after today. Estimated hours must be a finite positive number no greater than 10000. Missing/invalid inputs show inline errors. Saving displays a confirmation and refreshes the task list.

Dates have no time-of-day field. Priority calculations treat the selected local due date as ending at the next midnight.

## 4. Find, edit, complete, or delete tasks

| Action | Steps |
| --- | --- |
| Search | Type a task title, subject name, or subject code into the search box. Matching is case-insensitive for ordinary Latin text. |
| Filter by subject | Choose a subject from **Subject filter**, or select **All subjects**. |
| Filter by status | Tap **All**, **Pending**, or **Completed**. |
| Combine filters | Use text search, subject, and status together; a record must satisfy all active criteria. |
| Open details/edit | Tap the task title in the Tasks screen or tap **Edit**. The form displays its description and other saved values. |
| Complete work | Tap **Mark complete**, or enable **Completed task** in the form and save. |
| Reopen work | Tap **Mark pending**, or clear **Completed task** in the form and save. |
| Delete | Tap **Delete**, then confirm. The task and its study sessions are removed permanently. |

Clear your search and choose **All subjects** and **All** if the list unexpectedly appears empty.

When editing an overdue task, its existing due date can remain unchanged. A replacement date must be after today. Completed tasks remain available in the Tasks screen and are excluded from dashboard priority recommendations.

## 5. Understand priority ordering

The app calculates:

~~~text
urgency = subject difficulty × estimated task hours
          / max(1, hours until the end of the due date)
~~~

A higher score indicates a stronger recommendation. For otherwise identical tasks, a nearer deadline raises urgency. Greater difficulty or estimated effort also raises it. Due/overdue tasks use a minimum divisor of 1, preventing division by zero.

Equal scores are ordered by earlier due date, then task ID. The dashboard recalculates approximately every minute while visible and when returning from changes. The Tasks screen itself lists tasks by due date.

**Example:** For a task with difficulty 3, estimated hours 4, and 24 hours remaining, urgency is **0.5**. With only 6 hours remaining, it is **2.0**.

Estimated hours are the entered workload estimate; logging study time does not automatically reduce that estimate.

## 6. Record a focus session

1. Tap a pending task on the dashboard, tap its **Focus** button, or use **Quick focus / resume**.
2. Set a focus length from **1-180 minutes** before starting. The default is **25 minutes**.
3. Tap **Start / resume**.
4. Use **Pause** to exclude interruptions, then **Start / resume** to continue.
5. Enter optional session notes.
6. Tap **Finish and save** to log elapsed study time, or let the countdown finish while the timer screen is active.

At least one full second is required to save. Partial sessions are allowed. Finishing saves one session for the selected task and returns to the previous screen. It does not mark the task complete.

**Discard** opens a confirmation and removes only the current unsaved session and notes. Previously saved sessions are kept.

The timer screen shows the ten most recent saved sessions for that task, with a timestamp, minutes/seconds, and notes. Stored timestamps use UTC; dashboard study-hour figures are rounded for display.

## 7. Resume an interrupted session

- Rotating the device or leaving the timer screen retains a checkpoint.
- Time continues while a running session is in the background; pausing stops the count.
- Use **Quick focus / resume** to return to the active session.
- Only one active session is maintained. Selecting another task resumes the existing session until it is saved or discarded.
- If the app is closed while a session save is pending, reopening retries with the same token to prevent duplicate session records.
- After a device reboot, a running session resumes **paused at the last checkpoint**; the app does not count unverified time across a reboot.

There is no completion alarm or notification while the app is in the background. If the timer expires while away, reopening the timer records up to the planned duration.

If a save fails, the timer remains paused. Tap **Finish and save** to retry. If its task was deleted, the app clears the obsolete active timer.

## 8. Saved data and troubleshooting

Saved subjects, tasks, and sessions remain in the local SQLite database after closing/reopening the app. Uninstalling or clearing app storage can remove local data; there is no in-app export/restore feature.

| Problem | What to do |
| --- | --- |
| Save is blocked | Check the highlighted fields and make sure a subject is available. |
| A task is missing | Clear search/filters and check both pending and completed records. |
| No priority task appears | Create a pending task or reopen a completed task. |
| The color is rejected | Use # and exactly six hexadecimal digits (0-9, A-F). |
| Changes cannot be saved/loaded | Retry. Keep an unsaved focus session until its save succeeds. |
| The app cannot build in Android Studio | Check Gradle Sync, the selected Gradle JDK, SDK Platform 37, and dependency-download access. |

The source includes externalized strings, labeled inputs, accessible action descriptions, large controls, and light/dark themes. TalkBack, large-font, rotation, and device-reboot behavior still need the device tests in [Testing](Study_Boost_Testing.md).

**Verification note:** This guide describes the updated implementation. Android compilation and device execution were not completed in the review environment.
