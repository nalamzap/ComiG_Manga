package dev.nalamzap.comig

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.nalamzap.comig.core.navigation.NavGraph
import dev.nalamzap.comig.core.theme.ComiGMangaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComiGMangaTheme {
                NavGraph()
            }
        }
    }
}
