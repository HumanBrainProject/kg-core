FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/kg-core-api.jar kg-core-api.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-core-api.jar