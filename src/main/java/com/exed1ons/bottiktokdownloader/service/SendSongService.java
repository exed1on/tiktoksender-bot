package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;

@Service
public class SendSongService {
    private static final Logger logger = LoggerFactory.getLogger(SendSongService.class);

    private final SpotifySongDownloadService spotifySongDownloadService;

    @Value("${download.directory.audio}")
    private String downloadedVideoPath;

    public SendSongService(SpotifySongDownloadService spotifySongDownloadService) {
        this.spotifySongDownloadService = spotifySongDownloadService;
    }

    public InputFile getSong(String songUrl) {

        boolean downloadSuccess = spotifySongDownloadService.downloadSong(songUrl);

        if (downloadSuccess) {
            File directory = new File(downloadedVideoPath);
            File[] mp3Files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

            if (mp3Files != null && mp3Files.length == 1) {
                File mp3File = mp3Files[0];
                logger.info("Audio successfully downloaded and found at: " + mp3File.getAbsolutePath());
                return new InputFile(mp3File);
            } else {
                logger.error("Failed to find the audio file in the directory: " + downloadedVideoPath);
                return null;
            }
        } else {
            logger.error("Download was not successful.");
            return null;
        }
    }

    public void deleteFile(String fileName) {
        String videoFilePath = downloadedVideoPath + File.separator + fileName;
        spotifySongDownloadService.deleteFile(videoFilePath);
    }
}
