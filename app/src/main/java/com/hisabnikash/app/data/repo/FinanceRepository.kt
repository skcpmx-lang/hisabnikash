package com.hisabnikash.app.data.repo

import androidx.room.withTransaction
import com.hisabnikash.app.data.db.AccountEntity
import com.hisabnikash.app.data.db.AccountTransactionEntity
import com.hisabnikash.app.data.db.AppDatabase
import com.hisabnikash.app.data.db.AuditEventEntity
import com.hisabnikash.app.data.db.BudgetEntity
import com.hisabnikash.app.data.db.CampaignEntity
import com.hisabnikash.app.data.db.ExpenseEntity
import com.hisabnikash.app.data.db.PayableEntity
import com.hisabnikash.app.data.db.ReceivableEntity
import com.hisabnikash.app.data.db.SalesChannelEntity
import com.hisabnikash.app.data.db.TransferEntity
import kotlinx.coroutines.flow.Flow

data class TransferInput(
    val businessId: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amountMinor: Long,
    val dateAt: Long = System.currentTimeMillis(),
    val note: String? = null
)

data class ExpenseInput(
    val businessId: Long,
    val category: String,
    val accountId: Long?,
    val amountMinor: Long,
    val dateAt: Long = System.currentTimeMillis(),
    val vendor: String? = null,
    val description: String? = null,
    val note: String? = null,
    val attachmentPath: String? = null,
    val recurring: Boolean = false
)

class FinanceRepository(private val db: AppDatabase, private val workspace: WorkspaceRepository) {

    // ---------------------------------------------------------------- accounts

    fun observeBalances(businessId: Long) = db.accountDao().observeBalances(businessId)

    fun observeAccounts(businessId: Long) = db.accountDao().observeAll(businessId)

    suspend fun listAccounts(businessId: Long) = db.accountDao().listAll(businessId)

    suspend fun saveAccount(account: AccountEntity): Long {
        workspace.requireBusinessExists(account.businessId)
        if (account.id > 0) {
            val existing = db.accountDao().getById(account.id)
            if (existing != null && existing.businessId != account.businessId) {
                throw IllegalStateException(
                    "This account belongs to a different business. Switch business and try again."
                )
            }
        }
        return if (account.id == 0L) db.accountDao().insert(account)
        else {
            db.accountDao().update(account)
            account.id
        }
    }

    fun observeAccount(id: Long) = db.accountDao().observeById(id)

    // ------------------------------------------------------------------ ledger

    fun observeTransactions(businessId: Long, filter: String) =
        db.ledgerDao().observe(businessId, filter)

    fun observeTransfers(businessId: Long) = db.transferDao().observeAll(businessId)

    suspend fun recordTransfer(input: TransferInput) = db.withTransaction {
        workspace.requireBusinessExists(input.businessId)
        require(input.fromAccountId != input.toAccountId) { "Choose two different accounts." }
        val transferId = db.transferDao().insert(
            TransferEntity(
                businessId = input.businessId,
                fromAccountId = input.fromAccountId,
                toAccountId = input.toAccountId,
                amountMinor = input.amountMinor,
                dateAt = input.dateAt,
                note = input.note
            )
        )
        db.ledgerDao().insert(
            AccountTransactionEntity(
                businessId = input.businessId,
                accountId = input.fromAccountId,
                direction = "OUT",
                amountMinor = input.amountMinor,
                dateAt = input.dateAt,
                category = "TRANSFER",
                refType = "TRANSFER",
                refId = transferId,
                note = input.note ?: "Transfer out"
            )
        )
        db.ledgerDao().insert(
            AccountTransactionEntity(
                businessId = input.businessId,
                accountId = input.toAccountId,
                direction = "IN",
                amountMinor = input.amountMinor,
                dateAt = input.dateAt,
                category = "TRANSFER",
                refType = "TRANSFER",
                refId = transferId,
                note = input.note ?: "Transfer in"
            )
        )
        audit(input.businessId, "TRANSFER", transferId, "CREATE", "A transfer was recorded.")
    }

    // ---------------------------------------------------------------- expenses

    fun observeExpenses(businessId: Long, category: String) =
        db.expenseDao().observeFiltered(businessId, category)

    suspend fun createExpense(input: ExpenseInput): Long = db.withTransaction {
        workspace.requireBusinessExists(input.businessId)
        val expenseId = db.expenseDao().insert(
            ExpenseEntity(
                businessId = input.businessId,
                category = input.category,
                accountId = input.accountId,
                amountMinor = input.amountMinor,
                dateAt = input.dateAt,
                vendor = input.vendor,
                description = input.description,
                note = input.note,
                attachmentPath = input.attachmentPath,
                recurring = input.recurring
            )
        )
        input.accountId?.let { accountId ->
            db.ledgerDao().insert(
                AccountTransactionEntity(
                    businessId = input.businessId,
                    accountId = accountId,
                    direction = "OUT",
                    amountMinor = input.amountMinor,
                    dateAt = input.dateAt,
                    category = input.category,
                    refType = "EXPENSE",
                    refId = expenseId,
                    note = input.description ?: input.category.lowercase().replaceFirstChar { it.uppercase() }
                )
            )
        }
        audit(input.businessId, "EXPENSE", expenseId, "CREATE", "${input.category}: ${input.amountMinor}")
        expenseId
    }

