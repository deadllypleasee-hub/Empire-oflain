package ru.empire.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import ru.empire.game.ui.EmpireApp

class MainActivity : ComponentActivity() {
    private val game: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { EmpireApp(game) }
    }
    override fun onStart() { super.onStart(); game.foreground() }
    override fun onStop() { game.background(); super.onStop() }
}
