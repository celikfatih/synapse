# Synapse Slack App Setup & Installation Guide

This guide walks you through setting up, configuring, and installing the **Synapse Slack App** into your Slack workspace. Once configured, Synapse ingests autonomous coding tasks via channel mentions (`@Synapse`) or slash commands (`/synapse`), executes them in isolated Docker sandboxes, and posts rich **Block Kit** completion notifications back to your team.

---

## 1. Prerequisites & Local Ingress (`ngrok`)

Slack's API requires a **publicly accessible HTTPS endpoint** to send event callbacks and slash commands (`POST /api/slack/events` and `POST /api/slack/commands`). 

If you are running Synapse locally on port `8080` or inside Docker Compose (`http://localhost:8080`), use an ingress tunnel like **[ngrok](https://ngrok.com/)** or **cloudflared**:

```bash
# Expose local port 8080 via ngrok
ngrok http 8080
```

Copy the generated HTTPS forwarding URL (e.g., `https://1234-abcd.ngrok-free.app`). You will use this as your **Base Request URL** in the Slack API dashboard.

---

## 2. Create Your Slack App & Obtain Signing Secret

1. Go to the **[Slack API App Dashboard](https://api.slack.com/apps)** and click **Create New App**.
2. Select **From scratch**, enter **`Synapse Bot`** as the App Name, and choose your workspace.
3. In your app's navigation sidebar, go to **Basic Information** $\rightarrow$ **App Credentials**.
4. Locate the **Signing Secret** and click **Show**. Copy this value into your `.env` configuration:

```properties
# .env
SYNAPSE_SLACK_SIGNING_SECRET=your_copied_signing_secret_here
```

> [!IMPORTANT]
> Synapse's `SlackSignatureVerificationFilter` strictly verifies all incoming Slack requests (`/api/slack/*`) using `HMAC-SHA256` (`X-Slack-Signature`). If this secret does not match exactly, Synapse returns `HTTP 401 Unauthorized` and rejects the payload.

---

## 3. Configure Event Subscriptions (`@Synapse` Mentions)

Event subscriptions allow Synapse to listen for `@Synapse` tags and channel messages.

1. In the sidebar under **Features**, select **Event Subscriptions**.
2. Toggle **Enable Events** to **On**.
3. In the **Request URL** field, enter your Synapse Events endpoint:
   ```text
   https://<your-ngrok-or-domain>.ngrok-free.app/api/slack/events
   ```
   *Note: When you enter this URL, Slack immediately sends a `url_verification` challenge. Synapse (`SlackTaskRestAdapter`) will automatically respond with the challenge token, and Slack will display a green **Verified ✔** badge.*
4. Expand **Subscribe to bot events** and add the following bot scopes:
   - `app_mentions:read` — Receive events when `@Synapse` is tagged in channels.
   - `message.channels` — Receive public channel message events.
   - `message.groups` *(optional)* — Receive private channel message events.
   - `message.im` *(optional)* — Receive Direct Message (DM) events when users chat privately with Synapse.
5. Click **Save Changes** at the bottom of the page.

> [!TIP]
> **Built-in Event Deduplication & Retry Protection**:
> - **Channel vs. DM Separation (`SlackPayloadTranslator`)**: When both `app_mentions:read` and `message.channels` are enabled, mentioning `@Synapse` in a channel causes Slack to send both an `app_mention` and a `message` event. Synapse intelligently deduplicates this: `message` events in channels (`C...`/`G...`) are automatically ignored because `app_mention` handles them, preventing duplicate task creation. Conversely, direct messages (`D...` / `im`) are processed directly.
> - **Webhook Retry Filtering (`X-Slack-Retry-Num`)**: If network latency or heavy Kafka dispatching exceeds Slack's 3-second webhook timeout, Slack retries delivery. `SlackTaskRestAdapter` automatically checks for the `X-Slack-Retry-Num` header and immediately returns `200 OK` without resubmitting duplicate tasks.

---

## 4. Configure Slash Commands (`/synapse`)

Slash commands provide a fast shortcut (`/synapse` or `/tasks`) to submit structured tasks without tagging the bot in chat.

1. In the sidebar under **Features**, select **Slash Commands**.
2. Click **Create New Command** and fill out the form:
   - **Command**: `/synapse` *(or `/tasks`)*
   - **Request URL**: 
     ```text
     https://<your-ngrok-or-domain>.ngrok-free.app/api/slack/commands
     ```
   - **Short Description**: `Submit an autonomous coding task to Synapse`
   - **Usage Hint**: `[ticket-key or instructions] e.g. fix NPE in TaskService.java`
3. Click **Save**.

---

## 5. Configure Incoming Webhooks (Completion & Error Alerts)

Synapse uses an **Incoming Webhook** via Spring `RestClient` (`SendNotificationPort`) to post rich Block Kit cards summarizing GitHub Pull Request diffs or diagnostic failures.

1. In the sidebar under **Features**, select **Incoming Webhooks**.
2. Toggle **Activate Incoming Webhooks** to **On**.
3. Scroll down and click **Add New Webhook to Workspace**.
4. Select the target Slack channel where Synapse should post notifications (e.g., `#synapse-alerts` or `#engineering`) and click **Allow**.
5. Copy the generated **Webhook URL** (`https://hooks.slack.com/services/T000/B000/XXX`) into your `.env` configuration:

```properties
# .env
SYNAPSE_SLACK_WEBHOOK_URL=https://hooks.slack.com/services/T000/B000/XXX
```

---

## 6. Install App to Workspace & Invite Bot

1. In the sidebar, go to **Settings** $\rightarrow$ **Install App**.
2. Click **Install to Workspace** and authorize the requested OAuth scopes (`chat:write`, `app_mentions:read`, `incoming-webhook`, `commands`).
3. Open your Slack workspace and go to the channel where you want to interact with Synapse (or where you set up the webhook).
4. Invite the Synapse bot into the channel:
   ```text
   /invite @Synapse Bot
   ```

---

## 7. Verification & End-to-End Testing

Once your `.env` is updated and Synapse is running (`./gradlew bootRun` or `docker compose up -d`), test both inbound flows directly inside Slack:

### Test Slash Command
Type in your Slack channel:
```text
/synapse fix null pointer exception when Jira ticket description is empty
```
**Expected Behavior**:
1. Synapse immediately returns an ephemeral confirmation: `"Task submitted to Synapse! 🚀"`
2. Check your Synapse server logs:
   ```text
   Received Slack slash command [/synapse] from user [alice] in team [T01234]
   ```

### Test App Mention
Tag the bot directly in your Slack channel:
```text
@Synapse Bot add unit tests for UserRestAdapter.java and verify 80% coverage
```
**Expected Behavior**:
1. Synapse verifies the `HMAC-SHA256` signature, strips `<@U0123BOT>`, and submits the clean task prompt (`"add unit tests for UserRestAdapter.java..."`).
2. When the autonomous sandbox (`synapse-sandbox:java25`) completes the task and opens a Pull Request, Synapse posts a formatted **Block Kit** notification to `#synapse-alerts` containing:
   - ✅ **Synapse Task Completed Successfully**
   - **Task ID** & **Requester** (`alice (U01234)`)
   - Clickable **Pull Request Link** on GitHub (`https://github.com/org/repo/pull/42`)
