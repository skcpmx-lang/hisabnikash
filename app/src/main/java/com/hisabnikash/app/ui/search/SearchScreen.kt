package com.hisabnikash.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.repo.SearchResult
import com.hisabnikash.app.data.repo.SearchSection
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchUi(
    val query: String = "",
    val sections: List<SearchSection> = emptyList(),
    val searching: Boolean = false
)

class SearchViewModel(container: AppContainer) : ViewModel() {

    private val searchRepo = container.searchRepository
    private val queryFlow = MutableStateFlow("")

    val state: StateFlow<SearchUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(SearchUi())
            else queryFlow
                .debounce(250)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(SearchUi(query = query))
                    else kotlinx.coroutines.flow.flow {
                        emit(SearchUi(query = query, searching = true))
                        emit(
                            SearchUi(
                                query = query,
                                searching = false,
                                sections = runCatching { searchRepo.search(id, query) }
                                    .getOrDefault(emptyList())
                            )
                        )
                    }
                }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            SearchUi()
        )

    fun setQuery(query: String) { queryFlow.value = query }
}

@Composable
fun SearchScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { SearchViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Search", onBack = { navController.popBackStack() }) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { vm.setQuery(it) },
            placeholder = { Text("orders, products, customers, suppliers, invoices…") },
            leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        )
        if (state.query.isBlank()) {
            Text(
                "Try: \"today\", \"low stock\", \"pending cod\", \"unpaid\", an order number, product name, customer phone…",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else if (state.searching) {
            Text("Searching…", color = InkFaint, modifier = Modifier.padding(16.dp))
        } else if (state.sections.isEmpty()) {
            EmptyState(
                Icons.Filled.Search,
                "No results",
                "Nothing matched \"${state.query}\". Try a different term."
            )
        } else {
            LazyColumn {
                state.sections.forEach { section ->
                    item(key = section.title) {
                        SectionHeader(section.title)
                    }
                    items(section.items, key = { it.key }) { result ->
                        SearchResultRow(result) { navigateTo(navController, result) }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
    androidx.compose.material3.ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = MaterialTheme.shapes.small
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(result.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                result.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = InkFaint, maxLines = 1)
                }
            }
            result.trailing?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun navigateTo(navController: NavHostController, result: SearchResult) {
    when {
        result.route.startsWith("order/") ->
            navController.navigate("order/${result.routeArg ?: result.route.removePrefix("order/")}")
        result.route.startsWith("product/") ->
            navController.navigate("product/${result.routeArg ?: result.route.removePrefix("product/")}")
        result.route.startsWith("customer/") ->
            navController.navigate("customer/${result.routeArg ?: result.route.removePrefix("customer/")}")
        result.route.startsWith("supplier/") ->
            navController.navigate("supplier/${result.routeArg ?: result.route.removePrefix("supplier/")}")
        result.route.startsWith("invoice/") ->
            navController.navigate("invoice/${result.routeArg ?: result.route.removePrefix("invoice/")}")
        result.route == "receivables" -> navController.navigate("receivables")
        result.route == "expenses" -> navController.navigate("expenses")
        result.route == "transactions" -> navController.navigate("transactions")
        result.route == "receipts" -> navController.navigate("receipts")
        else -> Unit
    }
}
