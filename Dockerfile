# Build Stage
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copy pom.xml and fetch dependencies (improves caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source files and compile
COPY src ./src
RUN mvn clean package -DskipTests -B

# Run Stage
FROM openjdk:17-slim
WORKDIR /app

# Expose port
EXPOSE 8080

# Copy jar from build stage
COPY --from=build /app/target/tracker-0.0.1-SNAPSHOT.jar app.jar

# Run command
ENTRYPOINT ["java", "-jar", "app.jar"]
