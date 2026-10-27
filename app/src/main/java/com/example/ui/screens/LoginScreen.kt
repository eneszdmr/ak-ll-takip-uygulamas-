package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DatabaseConnectionTester
import com.example.utils.NetworkMonitor

@Composable
fun LoginScreen(
  isLoading: Boolean,
  externalError: String?,
  onLoginClicked: (phone: String, password: String) -> Unit
) {
  val context = LocalContext.current
  var phoneNumber by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var localError by remember { mutableStateOf<String?>(null) }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Compact DB connection status pill or offline warning
      DatabaseConnectionTester(modifier = Modifier.padding(bottom = 24.dp))

      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Phone,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(34.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Hoş Geldiniz",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "İlk girişte telefon numaranız ve şifreniz güvenli (SHA-256) olarak tanımlanır. Sonraki girişlerde şifre sormadan doğrudan açılır.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(28.dp))

      OutlinedTextField(
        value = phoneNumber,
        onValueChange = { phoneNumber = it },
        label = { Text("Telefon Numarası (örn: 05551234567)") },
        leadingIcon = {
          Icon(Icons.Default.Phone, contentDescription = null)
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("phone_input"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Şifre") },
        leadingIcon = {
          Icon(Icons.Default.Lock, contentDescription = null)
        },
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster"
            )
          }
        },
        visualTransformation = if (passwordVisible) {
          VisualTransformation.None
        } else {
          PasswordVisualTransformation()
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("password_input"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp)
      )

      val displayError = localError ?: externalError
      if (displayError != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = displayError,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          if (!NetworkMonitor.isInternetAvailable(context)) {
            localError = "İnternet bağlantınız kapalı! Giriş yapmak ve bilgilerin gelmesi için internet bağlantısı gereklidir."
            return@Button
          }
          val cleanPhone = phoneNumber.trim()
          if (cleanPhone.length < 10) {
            localError = "Lütfen geçerli bir telefon numarası girin (en az 10 hane)."
            return@Button
          }
          if (password.trim().length < 4) {
            localError = "Lütfen en az 4 karakterli bir şifre belirleyin/girin."
            return@Button
          }
          localError = null
          onLoginClicked(cleanPhone, password.trim())
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("login_button"),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.onPrimary
          )
        } else {
          Text("Giriş Yap / Kayıt Ol", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
