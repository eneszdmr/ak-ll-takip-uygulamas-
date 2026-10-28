# Security Specification (`security_spec.md`)

## 1. Data Invariants
1. **Default-Deny Catch-All**: All paths not explicitly matched in `firestore.rules` are unconditionally denied (`allow read, write: if false;`).
2. **Strict Ownership Isolation**: A `/recovery_profiles/{userId}` document can only be created, read (`get`/`list`), updated, or deleted when `request.auth != null` and `request.auth.uid == userId` (and `resource.data.userId == request.auth.uid` for list queries).
3. **Immutable Identity & Creation Timestamp**: On updates, `userId` and `createdAt` must remain strictly equal to their existing values (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).
4. **Schema & Boundary Enforcement**:
   - `userId`: string, 1..128 chars, must equal `request.auth.uid`.
   - `phoneNumber`: string, 10..15 chars.
   - `email`: string, 3..254 chars.
   - `securityQuestion`: optional string, max 255 chars.
   - `recoveryCodeHint`: optional string, max 64 chars.
   - `createdAt` / `updatedAt`: server timestamp (`is timestamp && <= request.time`).
5. **Anti-Shadow-Field Gate**: Only allowlisted keys (`['userId', 'phoneNumber', 'email', 'securityQuestion', 'recoveryCodeHint', 'createdAt', 'updatedAt']`) are accepted on create, and only `['phoneNumber', 'email', 'securityQuestion', 'recoveryCodeHint', 'updatedAt']` can be modified on update.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("recovery_profiles").doc("alice_123").get()` -> Reject.
2. **Unauthenticated Write**: `unauthDb.collection("recovery_profiles").doc("alice_123").set(...)` -> Reject.
3. **Cross-User Read (PII Leak)**: `aliceDb.collection("recovery_profiles").doc("bob_456").get()` -> Reject.
4. **Cross-User Write (Identity Spoofing)**: `aliceDb.collection("recovery_profiles").doc("bob_456").set({ userId: "bob_456", ... })` -> Reject.
5. **UID Mismatch in Payload**: `aliceDb.collection("recovery_profiles").doc("alice_123").set({ userId: "bob_456", ... })` -> Reject.
6. **Shadow Field Injection on Create**: Payload containing `isAdmin: true` -> Reject via `keys().hasOnly(...)`.
7. **Shadow Field Injection on Update**: Update payload containing `isVerified: true` -> Reject via `affectedKeys().hasOnly(...)`.
8. **Immutable `createdAt` Tampering on Update**: Updating `createdAt` to a new timestamp -> Reject.
9. **Immutable `userId` Tampering on Update**: Updating `userId` to another UID -> Reject.
10. **Invalid Phone Length (Boundary Attack)**: `phoneNumber: "123"` (< 10 chars) or 1000-char string -> Reject.
11. **Value Poisoning on Update**: Updating `email` to an integer `12345` or oversized string -> Reject via `isValidRecoveryProfile(incoming())`.
12. **Unfiltered Collection List Query**: `aliceDb.collection("recovery_profiles").get()` without `.where("userId", "==", "alice_123")` -> Reject.
