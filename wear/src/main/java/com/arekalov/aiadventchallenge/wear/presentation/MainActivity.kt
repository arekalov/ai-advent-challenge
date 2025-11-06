package com.arekalov.aiadventchallenge.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.arekalov.aiadventchallenge.wear.presentation.chat.WearChatScreen
import com.arekalov.aiadventchallenge.wear.presentation.chat.WearChatViewModel
import com.arekalov.aiadventchallenge.wear.presentation.theme.Aiadvent1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        val appComponent = (application as AiAdventWearApplication).appComponent
        val viewModelFactory = appComponent.viewModelFactory()
        val viewModel = ViewModelProvider(this, viewModelFactory)[WearChatViewModel::class.java]

        setContent {
            Aiadvent1Theme {
                WearChatScreen(viewModel = viewModel)
            }
        }
    }
}