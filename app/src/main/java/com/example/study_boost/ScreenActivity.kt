package com.example.study_boost

import android.content.Intent
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomnavigation.BottomNavigationView

object Extras {
    const val TASK_ID = "EXTRA_TASK_ID"
    const val TASK_TITLE = "EXTRA_TASK_TITLE"
    const val SUBJECT_ID = "EXTRA_SUBJECT_ID"
    const val NAV_DIRECTION = "EXTRA_NAV_DIRECTION"
}

abstract class ScreenActivity : AppCompatActivity() {
    private var navigationSelection: Int? = null
    private var hasBottomNavigation = false
    private val childResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) onChildSaved(result.data)
    }
    protected fun openScreen(intent: Intent) = childResult.launch(intent)
    protected open fun onChildSaved(data: Intent?) = Unit

    override fun onPostResume() {
        super.onPostResume()
        navigationSelection?.let { selected ->
            findViewById<BottomNavigationView>(R.id.bottomNavigation)?.let { navigation ->
                if (navigation.selectedItemId != selected) navigation.selectedItemId = selected
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (hasBottomNavigation) animateNavigationContent(intent)
    }

    protected fun screen(layout: Int, withBottomNavigation: Boolean = false) {
        hasBottomNavigation = withBottomNavigation
        enableEdgeToEdge()
        if (withBottomNavigation) {
            setContentView(R.layout.activity_screen_shell)
            layoutInflater.inflate(layout, findViewById(R.id.screenContent), true)
        } else {
            setContentView(layout)
        }
        val root = findViewById<View>(R.id.screenRoot)
        val left = root.paddingLeft; val top = root.paddingTop
        val right = root.paddingRight; val bottom = root.paddingBottom
        val navigation = if (withBottomNavigation) findViewById<BottomNavigationView>(R.id.bottomNavigation) else null
        val navigationLeft = navigation?.paddingLeft ?: 0
        val navigationTop = navigation?.paddingTop ?: 0
        val navigationRight = navigation?.paddingRight ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            if (withBottomNavigation) {
                v.setPadding(left + systemBars.left, top + systemBars.top, right + systemBars.right, bottom)
                navigation?.setPadding(navigationLeft, navigationTop, navigationRight, maxOf(systemBars.bottom, ime.bottom))
            } else {
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
        findViewById<View>(R.id.btnBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        if (withBottomNavigation) animateNavigationContent(intent)
    }

    private fun animateNavigationContent(intent: Intent) {
        val content = findViewById<View>(R.id.screenContent) ?: return
        val direction = intent.getIntExtra(Extras.NAV_DIRECTION, 0)
        content.animate().cancel()
        if (direction == 0) {
            content.translationX = 0f
            content.alpha = 1f
            return
        }
        val distance = content.resources.displayMetrics.widthPixels.toFloat().coerceAtLeast(1f)
        content.translationX = if (direction > 0) distance else -distance
        content.alpha = 0.98f
        content.animate().translationX(0f).alpha(1f).setDuration(220L)
            .setInterpolator(DecelerateInterpolator()).start()
    }
    protected fun <T> database(work: (DatabaseHelper) -> T, onError: () -> Unit = {}, done: (T) -> Unit) {
        StudyStore.executor.execute {
            try {
                val value = work(StudyStore.database(applicationContext))
                runOnUiThread { if (!isFinishing && !isDestroyed) done(value) }
            } catch (e: Exception) {
                Log.e("StudyBoost", "Database operation failed", e)
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) { toast(R.string.database_error); onError() }
                }
            }
        }
    }
    protected fun bindBottomNavigation(selected: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNavigation)?.apply {
            navigationSelection = selected
            setOnItemSelectedListener { item ->
                if (item.itemId != selected) {
                    val destination = when (item.itemId) {
                        R.id.nav_home -> MainActivity::class.java
                        R.id.nav_subjects -> SubjectListActivity::class.java
                        R.id.nav_tasks -> TaskListActivity::class.java
                        else -> null
                    }
                    destination?.let {
                        val currentIndex = navigationIndex(selected)
                        val targetIndex = navigationIndex(item.itemId)
                        startActivity(Intent(this@ScreenActivity, it)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            .putExtra(Extras.NAV_DIRECTION, if (targetIndex > currentIndex) 1 else -1))
                        overridePendingTransition(0, 0)
                    }
                }
                true
            }
            selectedItemId = selected
            post { if (selectedItemId != selected) selectedItemId = selected }
        }
    }

    private fun navigationIndex(itemId: Int) = when (itemId) {
        R.id.nav_home -> 0
        R.id.nav_subjects -> 1
        R.id.nav_tasks -> 2
        else -> 0
    }
    protected fun toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    protected fun confirm(message: String, action: () -> Unit) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.confirm_delete).setMessage(message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> action() }.show()
    }
    protected fun focus(task: TaskModel) = openScreen(
        Intent(this, StudyTimerActivity::class.java)
            .putExtra(Extras.TASK_ID, task.id).putExtra(Extras.TASK_TITLE, task.title))
}
