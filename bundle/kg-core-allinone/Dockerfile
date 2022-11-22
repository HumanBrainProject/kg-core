FROM  eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/kg-core-allinone.jar kg-core-allinone.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-core-allinone.jar