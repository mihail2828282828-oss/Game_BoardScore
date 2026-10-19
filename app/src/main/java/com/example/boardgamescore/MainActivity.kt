package com.example.boardgamescore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storage = Storage(applicationContext)

        setContent {
            ScoreApp(storage)
        }
    }
}
