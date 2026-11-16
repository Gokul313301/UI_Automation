# Multi-stage build for test execution
FROM openjdk:17-jdk-slim as builder

# Install Maven
RUN apt-get update && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/*

# Final stage
FROM openjdk:17-jdk-slim

# Install Maven
RUN apt-get update && apt-get install -y \
    maven \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Playwright browser dependencies (for headless execution)
RUN apt-get update && apt-get install -y \
    libnss3 \
    libnspr4 \
    libdbus-1-3 \
    libatk1.0-0 \
    libcups2 \
    libxkbcommon0 \
    libpango-1.0-0 \
    libpangoft2-1.0-0 \
    libgbm1 \
    libx11-6 \
    libxext6 \
    libxfixes3 \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Pre-install Playwright browsers
RUN mvn exec:java -e \
    -D exec.mainClass=com.microsoft.playwright.CLI \
    -D exec.args="install" || true

# Create directories for reports and logs
RUN mkdir -p /app/allure-results \
    && mkdir -p /app/logs \
    && mkdir -p /app/target/screenshots

# Set entry point
ENTRYPOINT ["sh", "-c"]
CMD ["mvn clean test"]
