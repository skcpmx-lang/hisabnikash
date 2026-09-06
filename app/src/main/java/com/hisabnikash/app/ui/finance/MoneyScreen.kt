package com.hisabnikash.app.ui.finance

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.db.AccountEntity
import com.hisabnikash.app.data.db.AccountTransactionEntity
import com.hisabnikash.app.data.db.ExpenseEntity
import com.hisabnikash.app.data.db.TransferEntity
import com.hisabnikash.app.domain.model.formatDateTime
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.domain.model.parseMoneyInput
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.FilterChips
import com.hisabnikash.app.ui.components.LabelValueRow
import com.hisabnikash.app.ui.components.MetricCard
import com.hisabnikash.app.ui.components.MoneyField
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.Error
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.Warning
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun accountTypeLabel(type: String): String = when (type) {
    "CASH" -> "Cash"
    "BANK" -> "Bank"
    "BKASH" -> "bKash"
    "NAGAD" -> "Nagad"
    "ROCKET" -> "Rocket"
    else -> "Custom"
}

// ---------------------------------------------------------------------------
// Accounts overview
// ---------------------------------------------------------------------------

data class AccountsUi(
    val loading: Boolean = true,
    val balances: List<com.hisabnikash.app.data.db.AccountBalance> = emptyList(),
    val total: Long = 0,
    val accounts: List<AccountEntity> = emptyList()
)

class AccountsViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository

    val state: StateFlow<AccountsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(AccountsUi().copy(loading = false))
            else combine(
                finance.observeBalances(id),
                finance.observeAccounts(id)
            ) { balances, accounts ->
                AccountsUi(
                    loading = false,
                    balances = balances,
                    accounts = accounts,
                    total = balances.sumOf { it.balanceMinor }
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            AccountsUi()
        )
}

