FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/kg-core-api.jar kg-core-api.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-core-api.jar