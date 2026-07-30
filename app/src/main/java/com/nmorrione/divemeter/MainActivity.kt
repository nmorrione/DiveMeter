package com.nmorrione.divemeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nmorrione.divemeter.data.DiveRepository
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
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    var signInAttempt by remember { mutableIntStateOf(0) }
    var isSignedIn by remember { mutableStateOf(false) }
    var signInFailed by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf(UserPreferences.getNickname(context)) }

    LaunchedEffect(signInAttempt) {
        signInFailed = false
        try {
            DiveRepository.ensureSignedIn()
            isSignedIn = true
        } catch (e: Exception) {
            signInFailed = true
        }
    }

    when {
        signInFailed -> Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.sign_in_failed))
                Button(
                    onClick = { signInAttempt++ },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
        !isSignedIn -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        nickname.isNullOrBlank() -> NicknameOnboardingScreen(onSaved = {
            UserPreferences.setNickname(context, it)
            nickname = it
        })
        else -> DiveMeterNavHost()
    }
}
