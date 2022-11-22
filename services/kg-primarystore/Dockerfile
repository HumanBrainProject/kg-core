FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/kg-primarystore.jar kg-primarystore.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-primarystore.jar