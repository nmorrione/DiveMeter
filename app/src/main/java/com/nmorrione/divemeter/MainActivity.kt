package com.nmorrione.divemeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nmorrione.divemeter.ui.navigation.DiveMeterNavHost
import com.nmorrione.divemeter.ui.theme.DiveMeterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiveMeterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DiveMeterNavHost()
                }
            }
        }
    }
}
