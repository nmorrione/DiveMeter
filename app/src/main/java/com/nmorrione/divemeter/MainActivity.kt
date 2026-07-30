package com.nmorrione.divemeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nmorrione.divemeter.data.UserPreferences
import com.nmorrione.divemeter.ui.navigation.DiveMeterNavHost
import com.nmorrione.divemeter.ui.onboarding.NicknameOnboardingScreen
import com.nmorrione.divemeter.ui.theme.DiveMeterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiveMeterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    var nickname by remember { mutableStateOf(UserPreferences.getNickname(context)) }
                    if (nickname.isNullOrBlank()) {
                        NicknameOnboardingScreen(onSave = {
                            UserPreferences.setNickname(context, it)
                            nickname = it
                        })
                    } else {
                        DiveMeterNavHost()
                    }
                }
            }
        }
    }
}
