package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.LockReset
import com.example.data.model.UserSecuritySummary
import com.example.ui.components.AppBrandIconBadge
import com.example.ui.components.DatabaseConnectionTester
import com.example.ui.components.ForgotPasswordDialog
import com.example.ui.components.TurkishPhoneVisualTransformation
import com.example.ui.components.pressClickEffect
import com.example.utils.NetworkMonitor
import com.example.utils.SessionManager

@Composable
fun LoginScreen(
  isLoading: Boolean,
  externalError: String?,
  onLoginClicked: (phone: String, password: String) -> Unit,
  onLoadSecuritySummary: suspend (String) -> UserSecuritySummary? = { null },
  onResetWithFirebase: suspend (
    phone: String,
    verifiedEmail: String,
    firebaseUid: String,
    newPassword: String
  ) -> Pair<Boolean, String?> = { _, _, _, _ -> false to null },
  onResetWithSecurityOrCode: suspend (
    phone: String,
    securityAnswer: String?,
    recoveryCode: String?,
    newPassword: String,
    selectedQuestionIfNew: String?
  ) -> Pair<Boolean, String?> = { _, _, _, _, _ -> false to null }
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  var rawPhoneDigits by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var localError by remember { mutableStateOf<String?>(null) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }

  val phoneVisualTransformation = remember { TurkishPhoneVisualTransformation() }

  fun submitLogin() {
    focusManager.clearFocus()
    if (!NetworkMonitor.isInternetAvailable(context)) {
      localError = "İnternet bağlantınız kapalı! Giriş yapmak ve bilgilerin gelmesi için internet bağlantısı gereklidir."
      return
    }
    val cleanPhone = SessionManager.normalizePhone(rawPhoneDigits)
    if (cleanPhone.length != 10) {
      localError = "Lütfen telefon numaranızı başında 0 olmadan eksiksiz 10 hane olarak giriniz."
      return
    }
    if (password.trim().length < 4) {
      localError = "Lütfen en az 4 karakterli bir şifre giriniz."
      return
    }
    localError = null
    onLoginClicked(cleanPhone, password.trim())
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = Color(0xFFF2F2F7)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .imePadding(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 460.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        DatabaseConnectionTester(modifier = Modifier.padding(bottom = 20.dp))

        AppBrandIconBadge(size = 84.dp, cornerRadius = 22.dp)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Akıllı Takip Merkezi",
          fontSize = 26.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color(0xFF1C1C1E),
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "İlk girişte telefon numaranız ve şifreniz güvenli (SHA-256) olarak tanımlanır. Sonraki girişlerde şifre sormadan doğrudan açılır.",
          fontSize = 13.sp,
          color = Color(0xFF6C6C70),
          textAlign = TextAlign.Center,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            OutlinedTextField(
              value = rawPhoneDigits,
              onValueChange = { input ->
                rawPhoneDigits = SessionManager.normalizePhone(input)
                if (localError != null) localError = null
              },
              label = { Text("Telefon Numarası") },
              placeholder = { Text("(___) ___ __ __", color = Color(0xFFC7C7CC)) },
              supportingText = {
                Text(
                  text = "Başında 0 olmadan 10 haneli giriniz (${rawPhoneDigits.length}/10)",
                  fontSize = 11.sp
                )
              },
              leadingIcon = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                ) {
                  Icon(
                    Icons.Default.Phone,
                    contentDescription = "Telefon İkonu",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "+90",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1C1C1E)
                  )
                }
              },
              visualTransformation = phoneVisualTransformation,
              textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                letterSpacing = 0.5.sp
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_input")
                .semantics { contentDescription = "Telefon Numarası Giriş Alanı" },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Next
              ),
              shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
              value = password,
              onValueChange = {
                password = it
                if (localError != null) localError = null
              },
              label = { Text("Şifre") },
              leadingIcon = {
                Icon(
                  Icons.Default.Lock,
                  contentDescription = "Şifre İkonu",
                  tint = Color(0xFF007AFF)
                )
              },
              trailingIcon = {
                IconButton(
                  onClick = { passwordVisible = !passwordVisible },
                  modifier = Modifier.semantics {
                    contentDescription = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster"
                  }
                ) {
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
              textStyle = TextStyle(
                fontSize = 16.sp,
                color = Color(0xFF1C1C1E)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input")
                .semantics { contentDescription = "Şifre Giriş Alanı" },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(onDone = { submitLogin() }),
              shape = RoundedCornerShape(14.dp)
            )

            val displayError = localError ?: externalError
            if (displayError != null) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
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

            Button(
              onClick = { submitLogin() },
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pressClickEffect()
                .testTag("login_button")
                .semantics { contentDescription = "Giriş Yap veya Kayıt Ol Butonu" },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
              enabled = !isLoading
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = Color.White,
                  strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Bağlanıyor...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
              } else {
                Text("Giriş Yap / Kayıt Ol", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }

            HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)

            TextButton(
              onClick = { showForgotPasswordDialog = true },
              modifier = Modifier
                .fillMaxWidth()
                .pressClickEffect()
                .testTag("forgot_password_button")
                .semantics { contentDescription = "Şifremi Unuttum - Hibrit Şifre Sıfırlama" }
            ) {
              Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = Color(0xFF5856D6),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Şifremi Unuttum (Firebase E-posta / Güvenlik Sorusu)",
                color = Color(0xFF5856D6),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
          initialPhoneDigits = rawPhoneDigits,
          onDismiss = { showForgotPasswordDialog = false },
          onLoadSecuritySummary = onLoadSecuritySummary,
          onResetWithFirebase = onResetWithFirebase,
          onResetWithSecurityOrCode = onResetWithSecurityOrCode,
          onPasswordResetSuccess = { resetPhone, resetPass ->
            showForgotPasswordDialog = false
            rawPhoneDigits = resetPhone
            password = resetPass
            onLoginClicked(resetPhone, resetPass)
          }
        )
      }
    }
  }
}
