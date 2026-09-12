package pl.fokus.app

import android.app.Application
import pl.fokus.app.data.FinanceDatabase
import pl.fokus.app.data.FinanceRepository

class FokusApplication : Application() {
    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FinanceRepository(this, FinanceDatabase.get(this).financeDao())
    }
}
