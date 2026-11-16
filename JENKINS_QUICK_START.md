# Jenkins CI/CD Quick Start Guide

## 5-Minute Setup Checklist

### ✅ Pre-Setup (One-time)

- [ ] Docker installed on Jenkins server
- [ ] Jenkins installed with plugins:
  - GitHub/GitLab plugin
  - Docker Pipeline
  - Email Extension
  - HTML Publisher
  - JUnit Plugin
- [ ] Webhook configured in GitHub/GitLab

### ✅ Jenkins Job Setup

1. **Create new Pipeline job**
   - Name: `automation-tests`
   - Type: Pipeline

2. **Configure SCM**
   - SCM: Git
   - Repo URL: `https://github.com/yourorg/yourrepo.git`
   - Credentials: Add if private
   - Script Path: `Jenkinsfile`

3. **Save job**

---

## How It Works

### Trigger #1: Git Push
```bash
git commit -m "Fix login bug"
git push origin feature/login
# ✓ Jenkins automatically runs
```

### Trigger #2: Git Tag
```bash
git tag -a v1.2.3 -m "Release"
git push origin v1.2.3
# ✓ Jenkins automatically runs
```

### Trigger #3: Manual
Jenkins UI → Build with Parameters → Select options → Build

---

## Build Parameters (When Triggering)

| Parameter | Options | Default |
|-----------|---------|---------|
| ENVIRONMENT | qa, dev, staging, prod | qa |
| BROWSER | chromium, firefox, webkit | chromium |
| HEADLESS_MODE | true, false | true |
| TEST_TAG | @smoke, @regression, @sanity | @smoke |

---

## What Happens During Build

```
Stage 1: Initialization
  ✓ Create logs, screenshots, allure directories
  ✓ Log git commit info

Stage 2: Checkout
  ✓ Pull code from git

Stage 3: Build Docker Image
  ✓ Build Dockerfile
  ✓ Pre-install Playwright browsers

Stage 4: Run Tests in Docker
  ✓ Execute tests with selected parameters
  ✓ Capture screenshots on failure
  ✓ Generate Allure results
  ✓ Log all output

Stage 5: Collect Artifacts
  ✓ Gather all logs, screenshots, reports

Stage 6: Generate Allure Report
  ✓ Convert allure-results to HTML report

Stage 7: Publish Reports
  ✓ Publish to Jenkins dashboard
  ✓ Archive logs & screenshots
  ✓ Publish JUnit results
```

---

## Output Locations

All artifacts saved to Jenkins workspace:

```
$WORKSPACE/
├── logs/
│   ├── build_TIMESTAMP.log
│   ├── test_execution_TIMESTAMP.log
│   ├── docker_build_TIMESTAMP.log
│   └── failure_report_TIMESTAMP.log
│
├── target/
│   ├── screenshots/
│   │   └── *.png (failure screenshots)
│   └── surefire-reports/
│       └── TEST-*.xml (JUnit reports)
│
└── allure-report/
    └── index.html (viewable from Jenkins dashboard)
```

---

## View Results

### In Jenkins Dashboard

1. **Latest Build**: Click build number
2. **Allure Report**: Link in left sidebar (after build completes)
3. **Logs**: "Console Output" tab
4. **Artifacts**: "Artifacts" section

### Console Output Shows

```
========== Build Initialization ==========
Build #123
Git Branch: feature/login
Git Commit: abc1234
Environment: qa
Browser: chromium
=========================================

[Building Docker Image]
Docker image built successfully: automation-tests:123

[Running Tests]
...test execution output...

[Allure Report Generated]
...allure generation output...

[Test Summary]
Tests Run: 45
Tests Passed: 43
Tests Failed: 2
Test Failures:
  - test_login_invalid_password
  - test_checkout_total_calculation
```

---

## Common Commands

### Manually Run from Command Line

```bash
# Build Docker image
docker build -t automation-tests:manual .

# Run all tests (QA)
docker run --rm \
  -v $(pwd)/allure-results:/app/allure-results \
  -v $(pwd)/logs:/app/logs \
  -v $(pwd)/target/screenshots:/app/target/screenshots \
  automation-tests:manual \
  "mvn clean test -Denv=qa -Dbrowser=chromium -Dheadless=true"

# Run only smoke tests
docker run --rm \
  -v $(pwd)/allure-results:/app/allure-results \
  automation-tests:manual \
  "mvn clean test -Denv=qa -Dcucumber.filter.tags='@smoke'"

# Run with Firefox in headed mode (debugging)
docker run --rm \
  -v $(pwd)/allure-results:/app/allure-results \
  automation-tests:manual \
  "mvn clean test -Denv=dev -Dbrowser=firefox -Dheadless=false"
```

