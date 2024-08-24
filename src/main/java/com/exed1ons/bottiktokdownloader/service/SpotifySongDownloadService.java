package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class SpotifySongDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifySongDownloadService.class);


    @Value("${download.directory.audio}")
    private String downloadDirectory;

    public boolean downloadSong(String url) {

        String songTitle = null;

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

            String[] command = {"spotdl", url, "--audio", "soundcloud", "slider-kz", "bandcamp", "piped"};

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(downloadDirectory));
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                logger.warn(line);
                if (line.contains("Downloaded")) {
                    songTitle = line.substring(line.indexOf(" - ") + 3, line.lastIndexOf("\""));
                    logger.warn("Song title: " + songTitle);
                }
            }

            if (exitCode == 0) {
                logger.info("Download completed successfully.");
                return true;
            } else {
                logger.info("Download failed. Exit code: " + exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            logger.info("Error executing spotdl command: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteFile(String filePath) {
        if (filePath != null) {
            File fileToDelete = new File(filePath);
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    logger.info("File deleted successfully: " + filePath);
                    return true;
                } else {
                    logger.info("Unable to delete file: " + filePath);
                    return false;
                }
            }
        }
        return false;
    }
}