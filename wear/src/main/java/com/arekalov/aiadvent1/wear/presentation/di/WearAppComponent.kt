package com.arekalov.aiadvent1.wear.presentation.di

import android.content.Context
import com.arekalov.aiadvent1.core.di.AppScope
import com.arekalov.aiadvent1.data.di.DataModule
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

