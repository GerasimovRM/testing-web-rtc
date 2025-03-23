# Build stage
FROM docker.puzzle-apps.ru/maven:java-21 AS build
WORKDIR /home/app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package


# Package stage
FROM eclipse-temurin:21-jre
COPY --from=build /home/app/target/auth-stub-1.0.0.jar /usr/local/lib/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/app.jar"]
