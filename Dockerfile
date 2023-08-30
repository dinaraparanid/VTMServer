# Replace `17` with your project's java version
FROM gradle:8-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN export GRADLE_OPTS="-Djdk.lang.Process.launchMechanism=vfork"
RUN gradle shadowJar --no-daemon

# Replace `17` with your project's java version
FROM openjdk:17

RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/VTMServer.jar

EXPOSE 8080

# Start point in your app
# In my case it was not required
# ENTRYPOINT ["java","-jar","/app/vtm-server.jar"]