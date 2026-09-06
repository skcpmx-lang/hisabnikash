package com.hisabnikash.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.hisabnikash.app.ui.nav.HisabNavGraph
import com.hisabnikash.app.ui.theme.HisabNikashTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HisabNikashApp).container
        setContent {
            HisabNikashTheme {
                HisabNavGraph(container)
            }
        }
    }
}
