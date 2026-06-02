# Payment Sandbox Configuration

This guide describes the optional Alipay sandbox setup for the F2 payment
upgrade. It is for development and staging verification only. It does not
authorize commercial payment launch.

## 1. Runtime Modes

The payment module supports two distinct modes:

- `MOCK`
  - Default for local development and automated tests.
  - Requires no external credentials.
  - Keeps deterministic success, failure, cancellation, timeout, and retry
    verification available.
- `ALIPAY_SANDBOX`
  - Opt-in for manual integration verification.
  - Uses Alipay sandbox credentials and sandbox gateway only.
  - Must not become the implicit default when credentials are absent.

## 2. Human Preparation Checklist

Before Alipay sandbox integration testing:

1. Sign in to the Alipay Open Platform with a real-name verified Alipay account.
2. Open the sandbox environment and record the sandbox application ID.
3. Configure RSA2 public-key mode for the sandbox application.
4. Keep the sandbox application private key outside the repository.
5. Record the sandbox Alipay public key used to verify responses and asynchronous notifications.
6. Obtain a sandbox buyer account or sandbox wallet from the sandbox console.
7. Prepare a callback URL that Alipay can reach over the network for manual asynchronous-notification testing.

The repository must never contain real private keys, sandbox account
passwords, or production credentials.

## 3. Environment Variables

The implementation reads the following environment variables:

| Name | Required | Purpose |
| --- | --- | --- |
| `ALIPAY_SANDBOX_ENABLED` | no | Set to `true` only for sandbox verification. |
| `ALIPAY_SANDBOX_APP_ID` | sandbox only | Sandbox application ID. |
| `ALIPAY_SANDBOX_PRIVATE_KEY` | sandbox only | Application RSA2 private key. |
| `ALIPAY_SANDBOX_PUBLIC_KEY` | sandbox only | Alipay public key for signature verification. |
| `ALIPAY_SANDBOX_GATEWAY_URL` | sandbox only | Sandbox API gateway URL. |
| `ALIPAY_SANDBOX_NOTIFY_URL` | sandbox callback only | Public asynchronous callback URL. |

Do not commit a populated `.env` file. Copy `.env.example` to `.env` and fill
the ignored local file only when sandbox verification starts.

Recommended local `.env` shape:

```env
MYSQL_PASSWORD=your-local-mysql-password
APP_JWT_SECRET=replace-with-a-local-secret-at-least-32-characters

ALIPAY_SANDBOX_ENABLED=true
ALIPAY_SANDBOX_APP_ID=9021000xxxxxxxxx
ALIPAY_SANDBOX_PRIVATE_KEY=MIIEv...
ALIPAY_SANDBOX_PUBLIC_KEY=MIIBIj...
ALIPAY_SANDBOX_NOTIFY_URL=https://your-public-host.example.com/api/payments/callbacks/alipay-sandbox
ALIPAY_SANDBOX_GATEWAY_URL=https://openapi-sandbox.dl.alipaydev.com/gateway.do
```

Key formatting rules:

- Use the application private key generated for RSA2 signing.
- Use the Alipay sandbox public key from the sandbox console.
- Remove `-----BEGIN ...-----` / `-----END ...-----` lines.
- Remove all line breaks so each key becomes one long line.
- Do not commit the populated values.

## 4. How To Fill The Values

### `ALIPAY_SANDBOX_APP_ID`

- Open the Alipay sandbox console.
- Find the sandbox app you created for this test.
- Copy the sandbox application ID exactly as shown.

### `ALIPAY_SANDBOX_PRIVATE_KEY`

- Generate or copy the RSA2 application private key for that sandbox app.
- Use the private key that belongs to your app, not Alipay's public key.
- Convert it to a single line before putting it into `.env`.

### `ALIPAY_SANDBOX_PUBLIC_KEY`

- In the sandbox console, copy the Alipay public key used for response and callback signature verification.
- Convert it to a single line before putting it into `.env`.

### `ALIPAY_SANDBOX_NOTIFY_URL`

- This must be a public HTTPS URL that reaches your backend callback endpoint.
- The full final value must end with:

```text
/api/payments/callbacks/alipay-sandbox
```

Example:

```text
https://abc123.trycloudflare.com/api/payments/callbacks/alipay-sandbox
```

## 5. Sandbox Wallet Requirement

For QR-code verification:

- Use the Alipay sandbox wallet or sandbox buyer environment supplied by the
  sandbox console.
- Use the sandbox wallet's scan feature for the QR payment flow.
- Do not treat the regular production Alipay app as a valid verifier for the
  sandbox QR code. It may fail, show the QR as expired, or refuse the flow even
  when backend initiation is correct.

## 6. Local Startup

The local startup scripts work from the current worktree root and load the
root `.env` automatically:

```powershell
.\scripts\start-backend-local.ps1
.\scripts\start-frontend-local.ps1
.\scripts\start-local-dev.ps1
```

Defaults:

- backend: `http://127.0.0.1:8080`
- frontend: `http://127.0.0.1:5173`
- frontend dev proxy target: `http://127.0.0.1:8080`
- local backend database: `localhost:3306/youyu`

When `ALIPAY_SANDBOX_ENABLED=false`, the application stays in mock mode and the
sandbox credential fields may remain empty.

## 7. Callback Reachability

Alipay asynchronous notifications originate outside the developer machine.
`localhost`, `127.0.0.1`, and private LAN addresses cannot be used for
end-to-end callback verification.

For local manual verification, use one of:

- a temporary HTTPS tunnel forwarding to the backend callback endpoint;
- a reachable staging deployment with sandbox variables configured.

The callback path is:

```text
/api/payments/callbacks/alipay-sandbox
```

Practical local pattern on Windows:

1. Start backend locally on `http://127.0.0.1:8080`
2. Expose port `8080` through an HTTPS tunnel
3. Set `ALIPAY_SANDBOX_NOTIFY_URL` to:

```text
https://<your-public-host>/api/payments/callbacks/alipay-sandbox
```

Two common tunnel choices:

- `cloudflared tunnel --url http://127.0.0.1:8080`
- `ngrok http 8080`

Important URL rules:

- The URL must be HTTPS.
- The URL must point to the backend, not the frontend dev server.
- The URL must include the full callback path above.

Mock mode remains the fallback when a public callback endpoint is not
available.

## 8. Verification Boundary

Sandbox integration is accepted only when:

- payment initiation returns a sandbox payment target;
- synchronous Alipay API responses are signature-verified;
- the browser or QR flow can be opened with a sandbox buyer account;
- the asynchronous notification is signature-verified;
- repeated notifications are idempotent;
- the server verifies payment number and amount before mutating state;
- refund execution updates refund and order state only after gateway-confirmed success;
- removing sandbox variables returns the application to mock mode without breaking automated tests.