---

## Troubleshooting

### Build Doesn't Trigger
**Check**: Webhook in GitHub/GitLab → Recent Deliveries
- Should see 200 response
- If 404/500: Check webhook URL matches Jenkins

### Tests Fail in Docker but Work Locally
**Reasons**: 
- Missing browser dependencies
- Network differences
- File path differences

**Fix**: 
```bash
docker run -it automation-tests:manual bash
# Debug inside container
```

### Allure Report Empty
**Check**: Is `allure-results/` being created?
```bash
docker exec test-runner-123 ls /app/allure-results/
```

### Screenshots Not Captured
**Check**: Test failure hook in `Hooks.java`
```java
@After
public void tearDown(Scenario scenario) {
    if (scenario.isFailed()) {
        // Screenshot captured here
    }
}
```

---

## Email Notifications

### Who Gets Notified

On **failure**:
- Build author (who committed)
- Change requester (who opened PR)
- Previous build breaker

Email includes:
- Build number & status
- Git branch & commit
- Environment used
- Link to Jenkins build
- **Link to Allure report** ← Most important

### Customize Email

Edit in Jenkinsfile:
```groovy
emailext(
    subject: "My Custom Subject",
    body: "Custom message",
    to: "team@company.com"  // Or use variables
)
```

---

## Test Tags Strategy

### Smoke Tests (Quick - 5 min)
```gherkin
@smoke
Scenario: User can log in
```
Run on every commit

### Regression Tests (Full - 30 min)
```gherkin
@regression
Scenario: Complete checkout flow
```
Run on release branches

### Skip in CI
```gherkin
@wip @skip
Scenario: Experimental feature
```
Only run locally

### Example: Run Only Regression
Jenkins → Build Parameters → TEST_TAG = `@regression`

---

## Performance Tips

### Parallel Execution
Already configured in `junit-platform.properties`:
```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.fixed.parallelism=4
```

Tests run 4 at a time = faster builds

### Cache Docker Layers
First build: ~3-5 min
Subsequent builds: ~1-2 min (layers cached)

### Reduce Test Count
Filter by tag:
```
TEST_TAG = "@smoke"  # Only 5-10 tests
vs
TEST_TAG = "@regression"  # All 50+ tests
```

---

## Sample Workflow

### Day 1: Commit & Push
```bash
git checkout -b feature/add-wishlist
# Make changes
git add .
git commit -m "Add wishlist feature"
git push origin feature/add-wishlist
```

**Jenkins automatically triggers** → Runs tests → Posts Allure report link

### Result
```
✓ All 45 tests passed
Allure report: http://jenkins:8080/job/automation-tests/123/Allure_Report_Build_123/
```

### If Tests Fail
```
✗ 2 tests failed
Failure screenshots attached to Allure report
```

Dev opens report → Sees failure screenshots → Fixes issue → Pushes again

### Release Day
```bash
git tag -a v2.0.0 -m "Production release"
git push origin v2.0.0
```

**Jenkins triggers** → Runs full @regression suite → Generates report for stakeholders

---

## Checklist: First Time Setup

- [ ] Docker installed: `docker --version`
- [ ] Dockerfile exists: `Dockerfile`
- [ ] Jenkinsfile exists: `Jenkinsfile`
- [ ] Webhook configured: GitHub/GitLab settings
- [ ] Jenkins plugins installed (see above)
- [ ] Test locally works: `mvn clean test`
- [ ] Test in Docker works: `docker build & docker run`
- [ ] Jenkins job created
- [ ] First push triggers build: `git push origin main`
- [ ] Build succeeds: Check Jenkins dashboard
- [ ] Allure report visible: Click report link
- [ ] ✓ Done!

---

## Next: Optimize & Extend

Once working, you can:
- Add more environments (staging, prod)
- Add performance tests
- Add parallel browsers
- Add Slack notifications
- Add deployment steps
- Add rollback on failure

---

## Quick Help

| Issue | Solution |
|-------|----------|
| Build won't start | Check webhook delivery |
| Docker build slow | First build is slow, ~3-5 min |
| Tests timeout | Increase timeout in Jenkinsfile |
| No Allure report | Verify allure-results has content |
| Email not sent | Check email config in Manage Jenkins |
| Need help? | See CI_CD_SETUP.md for detailed guide |

---

**Good luck! 🚀**
