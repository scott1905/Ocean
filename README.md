## Run Playwright + TestNG (Windows PowerShell)

Prereqs: Java 11+, Maven, internet access.

Steps:
- mvn -q -DskipTests=false test

Notes:
- First run downloads Playwright browsers.
- Report: `ExtentReport.html` in project root.
- Traces/screenshots: `trace.zip`, `screenshots/`.

Credentials:
- The example uses hard-coded credentials in `OccenPlaywrightTest`. Replace with secure env vars before committing.


