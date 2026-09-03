# SQA Daily Health Check Agent

Automatically logs into 15+ systems every day, checks whether each one is
up, and publishes a shareable dashboard (webpage) showing status, response
time, and failure reason per system.

## How it works (plain summary)
1. You list your systems in `config/systems.json` — name, URL, and where
   the username/password/login-button fields are on that login page.
2. Every day, before your 9:30-10:00 AM deadline, GitHub automatically:
   - Opens each system's login page in a real (headless) Chrome browser
   - Logs in using credentials stored securely as GitHub Secrets (never in code)
   - Checks whether a "you're logged in" element appears
   - Records: UP/DOWN, how long it took, and *why* it failed if it did
3. Publishes the result as a webpage (via GitHub Pages) that anyone with
   the link can open — no login needed to view it.

## One-time setup

### 1. Fill in `config/systems.json`
One entry per system. Example:
```json
{
  "name": "Case Management Portal",
  "url": "https://example-crm.yourcompany.com/login",
  "usernameLocator": "id:username",
  "passwordLocator": "id:password",
  "submitLocator": "id:loginBtn",
  "successLocator": "id:dashboardHeader",
  "usernameEnvVar": "CRM_USERNAME",
  "passwordEnvVar": "CRM_PASSWORD"
}
```

**How to find the locator values** (`id:username` etc.):
1. Open the system's login page in Chrome
2. Right-click the username field → **Inspect**
3. Look for an `id="..."` attribute — use `id:that_value`
4. If there's no `id`, use `name:that_value` (check the `name="..."` attribute instead)
5. If neither exists, use `css:` followed by a CSS selector, or ask a developer for 2 minutes of help — this is the only slightly technical step

`successLocator` = something that ONLY appears after a successful login
(e.g. the dashboard heading, a welcome message, a logout button).

### 2. Add credentials as GitHub Secrets (not in the JSON file)
For every system, go to your repo →
**Settings → Secrets and variables → Actions → New repository secret**,
and add two secrets matching the `usernameEnvVar`/`passwordEnvVar` names
you used in the JSON, e.g.:
- `CRM_USERNAME` = the actual login username
- `CRM_PASSWORD` = the actual login password

This keeps credentials encrypted and out of the codebase entirely.

### 3. Add the same secret names to the workflow file
Open `.github/workflows/health-check.yml` and add one `env:` line per
system under the "Run health checks" step, following the existing pattern
for CRM/HR/Finance. This tells GitHub which secrets to hand to the program.

### 4. Enable GitHub Pages
Go to repo **Settings → Pages → Build and deployment → Source** → select
**GitHub Actions**. This is what makes the dashboard viewable as a real
webpage link (shareable with your team) instead of just a downloaded file.

## Running it
- **Automatically**: every day at 04:00 UTC (9:00 AM Pakistan time) — edit
  the `cron` line in the workflow file if your timezone or deadline changes
- **Manually anytime**: go to the **Actions** tab → "Daily System Health
  Check" → **Run workflow**
- **On your own machine** (for testing before relying on CI):
  ```bash
  mvn clean package
  export CRM_USERNAME=youruser
  export CRM_PASSWORD=yourpass
  # ...repeat export for each system's credentials
  java -jar target/sqa-health-check-agent.jar
  ```
  Then open `docs/index.html` in a browser.

## Viewing the dashboard
Once GitHub Pages is enabled, your dashboard is available at a permanent
URL like:
```
https://<your-github-username>.github.io/<repo-name>/
```
It updates automatically after each run — just refresh the page, no need
to download anything or re-run manually to see the latest status.

## Adding a 16th (or 50th) system later
No code changes needed — just:
1. Add a new entry to `config/systems.json`
2. Add its two secrets in GitHub Settings
3. Add its two `env:` lines in the workflow file

## Notes & limitations
- If a system's login page redesigns its fields, that system's locators in
  `systems.json` will need updating (same maintenance need as any UI automation).
- If a system requires 2FA/OTP or CAPTCHA, this agent can't get past that
  automatically — those systems would need a different check (e.g. a simple
  "is the URL reachable" ping instead of full login).
- This checks **login success**, not full functionality — it's a smoke/health
  check, not a full regression test.
