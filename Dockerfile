FROM openjdk:17-jdk-alpine

RUN apk add --no-cache bash
RUN wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp

WORKDIR /VTMServer

CMD chmod +x gradlew
CMD ./gradlew run
