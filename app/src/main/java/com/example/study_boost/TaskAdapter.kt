package com.example.study_boost

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskAdapter(private val open: (TaskModel) -> Unit, private val focus: (TaskModel) -> Unit,
                  private val edit: (TaskModel) -> Unit, private val toggle: (TaskModel) -> Unit,
                  private val delete: (TaskModel) -> Unit) : RecyclerView.Adapter<TaskAdapter.Holder>() {
    private var tasks = emptyList<TaskModel>()
    fun submit(data: List<TaskModel>) { tasks = data; notifyDataSetChanged() }
    class Holder(val view: View) : RecyclerView.ViewHolder(view)
    override fun getItemCount() = tasks.size
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val task = tasks[position]; val v = holder.view; val c = v.context
        val color = runCatching { Color.parseColor(task.colorHex) }.getOrDefault(Color.rgb(77, 151, 232))
        v.findViewById<View>(R.id.subjectColor).backgroundTintList = ColorStateList.valueOf(color)
        v.findViewById<TextView>(R.id.tvTaskTitle).apply {
            text = task.title; contentDescription = c.getString(R.string.open_task, task.title)
            setOnClickListener { open(task) }
        }
        v.findViewById<TextView>(R.id.tvTaskSubject).text = c.getString(R.string.task_subject_label,
            task.subjectCode, task.subjectName)
        v.findViewById<TextView>(R.id.tvDueBadge).text = dueLabel(c, task.dueDate)
        v.findViewById<TextView>(R.id.tvTaskMeta).text = c.getString(R.string.task_meta,
            task.hours, task.difficulty, task.studiedSeconds / 60.0)
        v.findViewById<TextView>(R.id.tvTaskPriority).text = c.getString(R.string.task_priority,
            task.urgency(), task.studiedSeconds / 60.0)
        v.findViewById<Button>(R.id.btnFocus).apply {
            isEnabled = task.status == TaskModel.PENDING
            contentDescription = c.getString(R.string.focus_task, task.title); setOnClickListener { focus(task) }
        }
        v.findViewById<Button>(R.id.btnEdit).apply {
            contentDescription = c.getString(R.string.edit_task_named, task.title); setOnClickListener { edit(task) }
        }
        v.findViewById<Button>(R.id.btnToggle).apply {
            setText(if (task.status == TaskModel.PENDING) R.string.mark_complete else R.string.mark_pending)
            contentDescription = c.getString(R.string.change_status_named, task.title); setOnClickListener { toggle(task) }
        }
        v.findViewById<Button>(R.id.btnDelete).apply {
            contentDescription = c.getString(R.string.delete_task_named, task.title); setOnClickListener { delete(task) }
        }
    }

    private fun dueLabel(context: android.content.Context, date: String): String {
        val today = DateRules.today()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
            .let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) }
        return when {
            date < today -> context.getString(R.string.overdue)
            date == today -> context.getString(R.string.due_today)
            date == tomorrow -> context.getString(R.string.due_tomorrow)
            else -> runCatching {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
                context.getString(R.string.due_on, SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed ?: Date()))
            }.getOrDefault(context.getString(R.string.due_on, date))
        }
    }
}
