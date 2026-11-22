package com.example.study_boost

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SubjectListActivity : ScreenActivity() {
    private lateinit var adapter: SubjectAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen(R.layout.activity_subject_list, withBottomNavigation = true)
        bindBottomNavigation(R.id.nav_subjects)
        adapter = SubjectAdapter({ subject ->
            openScreen(Intent(this, AddEditSubjectActivity::class.java).putExtra(Extras.SUBJECT_ID, subject.id))
        }, { subject ->
            confirm(getString(R.string.delete_subject_message, subject.code, subject.name)) {
                database({ it.deleteSubject(subject.id) }) { toast(R.string.subject_deleted); load() }
            }
        })
        findViewById<RecyclerView>(R.id.recyclerViewSubjects).apply {
            layoutManager = LinearLayoutManager(this@SubjectListActivity); adapter = this@SubjectListActivity.adapter
        }
        findViewById<View>(R.id.btnAddSubject).setOnClickListener {
            openScreen(Intent(this, AddEditSubjectActivity::class.java))
        }
    }
    override fun onResume() { super.onResume(); load() }
    override fun onChildSaved(data: Intent?) { load() }
    private fun load() {
        database({ it.getSubjects() }) {
            adapter.submit(it)
            findViewById<View>(R.id.layoutEmptyState).visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
private class SubjectAdapter(private val edit: (Subject) -> Unit, private val delete: (Subject) -> Unit) :
    RecyclerView.Adapter<SubjectAdapter.Holder>() {
    private var items = emptyList<Subject>()
    fun submit(data: List<Subject>) { items = data; notifyDataSetChanged() }
    class Holder(val view: View) : RecyclerView.ViewHolder(view)
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val subject = items[position]; val v = holder.view
        v.findViewById<View>(R.id.subjectColor).backgroundTintList = ColorStateList.valueOf(Color.parseColor(subject.colorHex))
        v.findViewById<TextView>(R.id.tvSubjectCode).text = subject.code
        v.findViewById<TextView>(R.id.tvSubjectName).text = subject.name
        v.findViewById<TextView>(R.id.tvDifficulty).text = v.context.getString(
            R.string.difficulty_rating, subject.difficulty)
        v.findViewById<View>(R.id.btnEdit).apply {
            contentDescription = v.context.getString(R.string.edit_subject_named, subject.name)
            setOnClickListener { edit(subject) }
        }
        v.findViewById<View>(R.id.btnDelete).apply {
            contentDescription = v.context.getString(R.string.delete_subject_named, subject.name)
            setOnClickListener { delete(subject) }
        }
    }
}
