FROM adoptopenjdk:11-jre-hotspot
VOLUME /tmp
COPY ./target/propagationsidecar-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]