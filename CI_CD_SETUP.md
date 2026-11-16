# CI/CD Pipeline Setup Guide

## Overview
This document describes the complete CI/CD flow with Jenkins, Docker, logs, screenshots, and Allure reports.

---

## Architecture Flow

```
Developer pushes code to GitHub/GitLab branch + tag
              ↓
         GitHub/GitLab webhook
              ↓
        Jenkins triggered
              ↓
    Jenkins checks out code
              ↓
   Jenkins builds Docker image
              ↓
Docker container runs tests
              ↓
Tests generate:
- Allure results (allure-results/)
- Screenshots on failure (target/screenshots/)
- Test logs (logs/)
- JUnit XML reports (target/surefire-reports/)
              ↓
Jenkins collects artifacts
              ↓
Allure report generated
              ↓
Reports published to Jenkins dashboard
              ↓
Email notification to developer (on failure)
```

---

## Files Created

### 1. **Dockerfile**
Containerizes the test framework with:
- Java 17 + Maven
- Playwright browser dependencies
- Pre-installed browsers
- Volume mounts for reports/logs/screenshots

**Key features:**
- Headless execution ready
- Allure results directory
- Screenshot capture directory
- Logs directory

### 2. **Jenkinsfile**
Complete CI/CD pipeline with:
- **Parameters**: Environment, Browser, Headless mode, Test tag filter
- **Triggers**: GitHub/GitLab webhook (branch & tag)
- **Stages**:
  - Initialization (setup directories)
  - Checkout (code fetch)
  - Build Docker Image
  - Run Tests in Docker
  - Collect Artifacts
  - Generate Allure Report
  - Publish Reports
- **Post Actions**:
  - Success: Notification
  - Failure: Email with details + Allure link
  - Always: Cleanup + Docker image removal

### 3. **.dockerignore**
Optimizes Docker builds by excluding:
- .git, .idea, logs, target, node_modules
- IDE files, OS files

### 4. **junit-platform.properties**
JUnit/Cucumber configuration:
- Parallel execution (4 threads)
- Random test order
- Dry-run disabled

### 5. **allure.properties**
Allure report configuration:
- Results directory mapping
- Report title
- Link patterns (optional)

### 6. **.gitignore**
Excludes from git:
- Build artifacts (target/, *.jar)
- IDE files (.idea/, .vscode/)
- Test results (allure-results/)
- Logs and screenshots
- Environment files

---

## Prerequisites

### Local Machine
- Docker installed
- Docker Desktop running
- Git configured

### Jenkins Server
- Jenkins installed
- Docker plugin installed
- Email plugin installed (for notifications)
- Allure plugin installed (optional but recommended)
- GitHub/GitLab plugin installed

---

## Setup Instructions

### Step 1: Install Jenkins Plugins

In Jenkins, go to **Manage Jenkins** → **Manage Plugins**:

```
Required Plugins:
- GitHub Plugin (or GitLab Plugin)
- Docker Pipeline Plugin
- Email Extension Plugin
- JUnit Plugin
- HTML Publisher Plugin
- Allure Plugin (optional)
```

### Step 2: Configure GitHub/GitLab Webhook

#### For GitHub:
1. Go to Repository → Settings → Webhooks
2. Add Webhook:
   - Payload URL: `http://jenkins-server/github-webhook/`
   - Content type: `application/json`
   - Trigger: Push events + Tag push

#### For GitLab:
1. Go to Project → Settings → Integrations
2. Add Webhook:
   - URL: `http://jenkins-server/project/your-project`
   - Trigger: Push events + Tag push

### Step 3: Create Jenkins Pipeline Job

1. **New Item** → **Pipeline**
2. **Pipeline** section:
   - Definition: `Pipeline script from SCM`
   - SCM: Git
   - Repository URL: `https://github.com/yourorg/yourrepo.git`
   - Credentials: Add if private repo
   - Script Path: `Jenkinsfile`
   - Lightweight checkout: Unchecked

3. **Build Triggers**:
   - ✓ GitHub hook trigger for GITScm polling
   - OR
   - ✓ GitLab Push/Tag push triggers

### Step 4: Configure Email Notifications

