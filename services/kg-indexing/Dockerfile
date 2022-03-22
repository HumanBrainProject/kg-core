FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/kg-indexing.jar kg-indexing.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-indexing.jar