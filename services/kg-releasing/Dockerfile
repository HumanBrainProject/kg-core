FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/kg-releasing.jar kg-releasing.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-releasing.jar