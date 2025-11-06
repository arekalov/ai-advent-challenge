package com.arekalov.aiadventchallenge.wear.presentation

import android.app.Application
import com.arekalov.aiadventchallenge.wear.presentation.di.DaggerWearAppComponent
import com.arekalov.aiadventchallenge.wear.presentation.di.WearAppComponent

class AiAdventWearApplication : Application() {

    lateinit var appComponent: WearAppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerWearAppComponent.factory().create(this)
    }
}

