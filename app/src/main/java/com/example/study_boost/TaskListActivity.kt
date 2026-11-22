package com.example.study_boost

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup

class TaskListActivity : ScreenActivity() {
    private lateinit var adapter: TaskAdapter
    private var subjects = emptyList<Subject>()
    private var query = ""
    private var selectedSubject: Int? = null
    private var status: String? = TaskModel.PENDING
    private var generation = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen(R.layout.activity_task_list, withBottomNavigation = true)
        bindBottomNavigation(R.id.nav_tasks)
        query = savedInstanceState?.getString("query").orEmpty()
        selectedSubject = savedInstanceState?.getInt("subject", -1)?.takeIf { it > 0 }
        status = savedInstanceState?.getString("status") ?: TaskModel.PENDING
        adapter = TaskAdapter({ edit(it) }, { focus(it) }, { edit(it) }, { toggle(it) }, { remove(it) })
        findViewById<RecyclerView>(R.id.recyclerViewTasks).apply {
            layoutManager = LinearLayoutManager(this@TaskListActivity); adapter = this@TaskListActivity.adapter
        }
        findViewById<View>(R.id.btnAddTask).setOnClickListener {
            openScreen(Intent(this, AddEditTaskActivity::class.java))
        }
        findViewById<SearchView>(R.id.searchViewTasks).apply {
            setQuery(this@TaskListActivity.query, false)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(text: String?) = true
                override fun onQueryTextChange(text: String?): Boolean {
                    this@TaskListActivity.query = text.orEmpty()
                    load()
                    return true
                }
            })
        }
        findViewById<ChipGroup>(R.id.statusChips).apply {
            check(when (status) {
                TaskModel.PENDING -> R.id.chipPending
                TaskModel.COMPLETED -> R.id.chipCompleted
                else -> R.id.chipAll
            })
            setOnCheckedStateChangeListener { _, ids ->
                status = when {
                    ids.contains(R.id.chipPending) -> TaskModel.PENDING
                    ids.contains(R.id.chipCompleted) -> TaskModel.COMPLETED
                    else -> null
                }; load()
            }
        }
        findViewById<AutoCompleteTextView>(R.id.spinnerSubject).apply {
            setOnClickListener { showDropDown() }
            setOnItemClickListener { _, _, position, _ ->
                selectedSubject = subjects.getOrNull(position - 1)?.id
                load()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        database({ it.getSubjects() }) { data ->
            subjects = data
            if (subjects.none { it.id == selectedSubject }) selectedSubject = null
            findViewById<AutoCompleteTextView>(R.id.spinnerSubject).apply {
                setAdapter(ArrayAdapter(this@TaskListActivity, android.R.layout.simple_dropdown_item_1line,
                    listOf(getString(R.string.all_subjects)) + subjects.map { it.code + " - " + it.name })
                )
                val index = subjects.indexOfFirst { it.id == selectedSubject }
                setText(if (index >= 0) subjects[index].code + " - " + subjects[index].name
                else getString(R.string.all_subjects), false)
            }
            load()
        }
    }
    override fun onChildSaved(data: Intent?) { load() }
    private fun load() {
        val request = ++generation
        val text = query; val subject = selectedSubject; val state = status
        database({ it.getTasks(text, subject, state) }) { tasks ->
            if (request == generation) {
                adapter.submit(tasks)
                findViewById<TextView>(R.id.tvTaskCount).text = getString(R.string.task_count, tasks.size)
                findViewById<View>(R.id.layoutEmptyState).visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                findViewById<View>(R.id.recyclerViewTasks).visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
    private fun edit(task: TaskModel) = openScreen(
        Intent(this, AddEditTaskActivity::class.java).putExtra(Extras.TASK_ID, task.id))
    private fun toggle(task: TaskModel) {
        val next = if (task.status == TaskModel.PENDING) TaskModel.COMPLETED else TaskModel.PENDING
        database({ it.setTaskStatus(task.id, next) }) { toast(R.string.task_updated); load() }
    }
    private fun remove(task: TaskModel) {
        confirm(getString(R.string.delete_task_message, task.title)) {
            database({ it.deleteTask(task.id) }) { toast(R.string.task_deleted); load() }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", query); outState.putInt("subject", selectedSubject ?: -1)
        outState.putString("status", status); super.onSaveInstanceState(outState)
    }
}
