package com.hisabnikash.app.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.Error
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.Warning
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUi(
    val loading: Boolean = true,
    val filter: String = "ALL",
    val items: List<com.hisabnikash.app.data.db.AppNotificationEntity> = emptyList()
)

class NotificationsViewModel(container: AppContainer) : ViewModel() {

    private val repo = container.notificationRepository
    private val filterFlow = MutableStateFlow("ALL")

    val state: StateFlow<NotificationsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(NotificationsUi().copy(loading = false))
            else filterFlow.flatMapLatest { filter ->
                repo.observeNotifications(id, filter).map { list ->
                    NotificationsUi(loading = false, filter = filter, items = list)
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            NotificationsUi()
        )

    fun setFilter(filter: String) { filterFlow.value = filter }

    fun markRead(container: AppContainer, id: Long) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            container.prefs.activeBusinessId.first()?.let { biz ->
                container.notificationRepository.markRead(biz, id, true)
            }
        }
    }

    fun markAllRead(container: AppContainer) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            container.prefs.activeBusinessId.first()?.let { biz ->
                container.notificationRepository.markAllRead(biz)
            }
        }
    }
}

@Composable
fun NotificationsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { NotificationsViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame(
        "Notifications",
        onBack = { navController.popBackStack() },
        actions = {
            Button(onClick = { vm.markAllRead(container) }) { Text("Mark all read") }
        }
    ) {
        FilterChips(listOf("ALL", "CRITICAL", "IMPORTANT", "INSIGHT"), state.filter, { vm.setFilter(it) })
        if (state.items.isEmpty()) {
            EmptyState(
                Icons.Filled.Notifications,
                "No notifications",
                "Low stock, pending COD, payables due and insights appear here automatically."
            )
        } else {
            state.items.forEach { notification ->
                Surface(
                    onClick = {
                        vm.markRead(container, notification.id)
                        notificationTarget(notification.refType, notification.refId)?.let {
                            navController.navigate(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Text(
                                notification.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (!notification.read) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = "Unread",
                                    tint = BrandGreen,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        Text(notification.body, style = MaterialTheme.typography.bodySmall, color = InkFaint)
                        Text(
                            formatDateTime(notification.createdAt) + " • " + notification.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (notification.category) {
                                "CRITICAL" -> Error
                                "IMPORTANT" -> Warning
                                else -> InkFaint
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Resolves a notification to a registered navigation destination.
 *
 * Returns null when the referenced record has no detail route yet (for
 * example payment or settlement records), in which case the row still marks
 * itself read but does not navigate. Only routes registered in
 * [com.hisabnikash.app.ui.nav.HisabNavGraph] are ever returned, so a tap can
 * never hit an unregistered destination.
 */
private fun notificationTarget(refType: String?, refId: Long?): String? = when (refType) {
    "ORDER" -> refId?.let { "order/$it" }
    "PRODUCT" -> refId?.let { "product/$it" }
    "INVOICE" -> refId?.let { "invoice/$it" }
    "RETURN" -> Routes.RETURNS
    "EXCHANGE" -> Routes.EXCHANGES
    "REFUND" -> Routes.REFUNDS
    "SETTLEMENT", "SETTLEMENT_FEE" -> Routes.SETTLEMENTS
    "PAYMENT" -> Routes.TRANSACTIONS
    else -> null
}
