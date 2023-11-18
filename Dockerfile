FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY target/kafka-router.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]