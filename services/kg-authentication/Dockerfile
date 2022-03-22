FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/kg-authentication.jar kg-authentication.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-authentication.jar