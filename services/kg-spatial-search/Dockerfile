FROM openjdk:11-jre-slim
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} kg-spatial-search.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-spatial-search.jar