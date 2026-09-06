package com.hisabnikash.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.ui.business.BusinessSwitcherScreenRoute
import com.hisabnikash.app.ui.business.NewBusinessScreenRoute
import com.hisabnikash.app.ui.documents.InvoiceDetailRoute
import com.hisabnikash.app.ui.documents.InvoiceFormRoute
import com.hisabnikash.app.ui.documents.InvoicesListRoute
import com.hisabnikash.app.ui.finance.AccountDetailRoute
import com.hisabnikash.app.ui.finance.AccountFormRoute
import com.hisabnikash.app.ui.finance.AccountsScreenRoute
import com.hisabnikash.app.ui.finance.BudgetFormRoute
import com.hisabnikash.app.ui.finance.BudgetsScreenRoute
import com.hisabnikash.app.ui.finance.CampaignFormRoute
import com.hisabnikash.app.ui.finance.CampaignsScreenRoute
import com.hisabnikash.app.ui.finance.CourierFormRoute
import com.hisabnikash.app.ui.finance.CouriersScreenRoute
import com.hisabnikash.app.ui.finance.ExpenseFormRoute
import com.hisabnikash.app.ui.finance.ExpensesScreenRoute
import com.hisabnikash.app.ui.finance.PayOutRoute
import com.hisabnikash.app.ui.finance.PayablesScreenRoute
import com.hisabnikash.app.ui.finance.ReceivePaymentRoute
import com.hisabnikash.app.ui.finance.ReceivablesScreenRoute
import com.hisabnikash.app.ui.finance.SettlementFormRoute
import com.hisabnikash.app.ui.finance.SettlementsScreenRoute
import com.hisabnikash.app.ui.finance.TransferFormRoute
import com.hisabnikash.app.ui.finance.TransfersScreenRoute
import com.hisabnikash.app.ui.finance.TransactionsScreenRoute
import com.hisabnikash.app.ui.more.AnalyticsScreenRoute
import com.hisabnikash.app.ui.more.ChannelsScreenRoute
import com.hisabnikash.app.ui.more.InventoryScreenRoute
import com.hisabnikash.app.ui.more.ReceiptsListRoute
import com.hisabnikash.app.ui.notifications.NotificationsScreenRoute
import com.hisabnikash.app.ui.onboarding.OnboardingScreen
import com.hisabnikash.app.ui.orders.OrderDetailRoute
import com.hisabnikash.app.ui.orders.OrderFormRoute
import com.hisabnikash.app.ui.products.ProductDetailRoute
import com.hisabnikash.app.ui.products.ProductFormRoute
import com.hisabnikash.app.ui.root.BootScreen
import com.hisabnikash.app.ui.returns.ExchangeFormRoute
import com.hisabnikash.app.ui.returns.ExchangesListRoute
import com.hisabnikash.app.ui.returns.RefundFormRoute
import com.hisabnikash.app.ui.returns.RefundsListRoute
import com.hisabnikash.app.ui.returns.ReturnFormRoute
import com.hisabnikash.app.ui.returns.ReturnsListRoute
import com.hisabnikash.app.ui.search.SearchScreenRoute
import com.hisabnikash.app.ui.settings.AboutScreenRoute
import com.hisabnikash.app.ui.settings.BackupScreenRoute
import com.hisabnikash.app.ui.settings.DataHealthScreenRoute
import com.hisabnikash.app.ui.settings.SecurityScreenRoute
import com.hisabnikash.app.ui.settings.SettingsScreenRoute
import com.hisabnikash.app.ui.settings.SupportScreenRoute
import com.hisabnikash.app.ui.shell.MainShell
import com.hisabnikash.app.ui.suppliers.PurchaseDetailRoute
import com.hisabnikash.app.ui.suppliers.PurchaseFormRoute
import com.hisabnikash.app.ui.suppliers.SupplierDetailRoute
import com.hisabnikash.app.ui.suppliers.SupplierFormRoute
import com.hisabnikash.app.ui.suppliers.SuppliersScreenRoute
import com.hisabnikash.app.ui.customers.CustomerDetailRoute
import com.hisabnikash.app.ui.customers.CustomerFormRoute

object Routes {
    const val BOOT = "boot"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"

    const val ORDER = "order/{id}"
    const val NEW_ORDER = "order/new"
    fun order(id: Long) = "order/$id"

    const val PRODUCT = "product/{id}"
    const val NEW_PRODUCT = "product/new"
    const val PRODUCT_EDIT = "product/{id}/edit"
    fun product(id: Long) = "product/$id"
    fun productEdit(id: Long) = "product/$id/edit"

    const val CUSTOMER = "customer/{id}"
    const val NEW_CUSTOMER = "customer/new"
    const val CUSTOMER_EDIT = "customer/{id}/edit"
    fun customer(id: Long) = "customer/$id"
    fun customerEdit(id: Long) = "customer/$id/edit"

    const val SUPPLIER = "supplier/{id}"
    const val NEW_SUPPLIER = "supplier/new"
    const val SUPPLIER_EDIT = "supplier/{id}/edit"
    const val SUPPLIERS = "suppliers"
    fun supplier(id: Long) = "supplier/$id"
    fun supplierEdit(id: Long) = "supplier/$id/edit"

