package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class InstagramReelDownloadService {
    private static final Logger logger = LoggerFactory.getLogger(InstagramReelDownloadService.class);

    @Value("${download.directory.video}")
    private String downloadDirectory;

    public String downloadReel(String reelUrl) {
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

            String filename = "instagram_" + Math.abs(reelUrl.hashCode()) + ".mp4";
            String outputFilePath = downloadDirectory + File.separator + filename;

            String shortcode = extractShortcode(reelUrl);
            if (shortcode == null) {
                logger.error("Failed to extract shortcode from URL: " + reelUrl);
                return null;
            }

            List<String> command = new ArrayList<>();
            command.add("python");
            command.add("-m");
            command.add("instaloader");
            command.add("--dirname-pattern");
            command.add(downloadDirectory);
            command.add("--filename-pattern");
            command.add("{shortcode}");
            command.add("--no-metadata-json");
            command.add("--no-compress-json");
            command.add("--no-profile-pic");
            command.add("--no-captions");
            command.add("--");
            command.add("-" + shortcode);

            if (executeCommand(command, shortcode)) {
                String downloadedPath = downloadDirectory + File.separator + shortcode + ".mp4";
                File downloadedFile = new File(downloadedPath);

                if (downloadedFile.exists()) {
                    // Clean up thumbnail file
                    File thumbnailFile = new File(downloadDirectory + File.separator + shortcode + ".jpg");
                    if (thumbnailFile.exists()) {
                        thumbnailFile.delete();
                        logger.debug("Deleted thumbnail file: " + thumbnailFile.getName());
                    }

                    if (downloadedFile.renameTo(new File(outputFilePath))) {
                        logger.info("File renamed and ready at: " + outputFilePath);
                        return outputFilePath;
                    } else {
                        logger.info("File downloaded at: " + downloadedPath);
                        return downloadedPath;
                    }
                }
            }

            logger.error("Failed to download reel from URL: " + reelUrl);
            return null;

        } catch (IOException e) {
            logger.error("Error during reel download: " + e.getMessage(), e);
            return null;
        }
    }

    private String extractShortcode(String url) {
        String pattern = "/reel/([A-Za-z0-9_-]+)";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(url);

        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private boolean executeCommand(List<String> command, String shortcode) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            logger.info("Executing command: " + String.join(" ", command));
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("instaloader: " + line);
                }
            }

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                logger.error("Process timed out after 120 seconds");
                return false;
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                logger.info("Instaloader completed successfully");
                return true;
            } else {
                logger.error("Instaloader failed with exit code: " + exitCode);
                logger.error("Output: " + output.toString());
                return false;
            }

        } catch (IOException e) {
            if (e.getMessage().contains("Cannot run program")) {
                logger.error("Python or instaloader is not installed. Please install Python and run: pip install instaloader");
            } else {
                logger.error("Error executing command: " + e.getMessage());
            }
            return false;
        } catch (InterruptedException e) {
            logger.error("Process was interrupted: " + e.getMessage());
            return false;
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