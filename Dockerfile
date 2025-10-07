# Use OpenJDK 21 as base image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew build -x test

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-Dserver.port=${PORT:-8080}", "-Dspring.profiles.active=prod", "-jar", "build/libs/yogaBE-0.0.1-SNAPSHOT.jar"]