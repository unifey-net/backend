FROM openjdk:8-jre-slim
EXPOSE 8077

RUN mkdir /backend

COPY build/libs/*.jar /backend/backend.jar

ENTRYPOINT ["java", "-jar", "/backend/backend.jar"]