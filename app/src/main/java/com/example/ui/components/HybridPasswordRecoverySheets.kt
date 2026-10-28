package com.example.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.example.data.model.UserSecuritySummary
import com.example.utils.SessionManager
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "HybridRecoveryAuth"

val PRESET_SECURITY_QUESTIONS = listOf(
  "İlkokul öğretmeninizin adı nedir?",
  "İlk evcil hayvanınızın adı nedir?",
  "Doğduğunuz mahalle veya sokak adı nedir?",
  "En sevdiğiniz çocukluk arkadaşınızın adı nedir?",
  "İlk kullandığınız aracın markası veya modeli nedir?"
)

data class VerifiedFirebaseIdentity(
  val uid: String,
  val email: String,
  val displayName: String?
)

suspend fun authenticateWithGoogleCredentialManager(
  context: Context,
  onCancelled: () -> Unit,
  onError: (String) -> Unit
): VerifiedFirebaseIdentity? {
  val credentialManager = CredentialManager.create(context)
  val webClientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    Log.e(TAG, "Missing default_web_client_id string resource", e)
    onError("Firebase yapılandırması (default_web_client_id) bulunamadı.")
    return null
  }

  // Strictly one option per GetCredentialRequest as required by API 36 / Credential Manager guidelines
  val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()

  return try {
    val result = credentialManager.getCredential(
      request = request,
      context = context
    )
    val credential = result.credential
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
      val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
      val user = authResult.user
      if (user != null && !user.email.isNullOrBlank()) {
        VerifiedFirebaseIdentity(
          uid = user.uid,
          email = user.email ?: "",
          displayName = user.displayName
        )
      } else {
        onError("Google hesabından e-posta bilgisi alınamadı.")
        null
      }
    } else {
      onError("Desteklenmeyen kimlik doğrulama türü.")
      null
    }
  } catch (e: GetCredentialCancellationException) {
    Log.w(TAG, "Google sign-in was cancelled by user or unconfigured SHA-1 fingerprint", e)
    onCancelled()
    null
  } catch (e: GetCredentialException) {
    Log.e(TAG, "Credential Manager error during Google sign-in", e)
    onError("Google hesabı doğrulaması başarısız oldu: ${e.localizedMessage ?: "Bilinmeyen hata"}")
    null
  } catch (e: Exception) {
    Log.e(TAG, "Firebase signInWithCredential failed", e)
    onError("Firebase doğrulama hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}")
    null
  }
}

