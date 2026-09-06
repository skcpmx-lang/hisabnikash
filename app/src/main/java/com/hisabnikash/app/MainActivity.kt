package com.hisabnikash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hisabnikash.app.ui.nav.HisabNavGraph
import com.hisabnikash.app.ui.theme.HisabNikashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisabNikashTheme {
                HisabNavGraph()
            }
        }
    }
}
