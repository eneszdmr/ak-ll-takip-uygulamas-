package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.RecoveryProfile
import com.example.utils.OperationType
import com.example.utils.SessionManager
import com.example.utils.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RecoveryProfileRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth
  )

  suspend fun saveOrUpdateProfile(
    phoneNumber: String,
    email: String,
    securityQuestion: String = "",
    recoveryCodeHint: String = ""
  ): Result<String> = runCatching {
    val uid = checkNotNull(auth.currentUser?.uid) { "Oturum açmış Firebase kullanıcısı bulunamadı" }
    val normalizedPhone = SessionManager.normalizePhone(phoneNumber)
    require(normalizedPhone.length >= 10) { "Telefon numarası 10 haneli olmalıdır" }
    require(email.trim().length >= 3) { "Geçerli bir e-posta adresi gereklidir" }

    val docRef = db.collection(COLLECTION_RECOVERY_PROFILES).document(uid)
    try {
      val existingSnap = docRef.get().await()
      if (existingSnap.exists()) {
        val existing = existingSnap.toObject(
          RecoveryProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        val createdAt = requireNotNull(existing?.createdAt) { "Mevcut profil createdAt alanı eksik" }
        val updatedModel = RecoveryProfile(
          id = uid,
          userId = uid,
          phoneNumber = normalizedPhone,
          email = email.trim(),
          securityQuestion = securityQuestion.trim(),
          recoveryCodeHint = recoveryCodeHint.trim(),
          createdAt = createdAt,
          updatedAt = null
        )
        docRef.set(updatedModel).await()
      } else {
        val newModel = RecoveryProfile(
          id = uid,
          userId = uid,
          phoneNumber = normalizedPhone,
          email = email.trim(),
          securityQuestion = securityQuestion.trim(),
          recoveryCodeHint = recoveryCodeHint.trim(),
          createdAt = null,
          updatedAt = null
        )
        docRef.set(newModel).await()
      }
      uid
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun getMyProfile(): Result<RecoveryProfile?> = runCatching {
    val uid = checkNotNull(auth.currentUser?.uid) { "Oturum açmış Firebase kullanıcısı bulunamadı" }
    getProfileByUserId(uid).getOrThrow()
  }

  suspend fun getProfileByUserId(targetUserId: String): Result<RecoveryProfile?> = runCatching {
    require(targetUserId.isNotBlank()) { "Kullanıcı kimliği boş olamaz" }
    val docRef = db.collection(COLLECTION_RECOVERY_PROFILES).document(targetUserId)
    try {
      val snap = docRef.get().await()
      snap.toObject(
        RecoveryProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun queryMyProfiles(): Result<List<RecoveryProfile>> = runCatching {
    val uid = auth.currentUser?.uid ?: ""
    try {
      val querySnapshot = db.collection(COLLECTION_RECOVERY_PROFILES)
        .whereEqualTo("userId", uid)
        .get()
        .await()
      querySnapshot.toObjects(
        RecoveryProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, COLLECTION_RECOVERY_PROFILES)
      throw e
    }
  }

  fun observeMyProfiles(): Flow<List<RecoveryProfile>> {
    val uid = auth.currentUser?.uid ?: ""
    return db.collection(COLLECTION_RECOVERY_PROFILES)
      .whereEqualTo("userId", uid)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          RecoveryProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
  }

  companion object {
    const val COLLECTION_RECOVERY_PROFILES = "recovery_profiles"
  }
}
