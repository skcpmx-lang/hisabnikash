package com.hisabnikash.app

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.hisabnikash.app.ui.nav.HisabNavGraph
import com.hisabnikash.app.ui.theme.HisabNikashTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is light-only by design. Forcing light system bars (dark
        // icons on transparent bars) keeps the clock and status icons readable
        // even when the device itself is in dark mode — otherwise
        // enableEdgeToEdge() would pick light(white) icons for a light app.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        val container = (application as HisabNikashApp).container
        setContent {
            HisabNikashTheme {
                HisabNavGraph(container)
            }
        }
    }
}
