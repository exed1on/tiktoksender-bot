package com.exed1ons.bottiktokdownloader.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TikTokSlideDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokSlideDownloadService.class);

    public List<String> downloadSlides(String tiktokUrl) {
        logger.info("Starting downloadSlides with URL: " + tiktokUrl); // Entry log
        List<String> downloadedPhotos = new ArrayList<>();
        try {
            String jsonResponse = sendPostRequest(tiktokUrl);
            if (jsonResponse != null) {
                logger.debug("Received JSON response: " + jsonResponse); // Log the received JSON response

                Document doc = Jsoup.parse(jsonResponse);
                Elements downloadLinks = doc.select("a[href]");

                logger.info("Found " + downloadLinks.size() + " links in the response"); // Log number of links found

                for (Element link : downloadLinks) {
                    String imageUrl = link.attr("href");
                    logger.debug("Processing link: " + imageUrl); // Log each link being processed

                    if (imageUrl.contains("tos-maliva-i-photomode-us")) {
                        String downloadedPath = downloadImage(imageUrl);
                        if (downloadedPath != null) {
                            logger.info("Downloaded photo to: " + downloadedPath); // Log downloaded path
                            downloadedPhotos.add(downloadedPath);
                        } else {
                            logger.warn("Failed to download image: " + imageUrl); // Log if download fails
                        }
                    }
                }
            } else {
                logger.warn("Received null response for URL: " + tiktokUrl); // Log null response
            }
        } catch (Exception e) {
            logger.error("Error downloading slides: " + e.getMessage(), e);
        }
        logger.info("Finished downloadSlides with " + downloadedPhotos.size() + " photos downloaded."); // Exit log
        return downloadedPhotos;
    }

    private String sendPostRequest(String tiktokUrl) {
        logger.info("Sending POST request to TikTok API with URL: " + tiktokUrl); // Log POST request initiation
        String apiUrl = "https://ttsave.app/download";
        String jsonPayload = "{\"language_id\":\"1\", \"query\":\"" + tiktokUrl + "\"}";

        try {
            HttpURLConnection conn = getHttpURLConnection(apiUrl, jsonPayload);

            int responseCode = conn.getResponseCode();
            logger.info("POST request response code: " + responseCode); // Log response code

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = conn.getInputStream();
                String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                logger.debug("Received response from TikTok API: " + response); // Log the response from TikTok API
                return response;
            } else {
                logger.error("Failed to get a valid response. HTTP Code: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error sending POST request: " + e.getMessage(), e);
            return null;
        }
    }

    private static HttpURLConnection getHttpURLConnection(String apiUrl, String jsonPayload) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "*/*");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return conn;
    }

    private String downloadImage(String imageUrl) {
        logger.info("Starting download for image: " + imageUrl);
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            logger.info("GET request response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = conn.getInputStream()) {
                    BufferedImage image = ImageIO.read(inputStream);

                    if (image != null) {
                        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1, imageUrl.indexOf("~"));
                        String fileExtension = "jpg";
                        File downloadDir = new File("downloads");

                        if (!downloadDir.exists()) {
                            downloadDir.mkdirs();
                            logger.info("Created download directory: " + downloadDir.getAbsolutePath());
                        }

                        File outputFile = new File(downloadDir, fileName + "." + fileExtension);
                        logger.info("Saving image to file: " + outputFile.getAbsolutePath());

                        ImageIO.write(image, fileExtension, outputFile);
                        logger.info("Image downloaded successfully: " + outputFile.getName());
                        return outputFile.getAbsolutePath();
                    } else {
                        logger.error("Failed to read image from input stream.");
                    }
                }
            } else {
                logger.error("Failed to download image. HTTP response code: " + responseCode);
            }

        } catch (Exception e) {
            logger.error("Error downloading image: " + e.getMessage(), e);
        }
        return null;
    }
}