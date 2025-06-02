package com.example.maincontainerfor2048webmaster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.maincontainerfor2048webmaster.Game2048View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Optionally, set up interactions here if needed
        // val gameView = findViewById<Game2048View>(R.id.game_2048_view)
    }
}