@Composable
fun AccountsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { AccountsViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Money", onBack = { navController.popBackStack() }) {
        SectionHeader("Total balance")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    formatMoney(state.total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${state.accounts.size} accounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint
                )
            }
        }
        SectionHeader("Accounts")
        if (state.balances.isEmpty()) {
            EmptyState(
                Icons.Filled.AccountBalanceWallet,
                "No accounts",
                "Add a cash, bank or mobile-money account to track balances.",
                actionLabel = "Add Account",
                onAction = { navController.navigate(Routes.NEW_ACCOUNT) }
            )
        } else {
            state.balances.forEach { balance ->
                Surface(
                    onClick = { navController.navigate(Routes.account(balance.account.id)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(balance.account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                accountTypeLabel(balance.account.type),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        Text(
                            formatMoney(balance.balanceMinor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (balance.balanceMinor >= 0) BrandGreen else Error
                        )
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.NEW_ACCOUNT) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add account")
        }
        SectionHeader("Quick money actions")
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedButton(
                onClick = { navController.navigate(Routes.TRANSFER) },
                modifier = Modifier.weight(1f)
            ) { Text("Transfer") }
            androidx.compose.material3.OutlinedButton(
                onClick = { navController.navigate(Routes.EXPENSE) },
                modifier = Modifier.weight(1f)
            ) { Text("Expense") }
            androidx.compose.material3.OutlinedButton(
                onClick = { navController.navigate(Routes.TRANSACTIONS) },
                modifier = Modifier.weight(1f)
            ) { Text("Ledger") }
        }
    }
}

// ---------------------------------------------------------------------------
// Account form / detail
// ---------------------------------------------------------------------------

data class AccountForm(
    val businessId: Long = 0,
    val accountId: Long = 0,
    val name: String = "",
    val type: String = "CASH",
    val openingText: String = "",
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
)

class AccountFormViewModel(container: AppContainer, private val accountId: Long) : ViewModel() {

    private val finance = container.financeRepository
    private val form = MutableStateFlow(AccountForm())

    val state: StateFlow<AccountForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else if (accountId > 0 && !form.value.loaded) {
                finance.observeAccount(accountId).map { account ->
                    form.value.copy(
                        businessId = id,
                        accountId = accountId,
                        loaded = true,
                        name = account?.name ?: "",
                        type = account?.type ?: "CUSTOM",
                        openingText = if ((account?.openingBalanceMinor ?: 0) > 0)
                            com.hisabnikash.app.domain.model.formatMoneyPlain(account!!.openingBalanceMinor) else ""
                    )
                }
            } else flowOf(form.value.copy(businessId = id))
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            AccountForm()
        )

    fun setName(v: String) { form.value = form.value.copy(name = v) }
    fun setType(v: String) { form.value = form.value.copy(type = v) }
    fun setOpening(v: Long) { form.value = form.value.copy(openingText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }

    fun save(onSaved: (Long) -> Unit) {
        val f = form.value
        if (f.name.isBlank()) {
            form.value = f.copy(error = "Account name is required.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                finance.saveAccount(
                    AccountEntity(
                        id = f.accountId,
                        businessId = f.businessId,
                        name = f.name.trim(),
                        type = f.type,
                        openingBalanceMinor = parseMoneyInput(f.openingText) ?: 0
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess(onSaved).onFailure { e ->
                    form.value = f.copy(saving = false, error = "Couldn't save account: ${e.message}")
                }
            }
        }
    }
}

@Composable
fun AccountFormRoute(container: AppContainer, navController: NavHostController, accountId: Long?) {
    val vm = appViewModel(container, key = "account-form-${accountId ?: 0}") {
        AccountFormViewModel(it, accountId ?: 0)
    }
    val state by vm.state.collectAsState()

    ScreenFrame(
        if (accountId == null) "New account" else "Edit account",
        onBack = { navController.popBackStack() }
    ) {
        if (accountId != null && !state.loaded) {
            Text("Loading account…", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        AppTextField("Account name", state.name, { vm.setName(it) }, placeholder = "e.g. Cash drawer / bKash")
        AppDropdown(
            "Type",
            listOf(
                DropOption("CASH", "Cash"),
                DropOption("BANK", "Bank"),
                DropOption("BKASH", "bKash"),
                DropOption("NAGAD", "Nagad"),
                DropOption("ROCKET", "Rocket"),
                DropOption("CUSTOM", "Custom")
            ),
            state.type,
            { vm.setType(it.id) }
        )
        MoneyField("Opening balance", parseMoneyInput(state.openingText) ?: 0, { vm.setOpening(it) })
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = {
                vm.save { id ->
                    navController.navigate(Routes.account(id)) {
                        popUpTo(Routes.NEW_ACCOUNT) { inclusive = true }
                    }
                }
            },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Saving…" else "Save account") }
    }
}

data class AccountDetailUi(
    val businessId: Long = 0,
    val account: AccountEntity? = null,
    val transactions: List<AccountTransactionEntity> = emptyList()
)

class AccountDetailViewModel(container: AppContainer, private val accountId: Long) : ViewModel() {

    private val finance = container.financeRepository

    val state: StateFlow<AccountDetailUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(AccountDetailUi())
            else combine(
                finance.observeAccount(accountId),
                finance.observeTransactions(id, "ALL").map { list -> list.filter { it.accountId == accountId } }
            ) { account, transactions ->
                AccountDetailUi(businessId = id, account = account, transactions = transactions)
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            AccountDetailUi()
        )
}

@Composable
fun AccountDetailRoute(container: AppContainer, navController: NavHostController, accountId: Long) {
    val vm = appViewModel(container, key = "account-$accountId") { AccountDetailViewModel(it, accountId) }
    val state by vm.state.collectAsState()
    val account = state.account

    ScreenFrame(
        title = account?.name ?: "Account",
        onBack = { navController.popBackStack() },
        subtitle = account?.let { accountTypeLabel(it.type) },
        actions = {
            if (account != null) {
                TextButton(onClick = { navController.navigate(Routes.accountEdit(account.id)) }) { Text("Edit") }
            }
        }
    ) {
        if (account == null) {
            Text("Account not found.", color = InkFaint, modifier = Modifier.padding(16.dp))
            return@ScreenFrame
        }
        SectionHeader("Activity")
        if (state.transactions.isEmpty()) {
            EmptyState(
                Icons.Filled.Savings,
                "No activity yet",
                "Payments, expenses and transfers for this account will appear here."
            )
        } else {
            state.transactions.forEach { tx ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                tx.note ?: tx.category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                formatDateTime(tx.dateAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        Text(
                            (if (tx.direction == "IN") "+" else "-") + formatMoney(tx.amountMinor),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.direction == "IN") BrandGreen else Error
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Transactions (ledger)
// ---------------------------------------------------------------------------

data class LedgerUi(
    val loading: Boolean = true,
    val filter: String = "ALL",
    val transactions: List<AccountTransactionEntity> = emptyList(),
    val inflow: Long = 0,
    val outflow: Long = 0
)

class LedgerViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val filterFlow = MutableStateFlow("ALL")

    val state: StateFlow<LedgerUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(LedgerUi().copy(loading = false))
            else filterFlow.flatMapLatest { filter ->
                finance.observeTransactions(id, filter).map { list ->
                    LedgerUi(
                        loading = false,
                        filter = filter,
                        transactions = list,
                        inflow = list.filter { it.direction == "IN" && it.category != "TRANSFER" }
                            .sumOf { it.amountMinor },
                        outflow = list.filter { it.direction == "OUT" && it.category != "TRANSFER" }
                            .sumOf { it.amountMinor }
                    )
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            LedgerUi()
        )

    fun setFilter(filter: String) { filterFlow.value = filter }
}

@Composable
fun TransactionsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { LedgerViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Transactions", onBack = { navController.popBackStack() }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            MetricCard("In", formatMoney(state.inflow), Modifier.weight(1f))
            MetricCard("Out", formatMoney(state.outflow), Modifier.weight(1f))
            MetricCard("Net", formatMoney(state.inflow - state.outflow), Modifier.weight(1f))
        }
        FilterChips(
            listOf("ALL", "PAYMENT", "COD", "EXPENSE", "SETTLEMENT", "PURCHASE", "REFUND", "TRANSFER"),
            state.filter,
            { vm.setFilter(it) }
        )
        if (state.transactions.isEmpty()) {
            EmptyState(
                Icons.Filled.Payments,
                "No transactions",
                "Every payment, expense, settlement and transfer appears here."
            )
        } else {
            state.transactions.forEach { tx ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                tx.note ?: tx.category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${formatDateTime(tx.dateAt)} • ${tx.refType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint
                            )
                        }
                        Text(
                            (if (tx.direction == "IN") "+" else "-") + formatMoney(tx.amountMinor),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.direction == "IN") BrandGreen else Error
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Transfers
// ---------------------------------------------------------------------------

data class TransfersUi(
    val loading: Boolean = true,
    val transfers: List<TransferEntity> = emptyList()
)

class TransfersViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository

    val state: StateFlow<TransfersUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(TransfersUi().copy(loading = false))
            else finance.observeTransfers(id).map { list -> TransfersUi(loading = false, transfers = list) }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            TransfersUi()
        )
}

@Composable
fun TransfersScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { TransfersViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Transfers", onBack = { navController.popBackStack() }) {
        if (state.transfers.isEmpty()) {
            EmptyState(
                Icons.Filled.SwapHoriz,
                "No transfers yet",
                "Move money between your cash, bank and mobile wallets.",
                actionLabel = "New Transfer",
                onAction = { navController.navigate(Routes.TRANSFER) }
            )
        } else {
            state.transfers.forEach { transfer ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                    Column(Modifier.padding(14.dp)) {
                        LabelValueRow("Amount", formatMoney(transfer.amountMinor))
                        LabelValueRow("From", "#${transfer.fromAccountId}")
                        LabelValueRow("To", "#${transfer.toAccountId}")
                        LabelValueRow("Date", formatDateTime(transfer.dateAt))
                        LabelValueRow("Note", transfer.note ?: "—")
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.TRANSFER) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("New transfer") }
    }
}

data class TransferForm(
    val businessId: Long = 0,
    val fromId: Long? = null,
    val toId: Long? = null,
    val amountText: String = "",
    val note: String = "",
    val accounts: List<DropOption> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class TransferFormViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val form = MutableStateFlow(TransferForm())

    val state: StateFlow<TransferForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                finance.observeAccounts(id).map { list ->
                    list.map { DropOption("${it.id}", "${it.name} (${accountTypeLabel(it.type)})") }
                }
            ) { f, accounts ->
                f.copy(
                    businessId = id,
                    accounts = accounts,
                    fromId = f.fromId ?: accounts.firstOrNull()?.id?.toLong(),
                    toId = f.toId ?: accounts.getOrNull(1)?.id?.toLong()
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            TransferForm()
        )

    fun setFrom(id: Long?) { form.value = form.value.copy(fromId = id) }
    fun setTo(id: Long?) { form.value = form.value.copy(toId = id) }
    fun setAmount(v: Long) { form.value = form.value.copy(amountText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        if (f.fromId == null || f.toId == null) {
            form.value = f.copy(error = "Choose both accounts.")
            return
        }
        if (f.fromId == f.toId) {
            form.value = f.copy(error = "From and to accounts must differ.")
            return
        }
        val amount = parseMoneyInput(f.amountText) ?: 0
        if (amount <= 0) {
            form.value = f.copy(error = "Enter an amount greater than zero.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                finance.recordTransfer(
                    com.hisabnikash.app.data.repo.TransferInput(
                        businessId = f.businessId,
                        fromAccountId = f.fromId,
                        toAccountId = f.toId,
                        amountMinor = amount,
                        note = f.note.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't record transfer.")
                }
            }
        }
    }
}

@Composable
fun TransferFormRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { TransferFormViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("New transfer", onBack = { navController.popBackStack() }) {
        AppDropdown("From", state.accounts, state.fromId?.toString(), { vm.setFrom(it.id.toLong()) })
        AppDropdown("To", state.accounts, state.toId?.toString(), { vm.setTo(it.id.toLong()) })
        MoneyField("Amount", parseMoneyInput(state.amountText) ?: 0, { vm.setAmount(it) })
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Recording…" else "Record transfer") }
    }
}

// ---------------------------------------------------------------------------
// Expenses
// ---------------------------------------------------------------------------

data class ExpensesUi(
    val loading: Boolean = true,
    val category: String = "ALL",
    val expenses: List<ExpenseEntity> = emptyList(),
    val total: Long = 0
)

class ExpensesViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val categoryFlow = MutableStateFlow("ALL")

    val state: StateFlow<ExpensesUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(ExpensesUi().copy(loading = false))
            else categoryFlow.flatMapLatest { category ->
                finance.observeExpenses(id, category).map { list ->
                    ExpensesUi(loading = false, category = category, expenses = list, total = list.sumOf { it.amountMinor })
                }
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            ExpensesUi()
        )

    fun setCategory(category: String) { categoryFlow.value = category }
}

@Composable
fun ExpensesScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { ExpensesViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Expenses", onBack = { navController.popBackStack() }) {
        MetricCard("Total shown", formatMoney(state.total), Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        FilterChips(
            listOf(
                "ALL", "ADVERTISING", "COURIER", "PACKAGING", "PURCHASES",
                "RENT", "SALARY", "UTILITIES", "SOFTWARE", "OPERATIONS", "OTHER"
            ),
            state.category,
            { vm.setCategory(it) }
        )
        if (state.expenses.isEmpty()) {
            EmptyState(
                Icons.Filled.ReceiptLong,
                "No expenses",
                "Record every cost so profit stays honest.",
                actionLabel = "Add Expense",
                onAction = { navController.navigate(Routes.EXPENSE) }
            )
        } else {
            state.expenses.forEach { expense ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                expense.description ?: expense.category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${formatDateTime(expense.dateAt)} • ${expense.vendor ?: expense.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkFaint,
                                maxLines = 1
                            )
                        }
                        Text(
                            formatMoney(expense.amountMinor),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Error
                        )
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate(Routes.EXPENSE) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("Add expense") }
    }
}

data class ExpenseForm(
    val businessId: Long = 0,
    val category: String = "OTHER",
    val accountId: Long? = null,
    val amountText: String = "",
    val vendor: String = "",
    val description: String = "",
    val note: String = "",
    val accounts: List<DropOption> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class ExpenseFormViewModel(container: AppContainer) : ViewModel() {

    private val finance = container.financeRepository
    private val form = MutableStateFlow(ExpenseForm())

    val state: StateFlow<ExpenseForm> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                form,
                finance.observeAccounts(id).map { list ->
                    list.map { DropOption("${it.id}", it.name) }
                }
            ) { f, accounts ->
                f.copy(
                    businessId = id,
                    accounts = accounts,
                    accountId = f.accountId ?: accounts.firstOrNull()?.id?.toLong()
                )
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            ExpenseForm()
        )

    fun setCategory(v: String) { form.value = form.value.copy(category = v) }
    fun setAccount(v: Long?) { form.value = form.value.copy(accountId = v) }
    fun setAmount(v: Long) { form.value = form.value.copy(amountText = com.hisabnikash.app.domain.model.formatMoneyPlain(v)) }
    fun setVendor(v: String) { form.value = form.value.copy(vendor = v) }
    fun setDescription(v: String) { form.value = form.value.copy(description = v) }
    fun setNote(v: String) { form.value = form.value.copy(note = v) }

    fun save(onSaved: () -> Unit) {
        val f = form.value
        val amount = parseMoneyInput(f.amountText) ?: 0
        if (amount <= 0) {
            form.value = f.copy(error = "Enter an amount greater than zero.")
            return
        }
        form.value = f.copy(saving = true, error = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                finance.createExpense(
                    com.hisabnikash.app.data.repo.ExpenseInput(
                        businessId = f.businessId,
                        category = f.category,
                        accountId = f.accountId,
                        amountMinor = amount,
                        vendor = f.vendor.ifBlank { null },
                        description = f.description.ifBlank { null },
                        note = f.note.ifBlank { null }
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess { onSaved() }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't record expense.")
                }
            }
        }
    }
}

@Composable
fun ExpenseFormRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { ExpenseFormViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("New expense", onBack = { navController.popBackStack() }) {
        AppDropdown(
            "Category",
            listOf(
                DropOption("ADVERTISING", "Advertising"),
                DropOption("COURIER", "Courier"),
                DropOption("PACKAGING", "Packaging"),
                DropOption("PURCHASES", "Purchases"),
                DropOption("RENT", "Rent"),
                DropOption("SALARY", "Salary"),
                DropOption("UTILITIES", "Utilities"),
                DropOption("SOFTWARE", "Software"),
                DropOption("OPERATIONS", "Operations"),
                DropOption("OTHER", "Other")
            ),
            state.category,
            { vm.setCategory(it.id) }
        )
        AppDropdown("Paid from", state.accounts, state.accountId?.toString(), { vm.setAccount(it.id.toLong()) })
        MoneyField("Amount", parseMoneyInput(state.amountText) ?: 0, { vm.setAmount(it) })
        AppTextField("Vendor", state.vendor, { vm.setVendor(it) }, placeholder = "e.g. Pathao")
        AppTextField("Description", state.description, { vm.setDescription(it) }, singleLine = false, minLines = 2)
        AppTextField("Note", state.note, { vm.setNote(it) }, singleLine = false, minLines = 2)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save { navController.popBackStack() } },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(if (state.saving) "Recording…" else "Record expense") }
    }
}
