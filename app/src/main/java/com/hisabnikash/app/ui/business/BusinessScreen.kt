package com.hisabnikash.app.ui.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.InkFaint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BusinessSwitcherScreenRoute(container: AppContainer, navController: NavHostController) {
    val businesses by container.workspaceRepository.businesses.collectAsState(initial = emptyList())
    val active by container.prefs.activeBusinessId.collectAsState(initial = null)

    ScreenFrame("Businesses", onBack = { navController.popBackStack() }) {
        if (businesses.isEmpty()) {
            EmptyState(
                Icons.Filled.Business,
                "No businesses yet",
                "Create your first business to start recording commerce.",
                actionLabel = "Create Business",
                onAction = { navController.navigate(Routes.NEW_BUSINESS) }
            )
        } else {
            businesses.forEach { business ->
                ElevatedCard(
                    onClick = {
                        CoroutineScope(Dispatchers.Default).launch {
                            container.workspaceRepository.switchBusiness(business.id)
                            navController.popBackStack(Routes.MAIN, inclusive = false)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Business,
                            contentDescription = null,
                            tint = if (active == business.id) BrandGreen else InkFaint
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(business.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                business.category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        if (active == business.id) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Active",
                                tint = BrandGreen
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { navController.navigate(Routes.NEW_BUSINESS) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("New business")
        }
    }
}

@Composable
fun NewBusinessScreenRoute(container: AppContainer, navController: NavHostController) {
    com.hisabnikash.app.ui.onboarding.OnboardingScreen(container, navController, isAdditional = true)
}
