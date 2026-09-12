package pl.fokus.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IncomeEntity::class, ExpenseEntity::class, AllocationEntity::class, TransferEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var instance: FinanceDatabase? = null

        fun get(context: Context): FinanceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FinanceDatabase::class.java,
                "fokus-finanse.db",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
