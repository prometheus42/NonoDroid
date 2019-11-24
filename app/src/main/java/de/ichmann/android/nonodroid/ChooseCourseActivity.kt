package de.ichmann.android.nonodroid

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.support.constraint.ConstraintLayout
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ChooseCourseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_course)

        val chooseCourseLayout = findViewById<LinearLayout>(R.id.chooseCourseLayout)

        // get all files from folder nonograms in assets
        val listFiles = assets.list("nonograms")
        listFiles.forEach {
            val button = Button(this)
            button.setPadding(10, 10, 10, 10)
            val courseName = it.toString()
            button.text = courseName
            button.setOnClickListener(View.OnClickListener {
                val intent = Intent(applicationContext, ChooseNonogramActivity::class.java)
                intent.putExtra("course_name", courseName)
                startActivity(intent)
            })
            //button.layoutParams = LinearLayout.LayoutParams()
            //button.setBackgroundColor(Color.GREEN)
            //button.setTextColor(Color.RED)
            chooseCourseLayout.addView(button);
        }
    }
}
