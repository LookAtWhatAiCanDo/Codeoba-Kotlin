# Codeoba Multi-Device Ecosystem & Subscription Architecture (v3.5)

This document describes the security controls, device pairing, synchronization mechanics, and subscription model for the **Codeoba Multi-Device Ecosystem**.

---

## 🎯 Value Proposition & Threat Model

### 1. The Codeoba Ecosystem Value
Codeoba is a unified multi-device management and control plane for autonomous coding agents (such as Claude Code, Google Antigravity, Cursor, OpenAI Codex, and Aider). 

* **Free & Open Local Use**: Single-device local use (indexing log directories, local databases, and executing local lexical and semantic searches) is 100% free, open source, and unencrypted. We do not implement client-side DRM, binary signature verification, or anti-tamper blocks on local usage.
* **Premium Ecosystem Tier**: The monthly subscription gates the **Cross-Device Coordination & Relay Plane**:
  1. **Cross-Device Sync**: Aggregating, index-merging, and searching across session history from all of your development laptops, workstations, and remote servers.
  2. **Remote Agent Monitoring & Control**: Real-time status notifications and scoped control relays (e.g. view terminal outputs, pause tasks, approve agent command prompts, or cancel runs) from secondary interfaces such as mobile companion apps, remote CLI shells, smartwatches, or smart glasses.
  3. **Multi-Agent Aggregator**: Seamlessly controlling tasks and viewing histories across different agent providers (Anthropic, OpenAI, Google) from a single unified hub.

### 2. Attacker Classes, Honestly Graded

| Attacker | Goal | Defense | Honest Grade |
|---------|------|--------|-------------|
| **A — Casual Freeloader** | Run Codeoba search locally | None. We allow this by design. Local use is free. | **N/A (Intentionally Permitted)** |
| **B — Patcher / Cracker** | Bypass checks to get Premium cloud sync | None on client. The server enforces subscription validity per request. | **Defused by Design.** Cracking the client does not grant access to the Sync Hub database, because the server checks the caller's Firestore subscription state on every API call. |
| **C — Account Sharer** | Share Premium access across a team | Server-side concurrency and velocity thresholds on the Sync Hub. | **Bounds Abuse.** Limits maximum active sessions per subscription tier on the backend. |

---

## 🏗️ Ecosystem Architecture

```mermaid
sequenceDiagram
    participant Laptop as Development Machine (Laptop/Server)
    participant Glasses as Control Device (Phone/Watch/Glasses)
    participant Auth as Firebase Auth (Cloud)
    participant Hub as Sync Hub Backend (Firebase/API)
    
    Note over Laptop: LOCAL SEARCH (Always Free / Offline)
    Laptop->>Laptop: Watch agent log directories
    Laptop->>Laptop: Index turns into local SQLite DB
    Laptop->>Laptop: Run local semantic search (ONNX)
    
    Note over Laptop,Hub: ECOSYSTEM SYNC (Paid Subscription)
    Note over Laptop,Hub: (v1 authorization is same Firebase UID only)
    Laptop->>Auth: Authenticate (Get ID Token)
    Laptop->>Hub: Register Device & Sync search indices
    Glasses->>Auth: Authenticate (Get ID Token)
    Glasses->>Hub: Request Device List & Active Sessions
    Hub->>Hub: Check Subscription State in Firestore
    alt Subscription Status is Active
        Hub->>Glasses: Return Laptop sessions & state
        Glasses->>Hub: sendRemoteCommand (HTTPS Callable API with CommandID)
        Hub->>Hub: Verify device ownership, subscription, pairing & idempotency
        Hub->>Hub: Write to Append-Only Audit Log
        Hub->>Laptop: Push remote command via WebSocket/Push/Listener
        Laptop->>Laptop: Execute command (e.g., pause agent)
    else Subscription Status is Inactive
        Hub->>Glasses: Return 402 Payment Required
    end

### 3. Browser-Based OAuth Connection Handshake Flow

To support secure authentication (including third-party identity providers like Google and GitHub) without embedding credentials or heavy SDKs in the desktop client, Codeoba utilizes a local loopback server POST callback transfer:

```mermaid
sequenceDiagram
    participant App as Codeoba App (Desktop)
    participant Browser as System Browser
    participant Web as Auth Page (Firebase Web)
    participant Auth as Firebase Auth (Cloud)
    participant Hub as Sync Hub Backend
    
    App->>App: Start local HttpServer on random port P
    App->>Browser: Open system browser to Web Page with port P & state S
    Browser->>Web: Load connect page (?port=P&state=S)
    Web->>Web: User logs in (Google, GitHub, or Email/Password)
    Auth->>Web: Return ID Token & Refresh Token
    Web->>App: Send secure POST request with credentials (idToken, refreshToken, email, uid, state)
    App->>App: Save credentials & Stop HttpServer
    App->>Hub: Complete Handshake (registerEcosystemDevice)
    Web->>Browser: Display success message ("Successfully Authenticated! You can close this browser tab...")