@Composable
fun ForgotPasswordDialog(
  initialPhoneDigits: String,
  onDismiss: () -> Unit,
  onLoadSecuritySummary: suspend (String) -> UserSecuritySummary?,
  onResetWithFirebase: suspend (
    phone: String,
    verifiedEmail: String,
    firebaseUid: String,
    newPassword: String
  ) -> Pair<Boolean, String?>,
  onResetWithSecurityOrCode: suspend (
    phone: String,
    securityAnswer: String?,
    recoveryCode: String?,
    newPassword: String,
    selectedQuestionIfNew: String?
  ) -> Pair<Boolean, String?>,
  onPasswordResetSuccess: (phone: String, newPassword: String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val phoneVisualTransformation = remember { TurkishPhoneVisualTransformation() }

  var phoneDigits by remember { mutableStateOf(SessionManager.normalizePhone(initialPhoneDigits)) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Firebase / E-posta, 1: Güvenlik Sorusu / Kurtarma Kodu
  var useRecoveryCodeMode by remember { mutableStateOf(false) }

  var securitySummary by remember { mutableStateOf<UserSecuritySummary?>(null) }
  var isLoadingSummary by remember { mutableStateOf(false) }

  var verifiedIdentity by remember {
    val current = Firebase.auth.currentUser
    mutableStateOf(
      if (current != null && !current.email.isNullOrBlank()) {
        VerifiedFirebaseIdentity(current.uid, current.email ?: "", current.displayName)
      } else {
        null
      }
    )
  }

  var selectedQuestion by remember { mutableStateOf(PRESET_SECURITY_QUESTIONS.first()) }
  var securityAnswerInput by remember { mutableStateOf("") }
  var recoveryCodeInput by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var newPasswordVisible by remember { mutableStateOf(false) }

  var isSubmitting by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isStatusSuccess by remember { mutableStateOf(false) }

  LaunchedEffect(phoneDigits) {
    val clean = SessionManager.normalizePhone(phoneDigits)
    if (clean.length == 10) {
      isLoadingSummary = true
      val summary = onLoadSecuritySummary(clean)
      securitySummary = summary
      if (!summary?.securityQuestion.isNullOrBlank()) {
        selectedQuestion = summary?.securityQuestion ?: PRESET_SECURITY_QUESTIONS.first()
      }
      isLoadingSummary = false
    } else {
      securitySummary = null
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .widthIn(max = 500.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Hibrit Şifre Sıfırlama",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color(0xFF1C1C1E)
              )
              Text(
                text = "Firebase E-posta veya Güvenlik Sorusu / Kod ile sıfırlayın",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93)
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_forgot_password_dialog")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color(0xFF8E8E93))
          }
        }

        // Step 1: Phone Number
        OutlinedTextField(
          value = phoneDigits,
          onValueChange = {
            phoneDigits = SessionManager.normalizePhone(it)
            statusMessage = null
          },
          label = { Text("Kayıtlı Telefon Numaranız") },
          placeholder = { Text("(___) ___ __ __", color = Color(0xFFC7C7CC)) },
          leadingIcon = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(start = 12.dp, end = 4.dp)
            ) {
              Icon(
                Icons.Default.Phone,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("+90", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1C1E))
            }
          },
          visualTransformation = phoneVisualTransformation,
          textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1C1E)),
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_phone_input")
        )

        if (isLoadingSummary) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF007AFF)
          )
        }

        // Method Selector Tabs
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF2F2F7))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val tabs = listOf(
            "1. Firebase / E-posta",
            "2. Güvenlik Sorusu / Kod"
          )
          tabs.forEachIndexed { index, title ->
            val selected = selectedTab == index
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(11.dp))
                .clickable {
                  selectedTab = index
                  statusMessage = null
                }
                .testTag(if (index == 0) "tab_firebase_reset" else "tab_security_reset"),
              color = if (selected) Color.White else Color.Transparent,
              shadowElevation = if (selected) 2.dp else 0.dp,
              shape = RoundedCornerShape(11.dp)
            ) {
              Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFF007AFF) else Color(0xFF6C6C70),
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        if (selectedTab == 0) {
          // TAB 1: Firebase / Google E-posta Doğrulaması
          Surface(
            color = Color(0xFFF2F6FF),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Email,
                  contentDescription = null,
                  tint = Color(0xFF007AFF),
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Firebase Google E-posta Doğrulaması",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color(0xFF1C1C1E)
                )
              }
              val linkedEmail = securitySummary?.recoveryEmail
              Text(
                text = if (!linkedEmail.isNullOrBlank()) {
                  "Bu numara için kayıtlı kurtarma e-postası: $linkedEmail"
                } else {
                  "Google hesabınızla tek dokunuşta doğrulama yaparak MariaDB şifrenizi anında sıfırlayabilir ve e-postanızı hesabınıza bağlayabilirsiniz."
                },
                fontSize = 12.sp,
                color = Color(0xFF48484A),
                lineHeight = 16.sp
              )

              if (verifiedIdentity != null) {
                Surface(
                  color = Color(0xFFE8F8EE),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF28CD41),
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                        Text(
                          text = "Doğrulanan E-posta:",
                          fontSize = 11.sp,
                          color = Color(0xFF1C1C1E)
                        )
                        Text(
                          text = verifiedIdentity?.email ?: "",
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          color = Color(0xFF1E7E34)
                        )
                      }
                    }
                    TextButton(
                      onClick = {
                        Firebase.auth.signOut()
                        verifiedIdentity = null
                      }
                    ) {
                      Text("Değiştir", fontSize = 12.sp)
                    }
                  }
                }
              } else {
                OutlinedButton(
                  onClick = {
                    if (isSubmitting) return@OutlinedButton
                    isSubmitting = true
                    statusMessage = null
                    scope.launch {
                      val identity = authenticateWithGoogleCredentialManager(
                        context = context,
                        onCancelled = {
                          isSubmitting = false
                          isStatusSuccess = false
                          statusMessage = "Google oturum açma işlemi iptal edildi. Dilerseniz 2. sekmedeki Güvenlik Sorusu / Kurtarma Kodu ile de sıfırlayabilirsiniz."
                        },
                        onError = { err ->
                          isSubmitting = false
                          isStatusSuccess = false
                          statusMessage = err
                        }
                      )
                      if (identity != null) {
                        verifiedIdentity = identity
                      }
                      isSubmitting = false
                    }
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("firebase_google_verify_button"),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Google / E-posta ile Doğrula",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                  )
                }
              }
            }
          }
        } else {
          // TAB 2: Güvenlik Sorusu / Kurtarma Kodu
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = !useRecoveryCodeMode,
              onClick = {
                useRecoveryCodeMode = false
                statusMessage = null
              },
              label = { Text("Güvenlik Sorusu", fontSize = 12.sp) },
              leadingIcon = {
                Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = useRecoveryCodeMode,
              onClick = {
                useRecoveryCodeMode = true
                statusMessage = null
              },
              label = { Text("Kurtarma Kodu", fontSize = 12.sp) },
              leadingIcon = {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.weight(1f)
            )
          }

          if (!useRecoveryCodeMode) {
            val savedQuestion = securitySummary?.securityQuestion
            Surface(
              color = Color(0xFFF7F7FA),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = if (!savedQuestion.isNullOrBlank()) "Kayıtlı Güvenlik Sorunuz:" else "Güvenlik Sorusu Seçin:",
                  fontSize = 11.sp,
                  color = Color(0xFF8E8E93),
                  fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = savedQuestion ?: selectedQuestion,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF1C1C1E)
                )
                if (savedQuestion.isNullOrBlank() && securitySummary != null && !securitySummary!!.hasSecurityAnswer) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Not: Bu hesapta daha önce güvenlik sorusu tanımlanmamış. Aşağıdan bir soru seçip cevabınızı girerek hem şifrenizi sıfırlayabilir hem de sorunuzu kaydedebilirsiniz.",
                    fontSize = 11.sp,
                    color = Color(0xFF007AFF)
                  )
                }
              }
            }

            OutlinedTextField(
              value = securityAnswerInput,
              onValueChange = {
                securityAnswerInput = it
                statusMessage = null
              },
              label = { Text("Güvenlik Sorusu Cevabınız") },
              placeholder = { Text("Cevabınızı yazınız") },
              leadingIcon = {
                Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Color(0xFF5856D6))
              },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("security_answer_input")
            )
          } else {
            val codeHint = securitySummary?.recoveryCodeHint
            if (!codeHint.isNullOrBlank()) {
              Text(
                text = "Kayıtlı Kurtarma Kodu İpucu: $codeHint",
                fontSize = 12.sp,
                color = Color(0xFF5856D6),
                fontWeight = FontWeight.SemiBold
              )
            }
            OutlinedTextField(
              value = recoveryCodeInput,
              onValueChange = {
                recoveryCodeInput = it.uppercase()
                statusMessage = null
              },
              label = { Text("Yedek Kurtarma Kodu") },
              placeholder = { Text("Örn: ECZA-4829") },
              leadingIcon = {
                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFFF9500))
              },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("recovery_code_input")
            )
          }
        }

        // New Password Input
        OutlinedTextField(
          value = newPassword,
          onValueChange = {
            newPassword = it
            statusMessage = null
          },
          label = { Text("Yeni Şifreniz (En az 4 karakter)") },
          leadingIcon = {
            Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF00C7BE))
          },
          trailingIcon = {
            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
              Icon(
                imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (newPasswordVisible) "Gizle" else "Göster"
              )
            }
          },
          visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_new_password_input")
        )

        if (statusMessage != null) {
          Surface(
            color = if (isStatusSuccess) Color(0xFFE8F8EE) else MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = statusMessage ?: "",
              color = if (isStatusSuccess) Color(0xFF1E7E34) else MaterialTheme.colorScheme.onErrorContainer,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        // Submit Reset Button
        Button(
          onClick = {
            val cleanPhone = SessionManager.normalizePhone(phoneDigits)
            if (cleanPhone.length != 10) {
              isStatusSuccess = false
              statusMessage = "Lütfen başında 0 olmadan 10 haneli telefon numaranızı giriniz."
              return@Button
            }
            if (newPassword.trim().length < 4) {
              isStatusSuccess = false
              statusMessage = "Yeni şifreniz en az 4 karakter olmalıdır."
              return@Button
            }

            scope.launch {
              isSubmitting = true
              statusMessage = null
              if (selectedTab == 0) {
                val identity = verifiedIdentity
                if (identity == null) {
                  isSubmitting = false
                  isStatusSuccess = false
                  statusMessage = "Lütfen önce 'Google / E-posta ile Doğrula' butonuna dokunarak e-postanızı doğrulayın."
                  return@launch
                }
                val (ok, msg) = onResetWithFirebase(
                  cleanPhone,
                  identity.email,
                  identity.uid,
                  newPassword.trim()
                )
                isSubmitting = false
                isStatusSuccess = ok
                statusMessage = msg
                if (ok) {
                  onPasswordResetSuccess(cleanPhone, newPassword.trim())
                }
              } else {
                if (!useRecoveryCodeMode && securityAnswerInput.isBlank()) {
                  isSubmitting = false
                  isStatusSuccess = false
                  statusMessage = "Lütfen güvenlik sorusu cevabınızı giriniz."
                  return@launch
                }
                if (useRecoveryCodeMode && recoveryCodeInput.isBlank()) {
                  isSubmitting = false
                  isStatusSuccess = false
                  statusMessage = "Lütfen yedek kurtarma kodunuzu giriniz."
                  return@launch
                }
                val (ok, msg) = onResetWithSecurityOrCode(
                  cleanPhone,
                  if (!useRecoveryCodeMode) securityAnswerInput.trim() else null,
                  if (useRecoveryCodeMode) recoveryCodeInput.trim() else null,
                  newPassword.trim(),
                  selectedQuestion
                )
                isSubmitting = false
                isStatusSuccess = ok
                statusMessage = msg
                if (ok) {
                  onPasswordResetSuccess(cleanPhone, newPassword.trim())
                }
              }
            }
          },
          enabled = !isSubmitting,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .pressClickEffect()
            .testTag("submit_password_reset_button")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = Color.White,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("İşleniyor...", color = Color.White, fontWeight = FontWeight.Bold)
          } else {
            Text(
              text = "Şifreyi Sıfırla ve Giriş Yap",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun SecuritySettingsDialog(
  userPhone: String,
  initialSummary: UserSecuritySummary?,
  onDismiss: () -> Unit,
  onSaveRecoverySettings: suspend (
    recoveryEmail: String?,
    firebaseUid: String?,
    securityQuestion: String?,
    securityAnswerRaw: String?,
    rawRecoveryCode: String?
  ) -> Pair<Boolean, String?>
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var verifiedEmail by remember {
    mutableStateOf(initialSummary?.recoveryEmail ?: Firebase.auth.currentUser?.email)
  }
  var verifiedUid by remember {
    mutableStateOf(initialSummary?.firebaseUid ?: Firebase.auth.currentUser?.uid)
  }
  var selectedQuestion by remember {
    mutableStateOf(initialSummary?.securityQuestion ?: PRESET_SECURITY_QUESTIONS.first())
  }
  var securityAnswer by remember { mutableStateOf("") }
  var generatedRecoveryCode by remember { mutableStateOf<String?>(null) }

  var isSaving by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isSuccess by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .widthIn(max = 500.dp)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF5856D6).copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF5856D6),
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Hibrit Şifre Kurtarma Ayarları",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = Color(0xFF1C1C1E)
              )
              Text(
                text = "+90 ${SessionManager.formatDisplayPhone(userPhone)}",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93)
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color(0xFF8E8E93))
          }
        }

        // Section 1: Firebase / Google Email Link
        Surface(
          color = Color(0xFFF2F6FF),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "1. Firebase / Google E-posta Doğrulaması",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1C1C1E)
              )
            }
            Text(
              text = if (!verifiedEmail.isNullOrBlank()) {
                "Bağlı E-posta: $verifiedEmail (Firestore & MariaDB senkronize)"
              } else {
                "Google e-postanızı bağlayarak şifrenizi unuttuğunuzda tek dokunuşla sıfırlayabilirsiniz."
              },
              fontSize = 12.sp,
              color = Color(0xFF48484A)
            )
            OutlinedButton(
              onClick = {
                if (isSaving) return@OutlinedButton
                isSaving = true
                statusMessage = null
                scope.launch {
                  val identity = authenticateWithGoogleCredentialManager(
                    context = context,
                    onCancelled = {
                      isSaving = false
                      isSuccess = false
                      statusMessage = "Google doğrulaması iptal edildi."
                    },
                    onError = { err ->
                      isSaving = false
                      isSuccess = false
                      statusMessage = err
                    }
                  )
                  if (identity != null) {
                    verifiedEmail = identity.email
                    verifiedUid = identity.uid
                    isSuccess = true
                    statusMessage = "Google hesabınız (${identity.email}) doğrulandı! 'Ayarları Kaydet' butonuna basarak tamamlayın."
                  }
                  isSaving = false
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("link_google_account_button")
            ) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (!verifiedEmail.isNullOrBlank()) "Google Hesabını Güncelle ($verifiedEmail)" else "Google / E-posta Hesabımı Bağla",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF007AFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Section 2: Security Question & Answer
        Surface(
          color = Color(0xFFF7F7FA),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Color(0xFF5856D6), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "2. Güvenlik Sorusu ve Cevabı",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1C1C1E)
              )
            }
            Text(
              text = "Bir güvenlik sorusu seçin ve yalnızca sizin bildiğiniz cevabı belirleyin:",
              fontSize = 12.sp,
              color = Color(0xFF6C6C70)
            )
            PRESET_SECURITY_QUESTIONS.forEach { q ->
              val isSelected = selectedQuestion == q
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { selectedQuestion = q },
                color = if (isSelected) Color(0xFF5856D6).copy(alpha = 0.12f) else Color.White,
                shape = RoundedCornerShape(10.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  RadioButton(
                    selected = isSelected,
                    onClick = { selectedQuestion = q },
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = q,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = Color(0xFF1C1C1E)
                  )
                }
              }
            }

            OutlinedTextField(
              value = securityAnswer,
              onValueChange = { securityAnswer = it },
              label = {
                Text(
                  if (initialSummary?.hasSecurityAnswer == true) {
                    "Yeni Güvenlik Cevabı (Mevcut cevabı değiştirmek için yazın)"
                  } else {
                    "Güvenlik Sorusu Cevabınız"
                  }
                )
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_security_answer_input")
            )
          }
        }

        // Section 3: Backup Recovery Code
        Surface(
          color = Color(0xFFFFF8EE),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "3. Yedek Kurtarma Kodu",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = Color(0xFF1C1C1E)
                )
              }
              TextButton(
                onClick = {
                  generatedRecoveryCode = SessionManager.generateRecoveryCode()
                },
                modifier = Modifier.testTag("generate_recovery_code_button")
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kod Üret", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }

            if (generatedRecoveryCode != null) {
              Surface(
                color = Color.White,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "Yedek Kurtarma Kodunuz (Lütfen Not Edin):",
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93)
                  )
                  Text(
                    text = generatedRecoveryCode ?: "",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF9500),
                    letterSpacing = 1.5.sp
                  )
                }
              }
            } else if (!initialSummary?.recoveryCodeHint.isNullOrBlank()) {
              Text(
                text = "Aktif Kurtarma Kodu: ${initialSummary?.recoveryCodeHint}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6C6C70)
              )
            } else {
              Text(
                text = "Acil durumlar için tek kullanımlık/yedek kurtarma kodu üretebilirsiniz.",
                fontSize = 12.sp,
                color = Color(0xFF6C6C70)
              )
            }
          }
        }

        if (statusMessage != null) {
          Surface(
            color = if (isSuccess) Color(0xFFE8F8EE) else MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = statusMessage ?: "",
              color = if (isSuccess) Color(0xFF1E7E34) else MaterialTheme.colorScheme.onErrorContainer,
              fontSize = 13.sp,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        Button(
          onClick = {
            scope.launch {
              isSaving = true
              val (ok, msg) = onSaveRecoverySettings(
                verifiedEmail,
                verifiedUid,
                selectedQuestion,
                securityAnswer.takeIf { it.isNotBlank() },
                generatedRecoveryCode
              )
              isSaving = false
              isSuccess = ok
              statusMessage = msg
            }
          },
          enabled = !isSaving,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6)),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .pressClickEffect()
            .testTag("save_recovery_settings_button")
        ) {
          if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kaydediliyor...", color = Color.White, fontWeight = FontWeight.Bold)
          } else {
            Text("Kurtarma Ayarlarını Kaydet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }
    }
  }
}
