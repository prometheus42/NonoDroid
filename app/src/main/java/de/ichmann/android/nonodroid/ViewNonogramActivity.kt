package de.ichmann.android.nonodroid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_view_nonogram.*


class ViewNonogramActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_nonogram)

        val courseName = intent.getStringExtra("course_name")
        val nonogramName = intent.getStringExtra("nonogram_name")

        val nonogramPath =  "nonograms/${courseName}/${nonogramName}"
        val nonogramFile = assets.open(nonogramPath)
        nonogramView.setNonogram(nonogramFile)
    }

    override fun onDestroy() {
        super.onDestroy()
        nonogramView.stop()
    }
}
