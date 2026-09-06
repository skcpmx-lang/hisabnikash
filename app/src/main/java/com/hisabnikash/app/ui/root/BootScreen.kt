package com.hisabnikash.app.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.ui.components.BrandMark
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreenDeep
import com.hisabnikash.app.ui.theme.InkFaint

/**
 * Decides where the app starts. First launch goes to onboarding; returning
 * users with an active business go straight to the main shell. If a local PIN
 * or biometric lock has been set the security screen gates navigation.
 */
@Composable
fun BootScreen(container: AppContainer, navController: NavHostController) {
    val onboardingDone by container.prefs.onboardingDone.collectAsState(initial = false)
    val activeBusiness by container.prefs.activeBusinessId.collectAsState(initial = null)

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            BrandMark(96.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "HisabNikash",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = BrandGreenDeep
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your Complete Commerce OS",
                style = MaterialTheme.typography.bodyLarge,
                color = InkFaint,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            if (!onboardingDone || activeBusiness == null) {
                Button(onClick = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.BOOT) { inclusive = true }
                    }
                }) { Text("Get started") }
            } else {
                LaunchedEffect(Unit) {
                    val security = container.securityRepository
                    val secured = security.isPinSet() || security.isBiometricEnabled()
                    navController.navigate(if (secured) Routes.SECURITY else Routes.MAIN) {
                        popUpTo(Routes.BOOT) { inclusive = true }
                    }
                }
                Text(
                    "Loading your workspace…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkFaint
                )
            }
        }
    }
}
