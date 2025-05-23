package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;

@Service
public class SendReelService {
    private static final Logger logger = LoggerFactory.getLogger(SendReelService.class);

    private final InstagramReelDownloadService instagramReelDownloadService;

    @Value("${download.directory.video}")
    private String downloadedVideoPath;

    public SendReelService(InstagramReelDownloadService instagramReelDownloadService) {
        this.instagramReelDownloadService = instagramReelDownloadService;
    }

    public InputFile getVideo(String reelUrl) {
        String videoFilePath = instagramReelDownloadService.downloadReel(reelUrl);

        if (videoFilePath != null) {
            File videoFile = new File(videoFilePath);
            if (videoFile.exists()) {
                logger.info("Video successfully downloaded and found at: " + videoFilePath);
                return new InputFile(videoFile);
            } else {
                logger.error("Downloaded file not found at: " + videoFilePath);
                return null;
            }
        } else {
            logger.error("Failed to download video from URL: " + reelUrl);
            return null;
        }
    }

    public void deleteVideoFile(String fileName) {
        String videoFilePath = downloadedVideoPath + File.separator + fileName;
        instagramReelDownloadService.deleteFile(videoFilePath);
    }
}