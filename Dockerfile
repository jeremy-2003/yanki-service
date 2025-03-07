FROM openjdk:11-jdk
WORKDIR /app
COPY target/yanki-service-*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]