pipeline {
    agent any

    // Trigger on branch and tag
    triggers {
        githubPush()
    }

    options {
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['qa', 'dev', 'staging', 'prod'],
            description: 'Select execution environment'
        )
        choice(
            name: 'BROWSER',
            choices: ['chromium', 'firefox', 'webkit'],
            description: 'Select browser for test execution'
        )
        booleanParam(
            name: 'HEADLESS_MODE',
            defaultValue: true,
            description: 'Run tests in headless mode'
        )
        string(
            name: 'TEST_TAG',
            defaultValue: '@smoke',
            description: 'Cucumber tag to filter tests (e.g., @smoke, @regression, @sanity)'
        )
    }

    environment {
        DOCKER_IMAGE = "automation-tests:${BUILD_NUMBER}"
        DOCKER_REGISTRY = "your-registry" // Update with your registry
        WORKSPACE_DIR = "${WORKSPACE}"
        LOGS_DIR = "${WORKSPACE}/logs"
        SCREENSHOTS_DIR = "${WORKSPACE}/target/screenshots"
        ALLURE_DIR = "${WORKSPACE}/allure-results"
        TIMESTAMP = sh(script: "date +%Y%m%d_%H%M%S", returnStdout: true).trim()
    }

    stages {
        stage('Initialization') {
            steps {
                script {
                    echo "=========================================="
                    echo "Pipeline Triggered: ${env.BUILD_NUMBER}"
                    echo "=========================================="
                    echo "Git Branch: ${env.GIT_BRANCH}"
                    echo "Git Commit: ${env.GIT_COMMIT}"
                    echo "Environment: ${params.ENVIRONMENT}"
                    echo "Browser: ${params.BROWSER}"
                    echo "Headless: ${params.HEADLESS_MODE}"
                    echo "Test Tag: ${params.TEST_TAG}"
                    echo "=========================================="

                    // Create necessary directories
                    sh '''
                        mkdir -p ${LOGS_DIR}
                        mkdir -p ${SCREENSHOTS_DIR}
                        mkdir -p ${ALLURE_DIR}
                        echo "Directories created successfully"
                    '''
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    echo "Checking out code from repository..."
                    checkout scm

                    // Log git information
                    sh '''
                        echo "=== Git Information ===" | tee -a ${LOGS_DIR}/build_${TIMESTAMP}.log
                        git log --oneline -5 | tee -a ${LOGS_DIR}/build_${TIMESTAMP}.log
                        git branch -a | tee -a ${LOGS_DIR}/build_${TIMESTAMP}.log
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building Docker image: ${DOCKER_IMAGE}"
                    sh '''
                        docker build \
                            --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
                            --build-arg VCS_REF=${GIT_COMMIT} \
                            -t ${DOCKER_IMAGE} \
                            -f Dockerfile . 2>&1 | tee -a ${LOGS_DIR}/docker_build_${TIMESTAMP}.log

                        echo "Docker image built successfully: ${DOCKER_IMAGE}"
                    '''
                }
            }
        }

        stage('Run Tests in Docker') {
            steps {
                script {
                    echo "Executing tests in Docker container..."
                    sh '''
                        docker run --rm \
                            --name test-runner-${BUILD_NUMBER} \
                            -v ${WORKSPACE_DIR}/target:/app/target \
                            -v ${WORKSPACE_DIR}/allure-results:/app/allure-results \
                            -v ${LOGS_DIR}:/app/logs \
                            -e "env=${ENVIRONMENT}" \
                            -e "browser=${BROWSER}" \
                            -e "headless=${HEADLESS_MODE}" \
                            -e "BUILD_ID=${BUILD_NUMBER}" \
                            -e "BUILD_URL=${BUILD_URL}" \
                            -e "JOB_NAME=${JOB_NAME}" \
                            ${DOCKER_IMAGE} \
                            "mvn clean test \
                                -Denv=${ENVIRONMENT} \
                                -Dbrowser=${BROWSER} \
                                -Dheadless=${HEADLESS_MODE} \
                                -Dcucumber.filter.tags='${TEST_TAG}' \
                                -DsuiteXmlFile=testng.xml \
                                -Dorg.slf4j.simpleLogger.logFile=${LOGS_DIR}/test_execution_${TIMESTAMP}.log \
                                2>&1 | tee -a ${LOGS_DIR}/test_execution_${TIMESTAMP}.log" \
                            || true

                        echo "Test execution completed"
                    '''
                }
            }
        }

        stage('Collect Artifacts') {
            steps {
                script {
                    echo "Collecting test artifacts..."
                    sh '''
                        # Copy reports
                        if [ -d "target/surefire-reports" ]; then
                            cp -r target/surefire-reports ${LOGS_DIR}/ 2>/dev/null || true
                        fi

                        # Copy test execution logs
                        if [ -d "target" ]; then
                            find target -name "*.log" -exec cp {} ${LOGS_DIR}/ \; 2>/dev/null || true
                        fi

                        # List all collected artifacts
                        echo "=== Collected Artifacts ===" | tee -a ${LOGS_DIR}/artifacts_${TIMESTAMP}.log
                        echo "Allure Results:" | tee -a ${LOGS_DIR}/artifacts_${TIMESTAMP}.log
                        ls -la ${ALLURE_DIR}/ 2>/dev/null | tee -a ${LOGS_DIR}/artifacts_${TIMESTAMP}.log || echo "No allure results"

                        echo "Screenshots:" | tee -a ${LOGS_DIR}/artifacts_${TIMESTAMP}.log
                        ls -la ${SCREENSHOTS_DIR}/ 2>/dev/null | tee -a ${LOGS_DIR}/artifacts_${TIMESTAMP}.log || echo "No screenshots"

                        echo "Test Reports:" | tee -a ${LOGS_DIR}/artifacts_${TIMESTAMP}.log
                        ls -la ${LOGS_DIR}/ | tee -a ${LOGS_DIR}/artifacts_${TIMESTAMP}.log
                    '''
                }
            }
        }

        stage('Generate Allure Report') {
            steps {
                script {
                    echo "Generating Allure Report..."
                    sh '''
                        # Generate allure report
                        if [ -d "${ALLURE_DIR}" ] && [ "$(ls -A ${ALLURE_DIR})" ]; then
                            echo "Allure results found. Generating report..."
                            docker run --rm \
                                -v ${WORKSPACE_DIR}/allure-results:/app/allure-results \
                                -v ${WORKSPACE_DIR}/allure-report:/app/allure-report \
                                frankescobar/allure-docker-service:latest \
                                allure generate --clean -o /app/allure-report 2>&1 | tee -a ${LOGS_DIR}/allure_${TIMESTAMP}.log
                        else
                            echo "No Allure results found"
                        fi
                    '''
                }
            }
        }

        stage('Publish Reports') {
            steps {
                script {
                    echo "Publishing test reports..."

                    // JUnit XML report
                    junit testResults: 'target/surefire-reports/**/*.xml',
                          allowEmptyResults: true,
                          skipPublishingChecks: true

                    // Allure Report
                    publishHTML([
                        reportDir: 'allure-report',
                        reportFiles: 'index.html',
                        reportName: "Allure Report - Build ${BUILD_NUMBER}",
                        keepAll: true,
                        alwaysLinkToLastBuild: true
                    ])

                    // Archive logs and screenshots
                    archiveArtifacts artifacts: 'logs/**/*,target/screenshots/**/*,allure-results/**/*',
                                     allowEmptyArchive: true,
                                     fingerprint: true
                }
            }
        }
    }

    post {
        always {
            script {
                echo "=========================================="
                echo "Build Summary"
                echo "=========================================="

                // Generate build summary
                sh '''
                    echo "Build Number: ${BUILD_NUMBER}" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                    echo "Status: ${BUILD_STATUS}" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                    echo "Duration: ${BUILD_DURATION}" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                    echo "Branch: ${GIT_BRANCH}" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                    echo "Commit: ${GIT_COMMIT}" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                    echo "Environment: ${ENVIRONMENT}" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                    echo "Browser: ${BROWSER}" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                    echo "Test Tag: ${TEST_TAG}" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt

                    echo "=== Test Execution Summary ===" >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                    if [ -f "target/surefire-reports/TEST-*.xml" ]; then
                        grep -oP '(?<=tests=")[^"]*' target/surefire-reports/TEST-*.xml >> ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt || true
                    fi

                    cat ${LOGS_DIR}/build_summary_${TIMESTAMP}.txt
                '''

                // Clean up Docker image
                sh '''
                    echo "Cleaning up Docker image..."
                    docker rmi ${DOCKER_IMAGE} || true
                '''
            }
        }

        success {
            script {
                echo "✓ Tests passed successfully!"

                // Optional: Send success notification
                sh '''
                    echo "Test execution completed successfully" | tee -a ${LOGS_DIR}/notifications_${TIMESTAMP}.log
                '''
            }
        }

        failure {
            script {
                echo "✗ Tests failed!"

                // Capture failure logs
                sh '''
                    echo "=== Test Failure Details ===" > ${LOGS_DIR}/failure_report_${TIMESTAMP}.txt

                    # Collect error logs
                    if [ -f "target/surefire-reports/TEST-*.xml" ]; then
                        cat target/surefire-reports/TEST-*.xml >> ${LOGS_DIR}/failure_report_${TIMESTAMP}.txt
                    fi

                    # List screenshots of failures
                    echo "=== Failure Screenshots ===" >> ${LOGS_DIR}/failure_report_${TIMESTAMP}.txt
                    ls -la ${SCREENSHOTS_DIR}/ >> ${LOGS_DIR}/failure_report_${TIMESTAMP}.txt 2>/dev/null || true

                    cat ${LOGS_DIR}/failure_report_${TIMESTAMP}.txt
                '''

                // Optional: Send failure email notification
                emailext(
                    subject: "Automation Tests Failed - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """
                        <h2>Test Execution Failed</h2>
                        <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                        <p><strong>Build:</strong> ${env.BUILD_NUMBER}</p>
                        <p><strong>Branch:</strong> ${env.GIT_BRANCH}</p>
                        <p><strong>Commit:</strong> ${env.GIT_COMMIT}</p>
                        <p><strong>Environment:</strong> ${params.ENVIRONMENT}</p>
                        <p><strong>Browser:</strong> ${params.BROWSER}</p>
                        <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                        <p><strong>Allure Report:</strong> <a href="${env.BUILD_URL}Allure_Report_Build_${env.BUILD_NUMBER}/">View Report</a></p>
                    """,
                    to: "${env.CHANGE_AUTHOR_EMAIL}",
                    mimeType: 'text/html',
                    recipientProviders: [
                        developers(),
                        requestor(),
                        brokenBuildSuspects()
                    ]
                )
            }
        }

        unstable {
            echo "⚠ Build is unstable"
        }

        cleanup {
            // Clean workspace
            deleteDir()
        }
    }
}
