package com.arekalov.aiadventchallenge

import android.app.Application
import com.arekalov.aiadventchallenge.di.AppComponent
import com.arekalov.aiadventchallenge.di.DaggerAppComponent

class AiAdventApplication : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
    }
}

