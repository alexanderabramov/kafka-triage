FROM openjdk:11-jre-slim
LABEL maintainer="alexander.abramov.pub@gmail.com"
ADD build/libs/kafka-triage-*.jar /usr/share/kafka-triage/kafka-triage.jar
ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/kafka-triage/kafka-triage.jar"]
EXPOSE 8080
