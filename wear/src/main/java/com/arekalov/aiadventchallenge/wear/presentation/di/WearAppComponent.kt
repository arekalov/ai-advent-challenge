package com.arekalov.aiadventchallenge.wear.presentation.di

import android.content.Context
import com.arekalov.aiadventchallenge.core.di.AppScope
import com.arekalov.aiadventchallenge.data.di.DataModule
import dagger.BindsInstance
import dagger.Component

@AppScope
@Component(
    modules = [
        DataModule::class,
        WearViewModelModule::class
    ]
)
interface WearAppComponent {
    
    fun viewModelFactory(): WearViewModelFactory

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): WearAppComponent
    }
}

