package com.arekalov.aiadvent1

import android.app.Application
import com.arekalov.aiadvent1.di.AppComponent
import com.arekalov.aiadvent1.di.DaggerAppComponent

class AiAdventApplication : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
    }
}

