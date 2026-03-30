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

/**
 * The initial entry point screen for users to authenticate or register.
 * It provides fields for email and password and toggles between Login and Sign Up modes.
 *
 * @param onLoginSuccess Callback invoked when the user successfully authenticates.
 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    // State for user input and UI feedback
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Toggle state between Login mode and Sign Up mode
    var isSignup by remember { mutableStateOf(false) }

    // Coroutine scope for launching network requests from button clicks
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Branding
        Text(
            text = "CryptoSeal",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Email/Username Input Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Input Field with obfuscation
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

        // Conditional display of error messages
        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Show loading spinner or action buttons
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            // Main Action Button (Login or Sign Up)
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val result = if (isSignup) {
                            // If signing up, create account then immediately log in
                            ApiService.signup(email, password).fold(
                                onSuccess = { ApiService.login(email, password) },
                                onFailure = { Result.failure(it) }
                            )
                        } else {
                            // Standard login attempt
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

            // Secondary button to switch between Login and Sign Up modes
            OutlinedButton(
                onClick = { isSignup = !isSignup },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignup) "Switch to Login" else "Switch to Sign Up")
            }
        }
    }
}
