package app.cryptoseal.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.cryptoseal.data.api.ApiService
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSignup by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CryptoSeal",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val result = if (isSignup) {
                            ApiService.signup(email, password).fold(
                                onSuccess = { ApiService.login(email, password) },
                                onFailure = { Result.failure(it) }
                            )
                        } else {
                            ApiService.login(email, password)
                        }
                        
                        result.fold(
                            onSuccess = { onLoginSuccess() },
                            onFailure = { errorMessage = it.message }
                        )
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank()
            ) {
                Text(if (isSignup) "Sign Up" else "Login")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { isSignup = !isSignup },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignup) "Switch to Login" else "Switch to Sign Up")
            }
        }
    }
}