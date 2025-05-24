FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17.0.1-jdk-slim

RUN apt-get update && apt-get install -y python3 python3-pip ffmpeg && \
    pip3 install --upgrade pip && \
    pip3 install spotdl && \
    pip3 install --upgrade yt-dlp && \
    pip3 install --upgrade spotdl && \
    pip3 install instaloader

ENV SPOTDL_PATH /usr/local/bin/spotdl

COPY --from=build /target/bot-tiktok-downloader-0.0.1-SNAPSHOT.jar bot-tiktok-downloader.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","bot-tiktok-downloader.jar"]