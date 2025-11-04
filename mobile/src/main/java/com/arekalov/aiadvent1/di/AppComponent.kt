package com.arekalov.aiadvent1.di

import android.content.Context
import com.arekalov.aiadvent1.core.di.AppScope
import com.arekalov.aiadvent1.data.di.DataModule
import dagger.BindsInstance
import dagger.Component

@AppScope
@Component(
    modules = [
        DataModule::class,
        ViewModelModule::class
    ]
)
interface AppComponent {
    
    fun viewModelFactory(): ViewModelFactory

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}