**Manage Jenkins** → **Configure System** → **Extended E-mail Notification**:

```
SMTP Server: smtp.gmail.com (or your mail server)
SMTP Port: 587
Use SMTP Authentication: ✓
Username: your-email@gmail.com
Password: your-app-password
Use TLS: ✓
Default From: jenkins-notifications@yourcompany.com
```

### Step 5: Update Jenkinsfile (Optional Customizations)

Update these in Jenkinsfile:

```groovy
DOCKER_REGISTRY = "your-registry"  // For pushing images to registry
emailext(
    to: "${env.CHANGE_AUTHOR_EMAIL}"  // Change if needed
)
```

---

## How to Trigger

### Option 1: Push to Branch
```bash
git push origin main
# Jenkins automatically runs
```

### Option 2: Push Tag
```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
# Jenkins automatically runs
```

### Option 3: Manual Trigger
1. Go to Jenkins job
2. Click **Build with Parameters**
3. Select:
   - **ENVIRONMENT**: qa / dev / staging / prod
   - **BROWSER**: chromium / firefox / webkit
   - **HEADLESS_MODE**: true / false
   - **TEST_TAG**: @smoke / @regression / @sanity
4. Click **Build**

---

## Execution Flow Details

### Test Parameters (From Jenkinsfile)

```
Environment: Controls which .properties file is loaded
- qa.properties
- dev.properties
- staging.properties (create if needed)
- prod.properties (create if needed)

Browser: Which browser to use
- chromium (default)
- firefox
- webkit

Headless Mode: Run without UI
- true (for CI/CD)
- false (for debugging, headed mode)

Test Tag: Filter Cucumber scenarios
- @smoke (quick sanity tests)
- @regression (full suite)
- @sanity (basic checks)
```

### Docker Execution

The Docker container runs with these environment variables:

```
env=${ENVIRONMENT}              # qa/dev/staging/prod
browser=${BROWSER}              # chromium/firefox/webkit
headless=${HEADLESS_MODE}       # true/false
BUILD_ID=${BUILD_NUMBER}        # Jenkins build number
BUILD_URL=${BUILD_URL}          # Link to Jenkins build
JOB_NAME=${JOB_NAME}            # Jenkins job name
```

These are available to your test code via `ConfigManager.get("key")`.

---

## Artifact Collection

### What Gets Captured

```
├── logs/
│   ├── build_TIMESTAMP.log          # Build initialization
│   ├── docker_build_TIMESTAMP.log   # Docker build output
│   ├── test_execution_TIMESTAMP.log # Test execution logs
│   ├── allure_TIMESTAMP.log         # Allure generation
│   ├── surefire-reports/            # JUnit XML reports
│   └── failure_report_TIMESTAMP.log # Failure details (if failed)
│
├── target/screenshots/              # Failure screenshots
│   ├── test1_failure.png
│   ├── test2_failure.png
│   └── ...
│
└── allure-results/                  # Allure test data
    ├── *.json
    └── attachments/
        ├── screenshots/
        └── logs/
```

### Screenshot Capture

Screenshots are captured on test failure via `Hooks.java`:

```java
@After
public void tearDown(Scenario scenario) {
    if (scenario.isFailed()) {
        String screenshotPath = "target/screenshots/" + scenario.getName() + ".png";
        PlaywrightFactory.getPage().screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotPath)));
        scenario.attach(Files.readAllBytes(Paths.get(screenshotPath)), "image/png", "Failure Screenshot");
    }
    PlaywrightFactory.closeBrowser();
}
```

### Logs

**Test execution logs** include:
- Test execution timeline
- Step definitions executed
- Assertions passed/failed
- Browser actions
- Page source (if needed)

Enable detailed logging in `log4j.properties`:

```properties
log4j.rootLogger=INFO, FILE
log4j.appender.FILE=org.apache.log4j.FileAppender
log4j.appender.FILE.File=logs/test_execution.log
log4j.appender.FILE.layout=org.apache.log4j.PatternLayout
log4j.appender.FILE.layout.ConversionPattern=%d{ABSOLUTE} %5p %c{1}:%L - %m%n
```

