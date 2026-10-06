FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -B -Dsurefire.useFile=false -Dfailsafe.useFile=false "-Dtest=!IntegrationHarnessTest" verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/test-harness-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001

ENTRYPOINT ["java", "-jar", "app.jar"]
