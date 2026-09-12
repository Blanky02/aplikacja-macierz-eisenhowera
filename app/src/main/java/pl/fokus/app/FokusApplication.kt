package pl.fokus.app

import android.app.Application
import pl.fokus.app.data.FocusDatabase
import pl.fokus.app.data.TaskRepository
import pl.fokus.app.notifications.NotificationHelper

class FokusApplication : Application() {
    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        repository = TaskRepository(this, FocusDatabase.get(this).taskDao())
    }
}
