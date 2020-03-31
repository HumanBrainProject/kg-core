FROM openjdk:11-jre-slim
#Possibility to set JVM options (https://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} kg-graphdb-sync.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-graphdb-sync.jar