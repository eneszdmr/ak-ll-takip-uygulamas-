package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class RecoveryProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val phoneNumber: String = "",
  val email: String = "",
  val securityQuestion: String = "",
  val recoveryCodeHint: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class UserSecuritySummary(
  val phoneNumber: String = "",
  val recoveryEmail: String? = null,
  val firebaseUid: String? = null,
  val securityQuestion: String? = null,
  val hasSecurityAnswer: Boolean = false,
  val hasRecoveryCode: Boolean = false,
  val recoveryCodeHint: String? = null
)
