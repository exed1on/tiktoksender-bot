package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SendVideoService {
    private static final Logger logger = LoggerFactory.getLogger(SendVideoService.class);

    private final TikTokDownloadService tikTokDownloadService;

    @Value("${download.directory.video}")
    private String downloadedVideoPath;

    public SendVideoService(TikTokDownloadService tikTokDownloadService) {
        this.tikTokDownloadService = tikTokDownloadService;
    }

    public InputFile getVideo(String tikTokUrl) {


        String videoId = extractVideoId(tikTokUrl);
        tikTokDownloadService.downloadVideo(videoId);

        if (videoId == null) {
            logger.error("Failed to extract video ID from URL: " + tikTokUrl);
            return null;
        }

        String videoFilePath = downloadedVideoPath + File.separator + videoId + ".mp4";

        File videoFile = new File(videoFilePath);
        if (videoFile.exists()) {
            logger.info("Video successfully downloaded and found at: " + videoFilePath);
            return new InputFile(videoFile);
        } else {
            logger.error("Failed to download video. File not found at: " + videoFilePath);
            return null;
        }
    }

    public String extractVideoId(String tikTokUrl) {
        String pattern = "(?:https?:\\/\\/)?(?:www\\.)?tiktok\\.com\\/[^\\/]+\\/video\\/([0-9]+)|https:\\/\\/vm\\.tiktok\\.com\\/([A-Za-z0-9]+)";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(tikTokUrl);

        if (matcher.find()) {
            String videoId = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            logger.info("Extracted video ID: {}", videoId);
            return videoId;
        } else {
            logger.warn("Failed to extract video ID from URL: {}", tikTokUrl);
            return null;
        }
    }
}
