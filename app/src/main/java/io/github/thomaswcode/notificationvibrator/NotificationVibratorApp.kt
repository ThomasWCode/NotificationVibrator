package io.github.thomaswcode.notificationvibrator

import android.app.Application
import android.content.Context

class NotificationVibratorApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as NotificationVibratorApp).container
