FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/kg-ids.jar kg-ids.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-ids.jar