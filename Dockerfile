FROM gradle:7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

FROM openjdk:17-alpine
EXPOSE 1337:1337
RUN apk add --no-cache bash
RUN wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/vtm-server.jar
CMD java -jar /app/vtm-server.jar
# ENTRYPOINT ["java","-jar","/app/vtm-server.jar"]