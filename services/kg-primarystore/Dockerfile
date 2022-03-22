FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/kg-primarystore.jar kg-primarystore.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-primarystore.jar