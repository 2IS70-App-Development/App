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
 *
 * This screen provides a unified interface for both logging in and signing up. 
 * It manages its own local state for form fields and UI feedback.
 *
 * @param onLoginSuccess Callback invoked when the user successfully authenticates 
 * and is ready to proceed to the main application dashboard.
 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    // Local state for the email/username text field.
    var email by remember { mutableStateOf("") }

    // Local state for the password text field.
    var password by remember { mutableStateOf("") }

    // Tracks if a network request (Login or Signup) is currently in flight.
    var isLoading by remember { mutableStateOf(false) }

    // Holds an error message string to display to the user if an operation fails.
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Toggle state between Login mode (false) and Sign Up mode (true).
    var isSignup by remember { mutableStateOf(false) }

    // Coroutine scope for launching asynchronous network requests from UI events.
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Branding / Hero Text
        Text(
            text = "CryptoSeal",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Email/Username Input Field.
        // Clears any previous error message when the user starts typing.
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Input Field.
        // Uses PasswordVisualTransformation to hide the characters.
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

        // Conditional display of error messages if they exist.
        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Show a loading spinner while waiting for the server, or the action buttons otherwise.
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            // Main Action Button (Triggers Login or Sign Up process).
            Button(
                onClick = {
                    // Simple client-side validation for empty fields.
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        val result = if (isSignup) {
                            // Signup Flow: Create the account, then immediately try to log in to get a token.
                            ApiService.signup(email, password).fold(
                                onSuccess = { ApiService.login(email, password) },
                                onFailure = { Result.failure(it) }
                            )
                        } else {
                            // Standard Login Flow.
                            ApiService.login(email, password)
                        }
                        
                        result.fold(
                            onSuccess = {
                                // On success, navigate to the Dashboard.
                                onLoginSuccess()
                            },
                            onFailure = {
                                // On failure, stop loading and show the error returned by the API.
                                errorMessage = it.message
                            }
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

            // Secondary button to toggle between Login and Sign Up UI modes.
            OutlinedButton(
                onClick = { isSignup = !isSignup },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignup) "Switch to Login" else "Switch to Sign Up")
            }
        }
    }
}
