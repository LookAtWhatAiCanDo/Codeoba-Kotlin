# 🌌 Codeoba Ecosystem Guide: Connect & Control Your Coding Agents

Welcome to the **Codeoba Ecosystem**! 

Codeoba is a unified command center and cross-device coordinator for autonomous coding agents like **Claude Code, Google Antigravity, Cursor, OpenAI Codex, and Aider**. 

This guide explains what the Codeoba Ecosystem is, how it works, and how you will use it to monitor and control your active coding tasks from your laptop, phone, smartwatch, or smart glasses.

---

## 🚀 What is the Codeoba Ecosystem?

By default, Codeoba runs **100% locally and offline on your computer**. You can execute local lexical and semantic searches and index log directories completely for free. 

The **Ecosystem Subscription ($5/mo)** unlocks AI-powered conversation summaries and bridges your development environment to your personal companion devices. It creates a secure, real-time sync channel that lets you:

* **🔍 Multi-Device Search**: Search and view conversation logs from all of your workstations and remote servers in a single merged index on any device.
* **📱 Remote Control (Phone & Watch)**: Check the status of long-running terminal tasks, read live output, pause/resume agents, or cancel a runaway process right from your phone or smartwatch while away from your desk.
* **🕶️ Smart Glasses Updates**: Receive unobtrusive status alerts and success notifications directly on your smart glasses as your agents finish builds or encounter errors.
* **🔒 Secure Approvals**: Review and approve high-risk agent command prompts (like committing code or running script mutations) with a simple tap on your wrist or phone screen.

---

## 📂 Understanding Sync Modes & Privacy

Your privacy is our priority. You choose exactly what data leaves your development machine via the **Sync Mode** setting in the Account panel:

1. **Local Only (Free/Offline)**: No data ever leaves your device. All local lexical and semantic search indexing (and AI summaries, if subscribed) happens locally on your hardware.
2. **Metadata Only (Default)**: Synced devices only see high-level task metrics (e.g. task names, status, and execution times). Raw logs and code snippets remain on your machine.
3. **Summaries Only**: Syncs high-level AI-generated summaries of your conversation turns, letting you review what the agent accomplished without uploading raw terminal outputs or file paths.
4. **Full Sync**: Syncs raw conversation transcripts and terminal outputs. **This mode is required to enable remote approvals of command prompts.** Content is encrypted at rest to ensure unauthorized parties cannot access your logs.

> [!TIP]
> **Automatic Secrets Redaction:** Before any log data is uploaded in Full Sync mode, the Codeoba client automatically runs a local scanner to find and redact high-confidence secrets (such as GitHub PATs, OpenAI/Anthropic API keys, private key blocks, and `.env` password variables).

---

## 🤝 How Pairing Works (Planned Flow)

Once the mobile and wearable clients are released, connecting your devices will take less than 30 seconds:

```
+------------------------+             +------------------------+
|   Desktop App (Laptop) |             |    Mobile App (Phone)  |
|                        |             |                        |
|  Pairing Request       |  (Handshake)|  Enter Pairing Code:   |
|  Device: "My iPhone"   | <---------> |  [ 8 4 2 - 1 9 ]       |
|  Pairing Code: 84219   |             |                        |
+------------------------+             +------------------------+
```

### Step 1: Sign In
Sign into the same account on both your desktop app and your companion app (via the secure web authentication portal).

### Step 2: Request Pairing
From your mobile or wearable companion app, select your laptop from the list of active devices and tap **Request Pairing**.

### Step 3: Enter the One-Time Code
Your laptop will show a notification displaying the companion device's name and a one-time short code. Enter the code on your mobile/wearable client to authorize command execution permissions.

### Step 4: Configure Execution Policies
On your desktop app, you can restrict what remote commands are allowed:
* **Allow All**: Authorizes your paired devices to trigger pause, resume, cancel, and input confirmations.
* **Allow Paired Only (Default)**: Restricts high-risk modifications (like confirming code execution prompts) to explicitly paired devices.
* **Block All**: Disables all remote execution requests on this machine, treating the companion screen as read-only.

---

## 🛡️ Security Under the Hood

The Codeoba Ecosystem is built on industry-standard security practices:
* **Proof-of-Possession Nonces**: Your devices sign a single-use random cryptographic challenge that expires in 90 seconds to prevent device-identity spoofing.
* **Restricted OS Keychains**: Private keys are stored in secure hardware elements where available (macOS Keychain, Windows CNG/TPM, Linux Secret Service).
* **Append-Only Audit Logs**: Every remote command triggered from a companion device is written to a server-side audit log. This audit log cannot be modified or deleted by client devices, ensuring you have an untamperable record of what actions your agents performed.
