# --- Stage 1: Cache Maven Dependencies ---
FROM maven:3.9.6-eclipse-temurin-17 AS deps
WORKDIR /app
# Copy only the pom.xml to download dependencies
COPY pom.xml .
# Download dependencies (this layer will be cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# --- Stage 2: Build the Application ---
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
# Copy the downloaded dependencies from Stage 1
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /app /app
# Copy the actual source code
COPY src ./src
# Package the application (skipping tests for faster deployment builds)
RUN mvn package -DskipTests

# --- Stage 3: Run the Application ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy only the compiled .jar file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port (Render will override this, but it's good practice)
EXPOSE 8080

# Run the application with optimized memory settings
ENTRYPOINT ["java", "-jar", "-XX:+UseG1GC", "app.jar"]