    const val PURCHASE = "purchase/{id}"
    const val NEW_PURCHASE = "purchase/new"
    fun purchase(id: Long) = "purchase/$id"

    const val ACCOUNT = "account/{id}"
    const val NEW_ACCOUNT = "account/new"
    const val ACCOUNT_EDIT = "account/{id}/edit"
    const val ACCOUNTS = "accounts"
    fun account(id: Long) = "account/$id"
    fun accountEdit(id: Long) = "account/$id/edit"

    const val CAMPAIGN = "campaign/{id}"
    const val NEW_CAMPAIGN = "campaign/new"
    const val CAMPAIGNS = "campaigns"
    fun campaign(id: Long) = "campaign/$id"

    const val BUDGET = "budget/{id}"
    const val NEW_BUDGET = "budget/new"
    const val BUDGETS = "budgets"
    fun budget(id: Long) = "budget/$id"

    const val INVOICE = "invoice/{id}"
    const val NEW_INVOICE = "invoice/new"
    const val INVOICES = "invoices"
    fun invoice(id: Long) = "invoice/$id"

    const val RETURN = "return/{id}"
    const val NEW_RETURN = "return/new"
    const val RETURN_FOR_ORDER = "return/from-order/{orderId}"
    const val RETURNS = "returns"
    fun returnDetail(id: Long) = "return/$id"
    fun returnForOrder(orderId: Long) = "return/from-order/$orderId"

    const val EXCHANGE = "exchange/{id}"
    const val NEW_EXCHANGE = "exchange/new"
    const val EXCHANGE_FOR_ORDER = "exchange/from-order/{orderId}"
    const val EXCHANGES = "exchanges"
    fun exchange(id: Long) = "exchange/$id"
    fun exchangeForOrder(orderId: Long) = "exchange/from-order/$orderId"

    const val REFUND = "refund/{id}"
    const val NEW_REFUND = "refund/new"
    const val REFUND_FOR_ORDER = "refund/from-order/{orderId}"
    const val REFUNDS = "refunds"
    fun refund(id: Long) = "refund/$id"
    fun refundForOrder(orderId: Long) = "refund/from-order/$orderId"

    const val RECEIPTS = "receipts"
    const val SETTLEMENT = "settlement/new"
    const val SETTLEMENTS = "settlements"
    const val TRANSFER = "transfer/new"
    const val TRANSFERS = "transfers"
    const val EXPENSE = "expense/new"
    const val EXPENSES = "expenses"
    const val TRANSACTIONS = "transactions"
    const val RECEIVABLES = "receivables"
    const val PAYABLES = "payables"
    const val PAYMENT = "payment"
    const val RECEIVABLE_PAY = "payment/receivable/{id}"
    const val PAYABLE_PAY = "payment/payable/{id}"
    fun receivablePay(id: Long) = "payment/receivable/$id"
    fun payablePay(id: Long) = "payment/payable/$id"

    const val INVENTORY = "inventory"
    const val CHANNELS = "channels"
    const val COURIERS = "couriers"
    const val COURIER_EDIT = "courier/new"
    const val COURIER_EDIT_WITH_ID = "courier/{id}/edit"
    fun courierEdit(id: Long) = "courier/$id/edit"

    const val ANALYTICS = "analytics"
    const val SEARCH = "search"
    const val NOTIFICATIONS = "notifications"
    const val BACKUP = "backup"
    const val DATA_HEALTH = "data-health"
    const val SETTINGS = "settings"
    const val SECURITY = "security"
    const val SUPPORT = "support"
    const val ABOUT = "about"
    const val BUSINESS_SWITCHER = "businesses"
    const val NEW_BUSINESS = "business/new"
}

