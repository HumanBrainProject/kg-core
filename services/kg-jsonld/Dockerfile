FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/kg-jsonld.jar kg-jsonld.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /kg-jsonld.jar