package com.exed1ons.bottiktokdownloader.service;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class InstagramReelDownloadService {
    private static final Logger logger = LoggerFactory.getLogger(InstagramReelDownloadService.class);

    @Value("${download.directory.video}")
    private String downloadDirectory;

    private static final String cobaltApiUrl = "https://api.cobalt.tools/api/json";

    public void downloadReel(String reelUrl) {
        String outputFilePath = downloadDirectory + File.separator + Math.abs(reelUrl.hashCode()) + ".mp4";

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

            HttpURLConnection connection = getHttpURLConnection(reelUrl);

            logger.info("Sending request to Cobalt API for reel: " + reelUrl);
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String downloadUrl = getDownloadUrl(connection);

                downloadFileFromUrl(downloadUrl, outputFilePath);

            } else {
                logger.warn("Failed to request reel download. HTTP response code: " + responseCode);
            }
        } catch (IOException | JSONException e) {
            logger.warn("Error during reel download: " + e.getMessage());
        }
    }

    private static String getDownloadUrl(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("url");
    }

    private static HttpURLConnection getHttpURLConnection(String reelUrl) throws IOException {
        String requestBody = String.format("{\"url\": \"%s\", \"vCodec\": \"h264\", \"vQuality\": \"720\", \"aFormat\": \"mp3\"}", reelUrl);

        URL url = new URL(cobaltApiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    private void downloadFileFromUrl(String fileUrl, String outputFilePath) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");

            logger.info("Downloading file from: " + fileUrl);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    logger.info("Reel downloaded successfully to: " + outputFilePath);
                }
            } else {
                logger.warn("Failed to download reel. HTTP response code: " + responseCode);
            }
        } catch (IOException e) {
            logger.warn("Error during file download: " + e.getMessage());
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