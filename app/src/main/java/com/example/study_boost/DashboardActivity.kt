package com.example.study_boost

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class DashboardActivity : ScreenActivity() {
    private lateinit var adapter: TaskAdapter
    private var topTask: TaskModel? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { load(); handler.postDelayed(this, 60_000) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen(R.layout.activity_dashboard, withBottomNavigation = true)
        bindBottomNavigation(R.id.nav_home)
        adapter = TaskAdapter({ focus(it) }, { focus(it) }, { edit(it) }, { changeStatus(it) }, { remove(it) })
        findViewById<RecyclerView>(R.id.recyclerViewTasks).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = this@DashboardActivity.adapter
            isNestedScrollingEnabled = false
        }
        findViewById<View>(R.id.btnQuickFocus).setOnClickListener {
            val saved = getSharedPreferences(StudyTimerActivity.PREFS, MODE_PRIVATE)
            if (saved.contains("token")) openScreen(Intent(this, StudyTimerActivity::class.java))
            else topTask?.let(::focus) ?: toast(R.string.no_pending_tasks)
        }
        findViewById<View>(R.id.btnPriorityInfo).setOnLongClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.priority_info_title)
                .setMessage(R.string.priority_formula)
                .setPositiveButton(R.string.ok, null)
                .show()
            true
        }
    }
    override fun onResume() { super.onResume(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
    override fun onChildSaved(data: Intent?) { load() }
    private fun load() {
        database({ db ->
            val all = db.getTasks()
            val now = System.currentTimeMillis()
            val pending = all.filter { it.status == TaskModel.PENDING }
                .sortedWith(compareByDescending<TaskModel> { it.urgency(now) }.thenBy { it.dueDate }.thenBy { it.id })
            Triple(pending, all.count { it.status == TaskModel.COMPLETED }, db.getTotalStudySeconds())
        }) { (pending, completed, seconds) ->
            topTask = pending.firstOrNull()
            adapter.submit(pending.take(5))
            findViewById<TextView>(R.id.tvPendingCount).text = pending.size.toString()
            findViewById<TextView>(R.id.tvCompletedCount).text = completed.toString()
            findViewById<TextView>(R.id.tvHoursCount).text = getString(R.string.hours_short, seconds / 3600.0)
            findViewById<View>(R.id.layoutEmptyState).visibility = if (pending.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    private fun edit(task: TaskModel) = openScreen(
        Intent(this, AddEditTaskActivity::class.java).putExtra(Extras.TASK_ID, task.id))
    private fun changeStatus(task: TaskModel) {
        database({ it.setTaskStatus(task.id, TaskModel.COMPLETED) }) { toast(R.string.task_updated); load() }
    }
    private fun remove(task: TaskModel) {
        confirm(getString(R.string.delete_task_message, task.title)) {
            database({ it.deleteTask(task.id) }) { toast(R.string.task_deleted); load() }
        }
    }
}
