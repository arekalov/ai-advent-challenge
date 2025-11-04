package com.arekalov.aiadvent1.wear.presentation

import android.app.Application
import com.arekalov.aiadvent1.wear.presentation.di.WearAppComponent
import com.arekalov.aiadvent1.wear.presentation.di.DaggerWearAppComponent

class AiAdventWearApplication : Application() {

    lateinit var appComponent: WearAppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerWearAppComponent.factory().create(this)
    }
}

