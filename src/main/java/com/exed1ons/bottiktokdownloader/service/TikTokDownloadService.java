package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class TikTokDownloadService {
    private static final Logger logger = LoggerFactory.getLogger(TikTokDownloadService.class);

    @Value("${download.directory.video}")
    private String downloadDirectory;

    public void downloadVideo(String videoId) {
        String videoUrl = "https://tikcdn.io/ssstik/" + videoId;
        String outputFilePath = downloadDirectory + File.separator + videoId + ".mp4";

        try {
            File directory = new File(downloadDirectory);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    logger.info("Download directory created: " + downloadDirectory);
                } else {
                    logger.error("Failed to create download directory: " + downloadDirectory);
                    throw new IOException("Unable to create download directory: " + downloadDirectory);
                }
            }

            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");

            logger.info("Sending request to: " + videoUrl);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    logger.info("Video downloaded successfully to: " + outputFilePath);
                }
            } else {
                logger.warn("Failed to download video. HTTP response code: " + responseCode);
            }
        } catch (IOException e) {
            logger.warn("Error during video download: " + e.getMessage());
        }
    }

    public void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                logger.info("File deleted successfully: " + filePath);
            } else {
                logger.error("Failed to delete file: " + filePath);
            }
        } else {
            logger.warn("File not found for deletion: " + filePath);
        }
    }
}