```

* **Immutable UID Checkout Binding**: Checking out a subscription binds Polar custom metadata to the user's immutable Firebase `uid` (`custom_metadata: { uid: auth.uid }`) rather than email addresses to prevent entitlement mismatches during email updates.

---

## 🔒 Hardened Security Specifications

### 1. Webhook Signature Verification & Subscription Reconciliation
To prevent unauthorized entitlement generation, the Polar webhook listener endpoint is hardened as follows:
* **Signature Check**: Every incoming webhook POST request is validated using the Polar webhook signing key (`POLAR_WEBHOOK_SECRET`). Requests with missing, invalid, or mismatched signatures are rejected immediately.
* **Idempotency & Replay Protection**: Each webhook event ID (`event_id`) is stored in Firestore. If the server receives an event ID that has already been processed, it ignores it. Event records expire automatically after Polar's maximum retry window (48 hours).
* **Authoritative Reconciliation**: Rather than blindly trusting out-of-order webhook event payloads, the backend treats incoming webhooks as signals. Upon receipt of a subscription status change event, the backend uses Polar's API version/timestamp metadata to resolve ordering or makes a direct server-to-server API call to Polar to reconcile the authoritative state.
* **Write Lock**: Only the authenticated backend service account can write to the `/users/{uid}/subscriptions` subcollection. Direct write or modification access from the client SDKs is blocked unconditionally.

### 2. Device Identity (Proof of Possession)
To prevent simple device-ID spoofing and copying of cached session directories:
* **OS-Secured Storage**: Upon first ecosystem connection, each device generates a unique cryptographic keypair.
  - Keys are generated in-process and the encoded key material is stored in the OS credential store (Keychain / Credential Manager / Secret Service) via `java-keyring` when available, with a fallback to `java.util.prefs.Preferences` when the keyring is unavailable or disabled.
  - Note: this storage approach protects keys behind OS-level access controls, but it is not the same as using non-exportable/TPM/Secure-Enclave-backed keys, and no headless key-file fallback is currently implemented.
* **Challenge-Response Auth**: Subsequent socket connections and stream requests require proof of key possession. The backend issues a cryptographically random, single-use nonce that expires in 90 seconds. The client must sign the provided nonce (or a canonical payload containing the nonce and identifying fields, depending on the backend implementation).

### 3. Stream Expiry Gate (Mid-Stream Revocation)
Remote command relays use a persistent listener stream. To prevent users from maintaining access after their subscription expires or is canceled:
* **Token Lease (TTL)**: Client connection tokens expire after a maximum of 1 hour. Upon expiration, the client must request a new session lease.
* **Real-time Disconnection**: The Sync Hub monitors `/users/{uid}/subscriptions`. If a subscription cancels or lapses, the backend immediately terminates all active device socket connections and listener streams associated with the user. Revocation latency is primarily bounded by Polar webhook delivery, backend reconciliation, and stream-termination propagation. As a fallback, the 1-hour lease TTL ensures disconnection.

### 4. Command Authorization Matrix
Codeoba categorizes remote operations into risk-based scopes, both bound strictly by same-user account scopes (v1 does not support team/cross-account device relays):

* **Low-Risk Actions (Read-Only/Control)**: Viewing agent status, fetching summaries, pausing/resuming a run, or canceling a task.
  - *Local Target Policies*: Users can configure their target machines to restrict Low-Risk actions (e.g., disabling remote pause/resume or requiring local confirmation before cancel operations to prevent half-mutated workspace states).
* **High-Risk Actions (Execution/Modification)**: Approving an agent's terminal command, submitting custom keyboard input, or writing files.
  - *Opt-In Pairing Handshake*: High-Risk actions require an explicit handshake. The control device requests pairing. The target machine displays the control device's name and a one-time short code. The user must manually confirm pairing on the target machine. The backend stores an expiring pairing grant:
    `{ originDeviceId, targetDeviceId, allowedActions, createdAt, expiresAt: 24h, revoked: false }`
  - *Backend-Written Append-Only Audit Log*: Every remote command authorization decision is recorded to `/users/{uid}/audit` by the backend. Client SDKs have no direct write, update, or delete access to audit records. The backend writes this audit log event as part of the same transaction flow *before* dispatching the command payload to the target device. Backend code treats the collection as append-only; deletion is limited to retention-policy jobs or privileged administrative maintenance.

### 5. Backend-Mediated Relay API
Clients are not allowed to write command documents directly into Firestore. Instead, all commands must be submitted via a secure HTTPS Callable function (`sendRemoteCommand`). The backend API acts as the gatekeeper, verifying:
1. Origin user owns the sending device.
2. Target device belongs to the same user/account.
3. Subscription status is active.
4. Sending device holds a valid pairing grant (for High-Risk actions).
5. Stream lease is valid and rate limits are not exceeded.
6. **Command Idempotency**: Each payload must contain a unique `commandId` (idempotency key). Retried submissions with the same key are ignored or return the cached execution result.
7. **Rate Limits & Fail-Safes**: High-Risk command approvals are velocity-limited per device and user. Repeated failed pairing handshakes trigger temporary IP blocks and pairing lockdown.

---

## ⚙️ Data Custody, Sync Modes, & Abuse Quotas

### 1. Data Custody & Encryption Trade-Off
* **Server-Readable Search requirement**: Cross-device search requires the Sync Hub to index and read synced log content. Therefore, Full Sync logs are encrypted at rest using keys Codeoba manages (not end-to-end user-held keys). 
* **Privacy Choice**: If a user requires content that Codeoba cannot read, they must select **Metadata Only** or **Summaries Only** sync modes, which do not upload raw agent logs or source code.
* **Best-Effort Redaction**: The client features a best-effort local regex scanner that scrubs high-confidence credential patterns (GitHub tokens, OpenAI/Anthropic keys, AWS access keys `AKIA`, private key blocks, and `.env` assignments) before upload. This is presented as a **best-effort utility, not a guarantee**.
* **Sync Consent Warning**: When enabling Full Sync, the UI displays a clear warning: *"Full Sync uploads raw agent conversations, terminal output, file paths, source snippets, and command results. Enable only for workspaces where cloud sync is permitted."*

### 2. Retention Quotas
* *Metadata & Summaries*: Deleted after 90 days.
* *Full Sync Logs*: Deleted after 30 days default (user-configurable).
* *Audit Logs*: Retained for 1 year.
* *User Deletion*: Cloud-synced indices are queued for immediate deletion upon account deletion, with best-effort prompt removal from active serving paths and completion within the published SLA.

### 3. Usage Quotas & Abuse Thresholds
Subscription tiers map directly to operational limits:

| Quota Dimension | Free Tier | Individual Premium ($5/mo) | Pro / Team Tier |
|-----------------|-----------|----------------------------|-----------------|
| **Max Development Devices** | 1 | 3 | 10 |
| **Max Control Devices** | 0 | 3 | 10 |
| **Max Active Relay Streams** | 0 | 2 | 5 |
| **Full Sync Log Retention** | N/A (Local Only) | 30 Days | 90 Days |
| **Max Sync Volume / Day** | N/A | 50 MB | 500 MB |

* *Mobile Push Caveat*: For mobile, smartwatch, or smart glasses control devices, background connection streams may be woken up asynchronously using cloud push service notifications (FCM / APNs) to conserve battery and data.

---

## 🚨 Emergency Operations

### 1. Global Kill Switch
If a critical vulnerability or remote-control bypass is detected, the backend maintains a global emergency configuration:
* `disableHighRiskRelay = true`: Instantly blocks all High-Risk commands globally.
* `disableAllRelay = true`: Disables the remote control stream completely.
* `minimumClientVersion = "1.1.0"`: Rejects connections from outdated client builds.

---

## 🛠️ Compile-Time Subscription Gating (Temporary Developer Toggle)

To enable merging of ongoing subscription development changes to the `main` branch without exposing half-finished or confusing features to production users, and to allow developers to A/B test the application's behavior with and without subscription capabilities, the project uses a compile-time static configuration constant (`BuildConfig.ENABLE_SUBSCRIPTION`).

> [!IMPORTANT]
> **Temporary Transition Control:** This compile-time setting and the `codeoba.enable_subscription` toggle are temporary transition mechanisms. Once the subscription features are officially released, this toggle and gating logic will be removed, leaving the subscription and sync integration permanently enabled in all builds.

### 1. How Gating Works
When building for production, `BuildConfig.ENABLE_SUBSCRIPTION` is statically set to `false`. 
* **UI Gating**: In [SettingsDialog.kt](../app-desktop/src/desktopMain/kotlin/com/whataicando/codeoba/desktop/SettingsDialog.kt), the `Account & Subscription` settings tab category is hidden from the sidebar.
* **Background Logic Gating**: In [Main.kt](../app-desktop/src/desktopMain/kotlin/com/whataicando/codeoba/desktop/Main.kt), the background token-refresh and device registration sync loop is bypassed.
* **Dead Code Elimination**: Because `ENABLE_SUBSCRIPTION` is a `const val`, the Kotlin compiler performs constant folding and dead code elimination, physically tree-shaking the gated UI and background sync loop bytecode out of the release binary.

### 2. Enabling Subscription Features Locally
To enable the subscription/ecosystem features in your local development environment:
1. Open your local [local.properties](../local.properties) (which is git-ignored).
2. Append the following property:
   ```properties
   codeoba.enable_subscription=true
   ```
3. Re-run or build the project. The Gradle `generateBuildConfig` task will read this value and compile `BuildConfig.kt` with `ENABLE_SUBSCRIPTION = true`.

To disable features again, set the property to `false` or remove it entirely from `local.properties`.
