package com.lasertrac.app.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lasertrac.app.R
import com.lasertrac.app.util.SessionManager
import com.lasertrac.app.viewmodel.AuthViewModel
import com.lasertrac.app.viewmodel.LoginState
import kotlinx.coroutines.delay

private sealed class AuthScreenState {
    object Idle : AuthScreenState()
    data class AwaitingVerification(val email: String, val pass: String) : AuthScreenState()
}

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var screenState by remember { mutableStateOf<AuthScreenState>(AuthScreenState.Idle) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) }

    val loginState by authViewModel.loginState.collectAsState()
    val isOnline by authViewModel.isOnline.collectAsState()

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                if (state.message.contains("Verification required", ignoreCase = true) || state.message.contains("queued", ignoreCase = true)) {
                    screenState = AuthScreenState.AwaitingVerification(email, password)
                } else {
                    sessionManager.saveLoginSession()
                    onLoginSuccess()
                }
                authViewModel.resetLoginState() // Reset after handling
            }
            is LoginState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel.resetLoginState()
            }
            else -> { /* Handle Idle and Loading as needed, often by showing/hiding a spinner */ }
        }
    }

    if (screenState is AuthScreenState.AwaitingVerification) {
        val state = screenState as AuthScreenState.AwaitingVerification
        PendingVerificationDialog(
            email = state.email,
            onDismiss = { screenState = AuthScreenState.Idle },
            onCheckStatus = { authViewModel.checkRegistrationStatus(state.email) },
            isLoading = loginState is LoginState.Loading
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.dashboard_background_device),
            contentDescription = "Auth Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLoginMode) "Welcome Back" else "Create Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLoginMode) "Sign in to continue" else "Sign up to get started",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    if (!isLoginMode) {
                        ModernTextFieldWithIcon(
                            value = name,
                            onValueChange = { name = it },
                            label = "Name",
                            icon = Icons.Default.Person
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    ModernTextFieldWithIcon(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        icon = Icons.Default.Email
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ModernTextFieldWithIcon(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = !passwordVisible }
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    GradientButton(
                        text = if (isLoginMode) "Login" else "Register",
                        onClick = {
                            if (isLoginMode) {
                                authViewModel.login(email, password)
                            } else {
                                authViewModel.register(name, email, password)
                            }
                        },
                        enabled = loginState !is LoginState.Loading
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isLoginMode) "Don't have an account? Sign up" else "Already have an account? Sign in",
                        modifier = Modifier.clickable { isLoginMode = !isLoginMode },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (loginState is LoginState.Error) {
                        val errorState = loginState as LoginState.Error
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        ConnectionStatusIndicator(
            isOnline = isOnline,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        if (loginState is LoginState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun PendingVerificationDialog(
    email: String,
    onDismiss: () -> Unit,
    onCheckStatus: () -> Unit,
    isLoading: Boolean
) {
    LaunchedEffect(email) {
        while (true) {
            onCheckStatus()
            delay(15000) // Check every 15 seconds
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registration Pending") },
        text = { Text("Your registration for '$email' is awaiting admin approval. The app will check automatically.") },
        confirmButton = {
            Button(onClick = onCheckStatus, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Check Now")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ModernTextFieldWithIcon(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = label)
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            }
        }
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConnectionStatusIndicator(isOnline: Boolean, modifier: Modifier = Modifier) {
    val (color, text) = if (isOnline) {
        Color(0xFF2E7D32) to "Online"
    } else {
        Color(0xFFC62828) to "Offline"
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
