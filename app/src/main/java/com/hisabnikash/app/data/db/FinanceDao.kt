package com.hisabnikash.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class AccountBalance(
    @Embedded val account: AccountEntity,
    val balanceMinor: Long = 0
)

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE businessId = :businessId ORDER BY createdAt")
    suspend fun listAll(businessId: Long): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE businessId = :businessId ORDER BY createdAt")
    fun observeAll(businessId: Long): Flow<List<AccountEntity>>

    @Query(
        """
        SELECT a.*, a.openingBalanceMinor + COALESCE(
            (SELECT SUM(CASE WHEN t.direction = 'IN' THEN t.amountMinor ELSE -t.amountMinor END)
             FROM account_transactions t WHERE t.accountId = a.id), 0
        ) AS balanceMinor
        FROM accounts a
        WHERE a.businessId = :businessId
        ORDER BY a.createdAt
        """
    )
    fun observeBalances(businessId: Long): Flow<List<AccountBalance>>

    @Query(
        "SELECT COALESCE(SUM(openingBalanceMinor), 0) FROM accounts WHERE businessId = :businessId AND archived = 0"
    )
    suspend fun totalOpening(businessId: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(a.openingBalanceMinor +
            (SELECT COALESCE(SUM(CASE WHEN t.direction = 'IN' THEN t.amountMinor ELSE -t.amountMinor END), 0)
             FROM account_transactions t WHERE t.accountId = a.id)), 0)
        FROM accounts a WHERE a.businessId = :businessId AND a.archived = 0
        """
    )
    fun observeTotalBalance(businessId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)
}

@Dao
interface LedgerDao {

    @Insert
    suspend fun insert(transaction: AccountTransactionEntity): Long

    @Query("SELECT * FROM account_transactions WHERE id = :id")
    suspend fun getById(id: Long): AccountTransactionEntity?

    @Query("DELETE FROM account_transactions WHERE businessId = :businessId AND refType = :refType AND refId = :refId")
    suspend fun deleteForRef(businessId: Long, refType: String, refId: Long)

    @Query(
        """
        SELECT * FROM account_transactions
        WHERE businessId = :businessId AND (:filter = 'ALL' OR direction = :filter)
        ORDER BY dateAt DESC, id DESC
        LIMIT 1000
        """
    )
    fun observe(businessId: Long, filter: String): Flow<List<AccountTransactionEntity>>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN direction = 'IN' THEN amountMinor ELSE -amountMinor END), 0) " +
            "FROM account_transactions WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt"
    )
    suspend fun netInRange(businessId: Long, fromAt: Long, toAt: Long): Long

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN direction = 'IN' THEN amountMinor ELSE -amountMinor END), 0) " +
            "FROM account_transactions WHERE businessId = :businessId"
    )
    suspend fun netAllTime(businessId: Long): Long

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN direction = 'IN' THEN amountMinor ELSE 0 END), 0) " +
            "FROM account_transactions WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt"
    )
    suspend fun inflowInRange(businessId: Long, fromAt: Long, toAt: Long): Long

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN direction = 'OUT' THEN amountMinor ELSE 0 END), 0) " +
            "FROM account_transactions WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt"
    )
    suspend fun outflowInRange(businessId: Long, fromAt: Long, toAt: Long): Long

    @Query(
        """
        SELECT * FROM account_transactions
        WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt
        ORDER BY dateAt ASC
        """
    )
    suspend fun inRange(businessId: Long, fromAt: Long, toAt: Long): List<AccountTransactionEntity>

    @Query(
        """
        SELECT * FROM account_transactions
        WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt
        ORDER BY dateAt ASC
        """
    )
    fun observeInRange(businessId: Long, fromAt: Long, toAt: Long): Flow<List<AccountTransactionEntity>>

    @Query("SELECT * FROM account_transactions WHERE businessId = :businessId AND (note LIKE '%' || :query || '%' COLLATE NOCASE OR category LIKE '%' || :query || '%' COLLATE NOCASE) ORDER BY dateAt DESC LIMIT 200")
    suspend fun search(businessId: Long, query: String): List<AccountTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<AccountTransactionEntity>)

    @Query("DELETE FROM account_transactions WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)
}

@Dao
interface TransferDao {

    @Insert
    suspend fun insert(transfer: TransferEntity): Long

    @Query("SELECT * FROM transfers WHERE businessId = :businessId ORDER BY dateAt DESC, id DESC LIMIT 500")
    fun observeAll(businessId: Long): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transfers: List<TransferEntity>)

    @Query("DELETE FROM transfers WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)
}

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query(
        """
        SELECT * FROM expenses
        WHERE businessId = :businessId AND (:category = 'ALL' OR category = :category)
        ORDER BY dateAt DESC, id DESC LIMIT 1000
        """
    )
    fun observeFiltered(businessId: Long, category: String): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM expenses WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt"
    )
    suspend fun totalInRange(businessId: Long, fromAt: Long, toAt: Long): Long

    @Query(
        "SELECT * FROM expenses WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt ORDER BY dateAt ASC"
    )
    suspend fun inRange(businessId: Long, fromAt: Long, toAt: Long): List<ExpenseEntity>

    @Query(
        "SELECT * FROM expenses WHERE businessId = :businessId AND dateAt BETWEEN :fromAt AND :toAt ORDER BY dateAt ASC"
    )
    fun observeInRange(businessId: Long, fromAt: Long, toAt: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND vendor LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY dateAt DESC LIMIT 200")
    suspend fun search(businessId: Long, query: String): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE businessId = :businessId")
    suspend fun deleteAllForBusiness(businessId: Long)
}

@Dao
interface ReceivablePayableDao {

    @Insert
    suspend fun insertReceivable(receivable: ReceivableEntity): Long

    @Update
    suspend fun updateReceivable(receivable: ReceivableEntity)

    @Query("SELECT * FROM receivables WHERE id = :id")
    suspend fun getReceivable(id: Long): ReceivableEntity?

    @Query("SELECT * FROM receivables WHERE businessId = :businessId AND sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun findReceivable(businessId: Long, sourceType: String, sourceId: Long): ReceivableEntity?

    @Query("SELECT * FROM receivables WHERE businessId = :businessId AND sourceType = 'COD' AND status != 'PAID' ORDER BY id ASC")
    suspend fun openCodReceivables(businessId: Long): List<ReceivableEntity>

    @Query("SELECT * FROM receivables WHERE businessId = :businessId AND status != 'PAID' ORDER BY createdAt DESC LIMIT 1000")
    suspend fun openReceivables(businessId: Long): List<ReceivableEntity>

    @Query(
        """
        SELECT * FROM receivables WHERE businessId = :businessId AND (:status = 'ALL' OR status = :status)
        ORDER BY createdAt DESC LIMIT 1000
        """
    )
    fun observeReceivables(businessId: Long, status: String): Flow<List<ReceivableEntity>>

    @Query(
        """
        SELECT * FROM receivables WHERE businessId = :businessId AND customerId = :customerId
        ORDER BY createdAt DESC LIMIT 500
        """
    )
    fun observeReceivablesForCustomer(businessId: Long, customerId: Long): Flow<List<ReceivableEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountMinor - paidMinor), 0) FROM receivables WHERE businessId = :businessId AND status IN ('PENDING','PARTIAL','OVERDUE')"
    )
    fun observeOutstanding(businessId: Long): Flow<Long>

    @Query("SELECT * FROM receivables WHERE businessId = :businessId AND sourceType = 'COD' ORDER BY id ASC")
    fun observeCodReceivables(businessId: Long): Flow<List<ReceivableEntity>>

    @Insert
    suspend fun insertPayable(payable: PayableEntity): Long

    @Update
    suspend fun updatePayable(payable: PayableEntity)

    @Query("SELECT * FROM payables WHERE id = :id")
    suspend fun getPayable(id: Long): PayableEntity?

    @Query(
        """
        SELECT * FROM payables WHERE businessId = :businessId AND (:status = 'ALL' OR status = :status)
        ORDER BY createdAt DESC LIMIT 1000
        """
    )
    fun observePayables(businessId: Long, status: String): Flow<List<PayableEntity>>

    @Query("SELECT * FROM payables WHERE businessId = :businessId AND supplierId = :supplierId ORDER BY createdAt DESC LIMIT 500")
    fun observePayablesForSupplier(businessId: Long, supplierId: Long): Flow<List<PayableEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountMinor - paidMinor), 0) FROM payables WHERE businessId = :businessId AND status IN ('PENDING','PARTIAL','OVERDUE')"
    )
    fun observePayablesOutstanding(businessId: Long): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountMinor - paidMinor), 0) FROM payables WHERE businessId = :businessId AND status IN ('PENDING','PARTIAL','OVERDUE')"
    )
    suspend fun outstandingPayablesMinor(businessId: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReceivables(receivables: List<ReceivableEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayables(payables: List<PayableEntity>)

    @Query("DELETE FROM receivables WHERE businessId = :businessId")
    suspend fun deleteAllReceivables(businessId: Long)

    @Query("DELETE FROM payables WHERE businessId = :businessId")
    suspend fun deleteAllPayables(businessId: Long)
}
