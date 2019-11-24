package de.ichmann.android.nonodroid

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        addButtonHandlers()
    }

    fun addButtonHandlers() {
        val optionsButton = findViewById<Button>(R.id.button2)
        val helpButton = findViewById<Button>(R.id.button3)
        val aboutButton = findViewById<Button>(R.id.button4)
        optionsButton.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent)
        }
        helpButton.setOnClickListener {
            val intent = Intent(applicationContext, HelpActivity::class.java)
            startActivity(intent)
        }
        aboutButton.setOnClickListener {
            val intent = Intent(applicationContext, AboutActivity::class.java)
            startActivity(intent)
        }
        /* With Kotlin Anko:
        button {
            onClick { startActivity<SecondActivity>() }
        }*/
    }

    fun openCourseChooser(view: View) {
        val intent = Intent(view.context, ChooseCourseActivity::class.java)
        startActivity(intent)
    }
}
