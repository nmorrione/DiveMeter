package com.nmorrione.divemeter.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.data.DiveRepository
import com.nmorrione.divemeter.data.NicknameResult
import com.nmorrione.divemeter.data.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nickname by remember { mutableStateOf(UserPreferences.getNickname(context) ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val takenMessage = stringResource(R.string.onboarding_nickname_taken)

    fun submit() {
        val trimmed = nickname.trim()
        if (trimmed.isEmpty()) return
        isSaving = true
        errorMessage = null
        scope.launch {
            when (val result = DiveRepository.updateNickname(trimmed)) {
                is NicknameResult.Success -> {
                    UserPreferences.setNickname(context, trimmed)
                    onNavigateBack()
                }
                is NicknameResult.AlreadyTaken -> {
                    isSaving = false
                    errorMessage = takenMessage
                }
                is NicknameResult.Error -> {
                    isSaving = false
                    errorMessage = result.message
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.settings_nickname_label)) },
                singleLine = true,
                isError = errorMessage != null,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Button(
                onClick = { submit() },
                enabled = nickname.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.settings_save))
                }
            }
        }
    }
}
