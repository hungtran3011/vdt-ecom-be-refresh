FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# Run build and show what's created
RUN mvn clean package -DskipTests && ls -la target/

FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the specific JAR file and rename it
COPY --from=build /app/target/vdt-ecom-be-refresh-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/*.properties /app/src/main/resources/
# Verify the JAR exists
RUN ls -la
ENTRYPOINT ["java", "-jar", "app.jar"]