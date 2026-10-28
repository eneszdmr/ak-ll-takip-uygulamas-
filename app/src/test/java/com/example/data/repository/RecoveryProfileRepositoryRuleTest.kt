package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RecoveryProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveOrUpdateProfile_validPayload_createsAndUpdatesDocument() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = RecoveryProfileRepository(firestore, auth)

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveOrUpdateProfile(
        phoneNumber = "5071231212",
        email = ALICE_EMAIL,
        securityQuestion = "İlkokul öğretmeninizin adı nedir?",
        recoveryCodeHint = "ECZA-****"
      )
    }
    assertTrue(saveResult.isSuccess)
    assertEquals(uid, saveResult.getOrThrow())

    // Update existing profile
    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveOrUpdateProfile(
        phoneNumber = "5071231212",
        email = ALICE_EMAIL,
        securityQuestion = "İlk evcil hayvanınızın adı nedir?",
        recoveryCodeHint = "ECZA-99**"
      )
    }
    assertTrue(updateResult.isSuccess)
  }

  @Test
  fun queryMyProfiles_authenticatedOwner_returnsMatchingProfile() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = RecoveryProfileRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveOrUpdateProfile(
        phoneNumber = "5071231212",
        email = ALICE_EMAIL
      ).getOrThrow()
    }

    val profilesResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.queryMyProfiles() }
    assertTrue(profilesResult.isSuccess)
    assertTrue(profilesResult.getOrThrow().any { it.id == uid && it.phoneNumber == "5071231212" })
  }

  @Test
  fun observeMyProfiles_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = RecoveryProfileRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveOrUpdateProfile(
        phoneNumber = "5071231212",
        email = ALICE_EMAIL
      ).getOrThrow()
    }

    val emitted = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeMyProfiles().first { list -> list.any { it.id == uid } }
    }
    assertTrue(emitted.any { it.id == uid })
  }

  @Test
  fun getProfileByUserId_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = RecoveryProfileRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.saveOrUpdateProfile(
        phoneNumber = "5071231212",
        email = ALICE_EMAIL
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = RecoveryProfileRepository(firestore, auth)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getProfileByUserId(aliceUid) }
    assertTrue(result.isFailure)
    val ex = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(ex)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
  }

  @Test
  fun observeMyProfiles_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = RecoveryProfileRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeMyProfiles().first() }
      fail("Expected PERMISSION_DENIED exception for unauthenticated user")
    } catch (e: Exception) {
      val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
