FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/kg-system-test.jar kg-system-test.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-system-test.jar