package com.nmorrione.divemeter

import android.app.Application
import android.content.Context
import org.osmdroid.config.Configuration

class DiveMeterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            load(this@DiveMeterApp, getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
            userAgentValue = packageName
        }
    }
}
