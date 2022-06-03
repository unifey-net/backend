FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM openjdk:11
EXPOSE 8077:8077
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/unifey-backend.jar
ENTRYPOINT ["java","-jar","/app/unifey-backend.jar"]