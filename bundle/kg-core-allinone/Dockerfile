FROM openjdk:11-jre-slim
VOLUME /tmp
COPY target/kg-core-allinone.jar kg-core-allinone.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-core-allinone.jar