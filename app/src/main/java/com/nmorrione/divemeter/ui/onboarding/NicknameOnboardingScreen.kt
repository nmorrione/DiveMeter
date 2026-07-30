package com.nmorrione.divemeter.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.data.DiveRepository
import com.nmorrione.divemeter.data.NicknameResult
import kotlinx.coroutines.launch

@Composable
fun NicknameOnboardingScreen(onSaved: (String) -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val takenMessage = stringResource(R.string.onboarding_nickname_taken)

    fun submit() {
        val trimmed = nickname.trim()
        if (trimmed.isEmpty()) return
        isSaving = true
        errorMessage = null
        scope.launch {
            when (val result = DiveRepository.claimNickname(trimmed)) {
                is NicknameResult.Success -> onSaved(trimmed)
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

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.onboarding_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.onboarding_nickname_label)) },
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
                    Text(stringResource(R.string.onboarding_continue))
                }
            }
        }
    }
}
