FROM eclipse-temurin:17 as jre-build

# Create a custom Java runtime
# see https://dzone.com/articles/ways-to-reduce-jvm-docker-image-size
RUN $JAVA_HOME/bin/jlink \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

# Define your base image
FROM debian:buster-slim
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

# Continue with your application deployment
COPY target/kafka-router.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]