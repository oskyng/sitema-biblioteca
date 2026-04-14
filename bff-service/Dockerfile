FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/bff-service-0.0.1-SNAPSHOT.jar bff.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "bff.jar"]
