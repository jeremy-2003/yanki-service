FROM maven:3.8.4-openjdk-11 AS build
WORKDIR /app
COPY . .
RUN mvn clean package
FROM openjdk:11-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
DOCKER EXEC cotainersbootcamp ls /path/to/your/yanki-service-0.0.1-SNAPSHOT.jar.jar
EXPOSE 8088
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]