package com.quizbowl.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quizbowl.app.navigation.NavGraph
import com.quizbowl.app.ui.theme.QuizBowlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizBowlTheme {
                NavGraph()
            }
        }
    }
}
