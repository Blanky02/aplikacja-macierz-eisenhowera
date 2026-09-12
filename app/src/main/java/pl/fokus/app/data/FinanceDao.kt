package pl.fokus.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM incomes WHERE receivedAt BETWEEN :startAt AND :endAt ORDER BY receivedAt DESC, id DESC")
    fun observeIncomes(startAt: Long, endAt: Long): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM expenses WHERE occurredAt BETWEEN :startAt AND :endAt ORDER BY occurredAt DESC, id DESC")
    fun observeExpenses(startAt: Long, endAt: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC, id DESC")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM allocations WHERE periodKey = :periodKey ORDER BY quadrantIndex")
    fun observeAllocations(periodKey: String): Flow<List<AllocationEntity>>

    @Query("SELECT * FROM allocations WHERE periodKey = :periodKey")
    suspend fun getAllocations(periodKey: String): List<AllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllocations(allocations: List<AllocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncome(id: Long)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)
}
