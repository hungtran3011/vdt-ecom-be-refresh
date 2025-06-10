FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the POM file first and download dependencies
# This creates a separate layer that won't be rebuilt unless pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline

# Now copy source code (which changes more frequently)
COPY src/ ./src/

# Build the application
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Install curl for health checks and wait script
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/vdt-ecom-be-refresh-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/* /app/src/main/resources/
COPY wait-for-keycloak.sh /app/
RUN chmod +x /app/wait-for-keycloak.sh

ENTRYPOINT ["/app/wait-for-keycloak.sh", "keycloak", "8080", "ecom", "java", "-jar", "app.jar"]