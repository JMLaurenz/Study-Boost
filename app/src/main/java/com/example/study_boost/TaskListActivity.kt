package com.example.study_boost

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TaskListActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout

    // Dummy list for now so you can test search filtering
    private val taskList = listOf(
        TaskModel(1, "Data Structures Lab 1", "Computer Science", "PENDING"),
        TaskModel(2, "Database Schema Design", "Information Management", "PENDING"),
        TaskModel(3, "Calculus Problem Set", "Mathematics", "COMPLETED")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        // Bind UI components from activity_task_list.xml
        searchView = findViewById(R.id.searchViewTasks)
        recyclerView = findViewById(R.id.recyclerViewTasks)
        emptyStateLayout = findViewById(R.id.layoutEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initial load
        updateTaskList(taskList)

        // TC07: Real-time search filtering listener[cite: 3, 4]
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterTasks(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTasks(newText)
                return true
            }
        })
    }

    private fun filterTasks(query: String?) {
        val filteredList = taskList.filter { task ->
            task.title.contains(query.orEmpty(), ignoreCase = true) ||
                    task.subjectName.contains(query.orEmpty(), ignoreCase = true)
        }
        updateTaskList(filteredList)
    }

    private fun updateTaskList(data: List<TaskModel>) {
        // TC08: Toggle empty-state layout visibility if list is empty[cite: 3, 4]
        if (data.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            // TODO: Bind your RecyclerView adapter here when ready
        }
    }
}