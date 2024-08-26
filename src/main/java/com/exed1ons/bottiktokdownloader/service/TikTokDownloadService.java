package com.exed1ons.bottiktokdownloader.service;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class TikTokDownloadService {
    private static final Logger logger = LoggerFactory.getLogger(TikTokDownloadService.class);

    @Value("${download.directory.video}")
    private String downloadDirectory;

    private static final String PRE_DOWNLOAD_URL = "https://ssstik.io/en-1";

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

    public void sendPreDownloadRequest(String tiktokUrl) {

        String postUrl = "https://ssstik.io/abc?url=dl";
        String formData = "id=" + encodeValue(tiktokUrl) + "&locale=en&tt=YXZLVm01";

        try {
            URL url = new URL(postUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("DNT", "1");
            connection.setRequestProperty("Host", "ssstik.io");
            connection.setRequestProperty("HX-Current-URL", "https://ssstik.io/en-1");
            connection.setRequestProperty("HX-Request", "true");
            connection.setRequestProperty("HX-Target", "target");
            connection.setRequestProperty("HX-Trigger", "_gcaptcha_pt");
            connection.setRequestProperty("Origin", "https://ssstik.io");
            connection.setRequestProperty("Priority", "u=0");
            connection.setRequestProperty("Referer", "https://ssstik.io/en-1");
            connection.setRequestProperty("Sec-Fetch-Dest", "empty");
            connection.setRequestProperty("Sec-Fetch-Mode", "cors");
            connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
            connection.setRequestProperty("Sec-GPC", "1");
            connection.setRequestProperty("TE", "trailers");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:129.0) Gecko/20100101 Firefox/129.0");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(formData.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            logger.info("Pre-download request sent. Response code: " + responseCode);
        } catch (Exception e) {
            logger.error("Error during form submission: " + e.getMessage(), e);
        }
    }

    private String encodeValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error encoding value: " + e.getMessage(), e);
            return value;
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