---

## Reports & Notifications

### Allure Report

**Generated automatically** during Allure Report stage:

```
Jenkins Dashboard
└── Build #123
    └── Allure Report - Build 123 (link in sidebar)
        └── HTML report with:
            - Test results overview
            - Pass/Fail statistics
            - Timeline
            - Behaviors
            - Failures with screenshots
            - Logs (when attached)
            - Graphs & charts
```

### Email Notification

On **failure**, an email is sent with:

```
Subject: Automation Tests Failed - job-name #123

Body:
- Job Name
- Build Number
- Git Branch
- Git Commit
- Environment
- Browser
- Build URL (clickable)
- Allure Report link (clickable)
```

---

## Running Locally (Without Jenkins)

### Option 1: Docker Only

```bash
# Build
docker build -t automation-tests:local .

# Run with QA environment
docker run --rm \
  -v $(pwd)/allure-results:/app/allure-results \
  -v $(pwd)/logs:/app/logs \
  -v $(pwd)/target/screenshots:/app/target/screenshots \
  -e env=qa \
  -e browser=chromium \
  -e headless=true \
  automation-tests:local \
  "mvn clean test -Denv=qa -Dbrowser=chromium -Dheadless=true"
```

### Option 2: Maven Directly

```bash
# Default (QA environment)
mvn clean test

# Specific environment
mvn clean test -Denv=qa -Dbrowser=firefox -Dheadless=false

# With specific tag
mvn clean test -Dcucumber.filter.tags="@smoke"

# Generate Allure report
mvn allure:serve
```

---

## Troubleshooting

### Jenkins Build Fails to Trigger

**Solution**: Check webhook delivery in GitHub/GitLab:
- Go to Repo Settings → Webhooks → Recent Deliveries
- Check response code (should be 200)
- Re-deliver if needed

### Docker Image Build Fails

**Solution**: Check Docker logs:
```bash
docker build -t automation-tests:debug . 2>&1 | tail -50
```

### Tests Timeout in Docker

**Solution**: Increase timeout in Jenkinsfile:
```groovy
options {
    timeout(time: 120, unit: 'MINUTES')  // Increase from 60
}
```

### Allure Report Not Generated

**Solution**: Check allure-results directory has content:
```bash
docker exec test-runner-123 ls -la /app/allure-results/
```

### Screenshots Not Captured

**Solution**: Ensure Hooks.java is being executed:
```java
@After
public void takeScreenshot(Scenario scenario) {
    if (scenario.isFailed()) {
        // Capture screenshot
    }
}
```

---

## Best Practices

1. **Use Tags**: Tag tests appropriately
   - @smoke - Quick sanity (< 5 min)
   - @regression - Full suite (< 30 min)
   - @sanity - Basic checks
   - @wip - Work in progress (skip in CI)

2. **Parallel Execution**: Set in junit-platform.properties
   ```properties
   junit.jupiter.execution.parallel.enabled=true
   junit.jupiter.execution.parallel.fixed.parallelism=4
   ```

3. **Test Data**: Use QA environment for tests
   ```groovy
   ENVIRONMENT=qa  // Isolated test data
   ```

4. **Branch Strategy**: Use meaningful branch names
   ```
   feature/login-ui
   bugfix/checkout-flow
   release/v1.0.0
   ```

5. **Keep Builds Fast**: Exclude heavy tests from CI
   ```gherkin
   @wip @skip
   Scenario: Slow performance test
   ```

---

## Next Steps

1. **Install Jenkins and plugins** on your server
2. **Configure webhook** in GitHub/GitLab
3. **Create Jenkins pipeline** job with Jenkinsfile
4. **Test locally** with Docker:
   ```bash
   docker build -t automation-tests:test .
   ```
5. **Push a test commit** and watch Jenkins run
6. **View Allure report** in Jenkins dashboard

---

## Support & Documentation

- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/)
- [Docker Documentation](https://docs.docker.com/)
- [Cucumber/Gherkin Syntax](https://cucumber.io/docs/gherkin/)
- [Allure Reports](https://docs.qameta.io/allure/)
- [Playwright Java](https://playwright.dev/java/)
