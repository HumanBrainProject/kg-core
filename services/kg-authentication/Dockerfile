FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/kg-authentication.jar kg-authentication.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-authentication.jar