# Multi-stage Dockerfile using Java 21 and the project's Maven wrapper (mvnw)

# Build stage: use a JDK 21 image and run the project's maven wrapper to produce the jar
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
# copy wrapper and maven config first to leverage Docker cache
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

# Run stage: copy the jar from the build stage and run it on a lightweight JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

# JVM opts (can be overridden at runtime)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 9091
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