@Composable
fun HisabNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.BOOT) {
        composable(Routes.BOOT) {
            BootScreen(container, navController)
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(container, navController)
        }
        composable(Routes.MAIN) {
            MainShell(container, navController)
        }
        composable(
            Routes.ORDER,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            OrderDetailRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.NEW_ORDER) {
            OrderFormRoute(container, navController, null)
        }
        composable(
            Routes.PRODUCT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            ProductDetailRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.NEW_PRODUCT) {
            ProductFormRoute(container, navController, null)
        }
        composable(
            Routes.PRODUCT_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            ProductFormRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(
            Routes.CUSTOMER,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            CustomerDetailRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.NEW_CUSTOMER) {
            CustomerFormRoute(container, navController, null)
        }
        composable(
            Routes.CUSTOMER_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            CustomerFormRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(
            Routes.SUPPLIER,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            SupplierDetailRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.NEW_SUPPLIER) {
            SupplierFormRoute(container, navController, null)
        }
        composable(
            Routes.SUPPLIER_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            SupplierFormRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.SUPPLIERS) {
            SuppliersScreenRoute(container, navController)
        }
        composable(Routes.NEW_PURCHASE) {
            PurchaseFormRoute(container, navController)
        }
        composable(
            Routes.PURCHASE,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            PurchaseDetailRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(
            Routes.ACCOUNT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            AccountDetailRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.NEW_ACCOUNT) {
            AccountFormRoute(container, navController, null)
        }
        composable(
            Routes.ACCOUNT_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            AccountFormRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.ACCOUNTS) {
            AccountsScreenRoute(container, navController)
        }
        composable(Routes.TRANSACTIONS) {
            TransactionsScreenRoute(container, navController)
        }
        composable(Routes.TRANSFERS) {
            TransfersScreenRoute(container, navController)
        }
        composable(Routes.TRANSFER) {
            TransferFormRoute(container, navController)
        }
        composable(Routes.EXPENSES) {
            ExpensesScreenRoute(container, navController)
        }
        composable(Routes.EXPENSE) {
            ExpenseFormRoute(container, navController)
        }
        composable(Routes.RECEIVABLES) {
            ReceivablesScreenRoute(container, navController)
        }
        composable(Routes.PAYABLES) {
            PayablesScreenRoute(container, navController)
        }
        composable(Routes.PAYMENT) {
            ReceivePaymentRoute(container, navController, null)
        }
        composable(
            Routes.RECEIVABLE_PAY,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            ReceivePaymentRoute(container, navController, entry.arguments?.getLong("id"))
        }
        composable(
            Routes.PAYABLE_PAY,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            PayOutRoute(container, navController, entry.arguments?.getLong("id"))
        }
        composable(Routes.SETTLEMENTS) {
            SettlementsScreenRoute(container, navController)
        }
        composable(Routes.SETTLEMENT) {
            SettlementFormRoute(container, navController)
        }
        composable(Routes.COURIERS) {
            CouriersScreenRoute(container, navController)
        }
        composable(Routes.COURIER_EDIT) {
            CourierFormRoute(container, navController, null)
        }
        composable(
            Routes.COURIER_EDIT_WITH_ID,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            CourierFormRoute(container, navController, entry.arguments?.getLong("id"))
        }
        composable(Routes.CHANNELS) {
            ChannelsScreenRoute(container, navController)
        }
        composable(Routes.NEW_INVOICE) {
            InvoiceFormRoute(container, navController, null)
        }
        composable(
            Routes.INVOICE,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            InvoiceDetailRoute(container, navController, entry.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.INVOICES) {
            InvoicesListRoute(container, navController)
        }
        composable(Routes.RECEIPTS) {
            ReceiptsListRoute(container, navController)
        }
        composable(Routes.NEW_RETURN) {
            ReturnFormRoute(container, navController)
        }
        composable(
            Routes.RETURN_FOR_ORDER,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { entry ->
            ReturnFormRoute(container, navController, entry.arguments?.getLong("orderId"))
        }
        composable(Routes.RETURNS) {
            ReturnsListRoute(container, navController)
        }
        composable(Routes.NEW_EXCHANGE) {
            ExchangeFormRoute(container, navController)
        }
        composable(
            Routes.EXCHANGE_FOR_ORDER,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { entry ->
            ExchangeFormRoute(container, navController, entry.arguments?.getLong("orderId"))
        }
        composable(Routes.EXCHANGES) {
            ExchangesListRoute(container, navController)
        }
        composable(Routes.NEW_REFUND) {
            RefundFormRoute(container, navController)
        }
        composable(
            Routes.REFUND_FOR_ORDER,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { entry ->
            RefundFormRoute(container, navController, entry.arguments?.getLong("orderId"))
        }
        composable(Routes.REFUNDS) {
            RefundsListRoute(container, navController)
        }
        composable(Routes.NEW_CAMPAIGN) {
            CampaignFormRoute(container, navController)
        }
        composable(Routes.CAMPAIGNS) {
            CampaignsScreenRoute(container, navController)
        }
        composable(Routes.NEW_BUDGET) {
            BudgetFormRoute(container, navController)
        }
        composable(Routes.BUDGETS) {
            BudgetsScreenRoute(container, navController)
        }
        composable(Routes.INVENTORY) {
            InventoryScreenRoute(container, navController)
        }
        composable(Routes.ANALYTICS) {
            AnalyticsScreenRoute(container, navController)
        }
        composable(Routes.SEARCH) {
            SearchScreenRoute(container, navController)
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreenRoute(container, navController)
        }
        composable(Routes.BACKUP) {
            BackupScreenRoute(container, navController)
        }
        composable(Routes.DATA_HEALTH) {
            DataHealthScreenRoute(container, navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreenRoute(container, navController)
        }
        composable(Routes.SECURITY) {
            SecurityScreenRoute(container, navController)
        }
        composable(Routes.SUPPORT) {
            SupportScreenRoute(container, navController)
        }
        composable(Routes.ABOUT) {
            AboutScreenRoute(container, navController)
        }
        composable(Routes.BUSINESS_SWITCHER) {
            BusinessSwitcherScreenRoute(container, navController)
        }
        composable(Routes.NEW_BUSINESS) {
            NewBusinessScreenRoute(container, navController)
        }
    }
}
