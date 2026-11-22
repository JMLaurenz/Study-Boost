package com.example.study_boost

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import com.google.android.material.textfield.TextInputLayout

class AddEditSubjectActivity : ScreenActivity() {
    private data class ColorOption(val label: String, val hex: String)

    private class ColorAdapter(private val context: Context, private val options: List<ColorOption>) :
        ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, options.map { it.label }) {
        private fun row(position: Int, recycled: View?): TextView {
            val density = context.resources.displayMetrics.density
            val view = (recycled as? TextView) ?: TextView(context)
            view.text = options[position].label
            view.setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
            val swatch = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(options[position].hex.toColorInt())
                setSize((20 * density).toInt(), (20 * density).toInt())
            }
            view.setCompoundDrawables(swatch, null, null, null)
            view.compoundDrawablePadding = (10 * density).toInt()
            return view
        }
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = row(position, convertView)
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = row(position, convertView)
    }

    private var subjectId: Int? = null
    private var selectedColor = "#4D97E8"
    private lateinit var code: EditText
    private lateinit var name: EditText
    private lateinit var color: AutoCompleteTextView
    private lateinit var colorLayout: TextInputLayout
    private lateinit var difficultyGroup: RadioGroup
    private lateinit var save: Button

    private val colorOptions by lazy {
        listOf(
            ColorOption(getString(R.string.color_ocean_blue), "#4D97E8"),
            ColorOption(getString(R.string.color_sage_green), "#9BC99D"),
            ColorOption(getString(R.string.color_soft_lavender), "#8D5ED3"),
            ColorOption(getString(R.string.color_warm_amber), "#D58A0B"),
            ColorOption(getString(R.string.color_dusty_rose), "#E598A9"),
            ColorOption(getString(R.string.color_slate_gray), "#A7B0BA")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screen(R.layout.activity_subject_add)
        subjectId = intent.getIntExtra(Extras.SUBJECT_ID, -1).takeIf { it > 0 }
        findViewById<TextView>(R.id.tvHeading).setText(if (subjectId == null) R.string.add_subject else R.string.edit_subject)
        code = findViewById(R.id.etSubjectCode)
        name = findViewById(R.id.etSubjectName)
        color = findViewById(R.id.actColor)
        colorLayout = findViewById(R.id.colorTagLayout)
        colorLayout.setStartIconTintList(null)
        difficultyGroup = findViewById(R.id.difficultyGroup)
        save = findViewById(R.id.btnSaveSubject)
        save.isEnabled = false

        color.setAdapter(ColorAdapter(this, colorOptions))
        color.setOnClickListener { color.showDropDown() }
        color.setOnItemClickListener { _, _, position, _ ->
            selectedColor = colorOptions[position].hex
            updateColorSwatch()
        }

        if (subjectId == null) bind(null, savedInstanceState) else database({ it.getSubject(requireNotNull(subjectId)) }) {
            if (it == null) {
                toast(R.string.subject_missing)
                finish()
            } else bind(it, savedInstanceState)
        }
        save.setOnClickListener { saveSubject() }
    }

    private fun bind(subject: Subject?, state: Bundle?) {
        code.setText(state?.getString("code") ?: subject?.code.orEmpty())
        name.setText(state?.getString("name") ?: subject?.name.orEmpty())
        selectedColor = state?.getString("colorValue")
            ?: subject?.colorHex?.uppercase()
            ?: colorOptions.first().hex
        val option = colorOptions.firstOrNull { it.hex.equals(selectedColor, true) }
        color.setText(option?.label ?: getString(R.string.color_custom, selectedColor), false)
        updateColorSwatch()
        val difficulty = state?.getInt("difficulty", -1)?.takeIf { it in 1..5 }
            ?: subject?.difficulty ?: 3
        difficultyGroup.check(difficultyButtonId(difficulty))
        save.isEnabled = true
    }

    private fun updateColorSwatch() {
        val density = resources.displayMetrics.density
        val swatch = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 5 * density
            setColor(selectedColor.toColorInt())
            setSize((24 * density).toInt(), (24 * density).toInt())
        }
        colorLayout.setStartIconDrawable(swatch)
    }

    private fun difficultyButtonId(value: Int) = when (value) {
        1 -> R.id.difficulty1
        2 -> R.id.difficulty2
        3 -> R.id.difficulty3
        4 -> R.id.difficulty4
        else -> R.id.difficulty5
    }

    private fun selectedDifficulty(): Int = when (difficultyGroup.checkedRadioButtonId) {
        R.id.difficulty1 -> 1
        R.id.difficulty2 -> 2
        R.id.difficulty3 -> 3
        R.id.difficulty4 -> 4
        R.id.difficulty5 -> 5
        else -> 0
    }

    private fun saveSubject() {
        val c = code.text.toString().trim()
        val n = name.text.toString().trim()
        val d = selectedDifficulty()
        val errors = Validation.subject(c, n, selectedColor, d)
        code.error = if ("code" in errors) getString(R.string.required) else null
        name.error = if ("name" in errors) getString(R.string.required) else null
        color.error = if ("color" in errors) getString(R.string.invalid_color) else null
        if (errors.isEmpty()) {
            save.isEnabled = false
            database({ it.saveSubject(subjectId, c, n, selectedColor, d) }, { save.isEnabled = true }) {
                toast(R.string.subject_saved)
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("code", code.text.toString())
        outState.putString("name", name.text.toString())
        outState.putString("colorValue", selectedColor)
        outState.putInt("difficulty", selectedDifficulty())
        super.onSaveInstanceState(outState)
    }
}
