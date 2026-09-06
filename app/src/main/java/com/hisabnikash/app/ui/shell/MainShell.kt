package com.hisabnikash.app.ui.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.ui.customers.CustomersTab
import com.hisabnikash.app.ui.home.HomeTab
import com.hisabnikash.app.ui.more.MoreTab
import com.hisabnikash.app.ui.orders.OrdersTab
import com.hisabnikash.app.ui.products.ProductsTab

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Home", Icons.Filled.Home),
    TabItem("Orders", Icons.Filled.Article),
    TabItem("Products", Icons.Filled.Inventory2),
    TabItem("Customers", Icons.Filled.Badge),
    TabItem("More", Icons.Filled.MoreHoriz)
)

@Composable
fun MainShell(container: AppContainer, navController: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeTab(container, navController)
                1 -> OrdersTab(container, navController)
                2 -> ProductsTab(container, navController)
                3 -> CustomersTab(container, navController)
                4 -> MoreTab(container, navController)
            }
        }
    }
}
