package de.ichmann.android.nonodroid

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_choose_nonogram.*

class ChooseNonogramActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_nonogram)

        val courseName = intent.getStringExtra("course_name")

        // get all files from folder nonograms in assets
        val listFiles = assets.list("nonograms/" + courseName)
        listFiles.forEach {
            val button = Button(this)
            //button.setPadding(10, 10, 10, 10)
            val nonogramName = it.toString()
            button.text = nonogramName
            button.setOnClickListener(View.OnClickListener {
                val intent = Intent(applicationContext, ViewNonogramActivity::class.java)
                intent.putExtra("course_name", courseName)
                intent.putExtra("nonogram_name", nonogramName)
                startActivity(intent)
            })
            chooseNonogramLayout.addView(button);
        }
    }
}