    suspend fun updateExpense(expense: ExpenseEntity) = db.withTransaction {
        val current = db.expenseDao().getById(expense.id) ?: return@withTransaction
        db.expenseDao().update(expense.copy(updatedAt = System.currentTimeMillis()))
        // Keep the ledger in sync: replace the expense's ledger entry.
        db.ledgerDao().deleteForRef(expense.businessId, "EXPENSE", expense.id)
        expense.accountId?.let { accountId ->
            db.ledgerDao().insert(
                AccountTransactionEntity(
                    businessId = expense.businessId,
                    accountId = accountId,
                    direction = "OUT",
                    amountMinor = expense.amountMinor,
                    dateAt = expense.dateAt,
                    category = expense.category,
                    refType = "EXPENSE",
                    refId = expense.id,
                    note = expense.description ?: expense.category.lowercase().replaceFirstChar { it.uppercase() }
                )
            )
        }
        audit(expense.businessId, "EXPENSE", expense.id, "UPDATE", current.description ?: current.category)
    }

    // ----------------------------------------------------- receivables/payables

    fun observeReceivables(businessId: Long, status: String) =
        db.receivablePayableDao().observeReceivables(businessId, status)

    fun observeReceivablesForCustomer(businessId: Long, customerId: Long) =
        db.receivablePayableDao().observeReceivablesForCustomer(businessId, customerId)

    fun observeReceivablesOutstanding(businessId: Long) =
        db.receivablePayableDao().observeOutstanding(businessId)

    fun observePayables(businessId: Long, status: String) =
        db.receivablePayableDao().observePayables(businessId, status)

    fun observePayablesForSupplier(businessId: Long, supplierId: Long) =
        db.receivablePayableDao().observePayablesForSupplier(businessId, supplierId)

    fun observePayablesOutstanding(businessId: Long) =
        db.receivablePayableDao().observePayablesOutstanding(businessId)

    suspend fun saveCustomReceivable(receivable: ReceivableEntity): Long {
        workspace.requireBusinessExists(receivable.businessId)
        return if (receivable.id == 0L) db.receivablePayableDao().insertReceivable(receivable)
        else {
            db.receivablePayableDao().updateReceivable(receivable)
            receivable.id
        }
    }

    suspend fun saveCustomPayable(payable: PayableEntity): Long {
        workspace.requireBusinessExists(payable.businessId)
        return if (payable.id == 0L) db.receivablePayableDao().insertPayable(payable)
        else {
            db.receivablePayableDao().updatePayable(payable)
            payable.id
        }
    }

    // --------------------------------------------------------- growth: ad/budget

    fun observeCampaigns(businessId: Long) = db.campaignDao().observeAll(businessId)

    fun observeCampaign(id: Long) = db.campaignDao().observeById(id)

    suspend fun saveCampaign(campaign: CampaignEntity): Long {
        workspace.requireBusinessExists(campaign.businessId)
        if (campaign.id > 0) {
            val existing = db.campaignDao().getById(campaign.id)
            if (existing != null && existing.businessId != campaign.businessId) {
                throw IllegalStateException(
                    "This campaign belongs to a different business. Switch business and try again."
                )
            }
        }
        return if (campaign.id == 0L) db.campaignDao().insert(campaign)
        else {
            db.campaignDao().insert(campaign)
            campaign.id
        }
    }

    fun observeChannels(businessId: Long) = db.channelDao().observeAll(businessId)

    suspend fun listChannels(businessId: Long) = db.channelDao().listAll(businessId)

    suspend fun addChannel(businessId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            db.channelDao().insert(SalesChannelEntity(businessId = businessId, name = trimmed))
        }
    }

    fun observeBudgets(businessId: Long, category: String) =
        db.budgetDao().observeFiltered(businessId, category)

    suspend fun saveBudget(budget: BudgetEntity): Long {
        workspace.requireBusinessExists(budget.businessId)
        if (budget.id > 0) {
            val existing = db.budgetDao().getById(budget.id)
            if (existing != null && existing.businessId != budget.businessId) {
                throw IllegalStateException(
                    "This budget belongs to a different business. Switch business and try again."
                )
            }
        }
        return if (budget.id == 0L) db.budgetDao().insert(budget)
        else {
            db.budgetDao().insert(budget)
            budget.id
        }
    }

    // --------------------------------------------------------------------- misc

    private suspend fun audit(businessId: Long, type: String, id: Long?, action: String, text: String? = null) {
        db.auditDao().insert(
            AuditEventEntity(
                businessId = businessId,
                entityType = type,
                entityId = id,
                action = action,
                detail = text
            )
        )
    }
}
