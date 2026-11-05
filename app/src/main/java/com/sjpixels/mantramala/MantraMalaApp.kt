package com.sjpixels.mantramala

import android.app.Application
import android.content.Context
import com.sjpixels.mantramala.data.CounterRepository
import com.sjpixels.mantramala.data.JapaDatabase
import com.sjpixels.mantramala.data.SettingsRepository
import com.sjpixels.mantramala.util.Feedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

/** Simple manual dependency container — the app has no server and no DI framework. */
class AppContainer(context: Context) {
    /** Lives as long as the process; used for work that must outlive a screen (saving counts, history). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database: JapaDatabase by lazy { JapaDatabase.create(context) }
    val settings: SettingsRepository by lazy { SettingsRepository(context) }
    val feedback: Feedback by lazy { Feedback(context) }
    val counter: CounterRepository by lazy {
        CounterRepository(appScope, settings, database.japaDao(), database.customMantraDao(), feedback)
    }

    /** True while an app screen is visible; the over-other-apps bead hides itself then. */
    val appVisible = MutableStateFlow(false)
}

class MantraMalaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
