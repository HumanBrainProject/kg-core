FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/kg-inference.jar kg-inference.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-inference.jar