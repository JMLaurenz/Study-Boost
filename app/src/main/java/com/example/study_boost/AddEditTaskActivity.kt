package com.example.study_boost

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddEditTaskActivity : ScreenActivity() {
    private data class TimeOption(val label: String, val hours: Double)

    private var taskId: Int? = null
    private var original: TaskModel? = null
    private var subjects = emptyList<Subject>()
    private var selectedSubjectId: Int? = null
    private var selectedHours = 1.5
    private var selectedDate = ""
    private var initialized = false
    private var restored: Bundle? = null
    private lateinit var titleInput: EditText
    private lateinit var description: EditText
    private lateinit var due: AutoCompleteTextView
    private lateinit var hours: AutoCompleteTextView
    private lateinit var completed: CheckBox
    private lateinit var subject: AutoCompleteTextView
    private lateinit var save: Button

    private val timeOptions by lazy {
        listOf(
            TimeOption(getString(R.string.time_15_min), 0.25),
            TimeOption(getString(R.string.time_30_min), 0.5),
            TimeOption(getString(R.string.time_45_min), 0.75),
            TimeOption(getString(R.string.time_1_hour), 1.0),
            TimeOption(getString(R.string.time_1_30), 1.5),
            TimeOption(getString(R.string.time_2_hours), 2.0),
            TimeOption(getString(R.string.time_3_hours), 3.0)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen(R.layout.activity_task_edit)
        restored = savedInstanceState
        taskId = intent.getIntExtra(Extras.TASK_ID, -1).takeIf { it > 0 }
        findViewById<TextView>(R.id.tvHeading).setText(if (taskId == null) R.string.add_task else R.string.edit_task)
        titleInput = findViewById(R.id.etTaskTitle)
        description = findViewById(R.id.etDescription)
        due = findViewById(R.id.etDueDate)
        hours = findViewById(R.id.etHours)
        completed = findViewById(R.id.cbCompleted)
        subject = findViewById(R.id.spinnerSubject)
        save = findViewById(R.id.btnSaveTask)
        save.isEnabled = false

        subject.setOnClickListener { subject.showDropDown() }
        subject.setOnItemClickListener { _, _, position, _ -> selectedSubjectId = subjects.getOrNull(position)?.id }
        due.setOnClickListener { showDatePicker() }
        findViewById<TextInputLayout>(R.id.dueDateLayout).setEndIconOnClickListener { showDatePicker() }
        hours.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            timeOptions.map { it.label }))
        hours.setOnClickListener { hours.showDropDown() }
        hours.setOnItemClickListener { _, _, position, _ -> selectedHours = timeOptions[position].hours }
        findViewById<Button>(R.id.btnManageSubjects).setOnClickListener {
            openScreen(android.content.Intent(this, SubjectListActivity::class.java))
        }
        save.setOnClickListener { saveTask() }
        @Suppress("UNCHECKED_CAST")
        val restoredPicker = supportFragmentManager.findFragmentByTag("due_date_picker") as? MaterialDatePicker<Long>
        restoredPicker?.let(::configureDatePicker)
    }

    override fun onResume() {
        super.onResume()
        database({ Pair(it.getSubjects(), taskId?.let(it::getTask)) }) { (data, task) ->
            if (taskId != null && task == null) {
                toast(R.string.task_record_missing)
                finish()
                return@database
            }
            subjects = data
            subject.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
                subjects.map { it.code + " - " + it.name }))
            val wanted = selectedSubjectId
                ?: restored?.getInt("subject", -1)?.takeIf { it > 0 }
                ?: task?.subjectId
            selectedSubjectId = wanted
            val subjectIndex = subjects.indexOfFirst { it.id == wanted }
            subject.setText(if (subjectIndex >= 0) subjects[subjectIndex].code + " - " + subjects[subjectIndex].name else "", false)

            if (!initialized) {
                original = task
                titleInput.setText(restored?.getString("title") ?: task?.title.orEmpty())
                description.setText(restored?.getString("description") ?: task?.description.orEmpty())
                selectedDate = restored?.getString("due") ?: task?.dueDate.orEmpty()
                due.setText(displayDate(selectedDate), false)
                selectedHours = restored?.getDouble("hoursValue", Double.NaN)?.takeIf { !it.isNaN() }
                    ?: task?.hours ?: 1.5
                hours.setText(restored?.getString("hoursLabel") ?: timeLabel(selectedHours), false)
                completed.isChecked = restored?.getBoolean("completed") ?: (task?.status == TaskModel.COMPLETED)
                initialized = true
            }
            save.isEnabled = subjects.isNotEmpty()
            findViewById<android.view.View>(R.id.tvNeedSubject).visibility = if (subjects.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun timeLabel(value: Double): String = timeOptions.firstOrNull { it.hours == value }?.label
        ?: if (value >= 1.0) String.format(Locale.US, "%.1fh", value) else String.format(Locale.US, "%.2fh", value)

    private fun existingDateSelection(): Long? = if (DateRules.valid(selectedDate)) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(selectedDate)?.time
    } else null

    private fun displayDate(date: String): String {
        if (!DateRules.valid(date)) return ""
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(date) ?: return ""
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(parsed)
    }

    private fun showDatePicker() {
        if (supportFragmentManager.findFragmentByTag("due_date_picker") != null) return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.choose_due_date))
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setSelection(existingDateSelection() ?: DateRules.localTodayAsPickerUtc())
            .build()
        configureDatePicker(picker)
        picker.show(supportFragmentManager, "due_date_picker")
    }

    private fun configureDatePicker(picker: MaterialDatePicker<Long>) {
        picker.clearOnPositiveButtonClickListeners()
        picker.addOnPositiveButtonClickListener { selection ->
            selection ?: return@addOnPositiveButtonClickListener
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
            if (!initialized) restored = (restored ?: Bundle()).apply { putString("due", selectedDate) }
            due.setText(displayDate(selectedDate), false)
            due.error = null
        }
        picker.viewLifecycleOwnerLiveData.observe(picker) { owner ->
            if (owner != null) {
                picker.view?.findViewById<View>(com.google.android.material.R.id.mtrl_picker_header_toggle)?.visibility = View.GONE
            }
        }
    }

    private fun saveTask() {
        val title = titleInput.text.toString().trim()
        val date = selectedDate
        val text = description.text.toString()
        val state = if (completed.isChecked) TaskModel.COMPLETED else TaskModel.PENDING
        val selected = subjects.firstOrNull { it.id == selectedSubjectId }
        val errors = Validation.task(title, date, selectedHours, original?.dueDate)
        titleInput.error = if ("title" in errors) getString(R.string.required) else null
        subject.error = if (selected == null) getString(R.string.required) else null
        due.error = if ("date" in errors) getString(R.string.invalid_date) else null
        hours.error = if ("hours" in errors) getString(R.string.invalid_hours) else null
        if (errors.isNotEmpty() || selected == null) return
        save.isEnabled = false
        database({ it.saveTask(taskId, selected.id, title, text, date, selectedHours, state) }, { save.isEnabled = true }) {
            toast(R.string.task_saved)
            setResult(RESULT_OK, android.content.Intent().putExtra(Extras.TASK_ID, it.toInt()))
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("title", titleInput.text.toString())
        outState.putString("description", description.text.toString())
        outState.putString("due", selectedDate)
        outState.putString("hoursLabel", hours.text.toString())
        outState.putDouble("hoursValue", selectedHours)
        outState.putBoolean("completed", completed.isChecked)
        outState.putInt("subject", selectedSubjectId ?: -1)
        super.onSaveInstanceState(outState)
    }
}
