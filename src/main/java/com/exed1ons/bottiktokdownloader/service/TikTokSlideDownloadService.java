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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TikTokSlideDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokSlideDownloadService.class);

    public List<String> downloadSlides(String tiktokUrl) {
        logger.info("Starting downloadSlides with URL: " + tiktokUrl);
        List<String> downloadedPhotos = new ArrayList<>();
        try {
            String jsonResponse = sendPostRequest(tiktokUrl);
            if (jsonResponse != null) {
                logger.debug("Received JSON response: " + jsonResponse);

                Document doc = Jsoup.parse(jsonResponse);
                Elements downloadLinks = doc.select("img[src]"); // Changed to 'img[src]' for image URLs

                logger.info("Found " + downloadLinks.size() + " image links in the response");

                for (Element link : downloadLinks) {
                    String imageUrl = link.attr("src");
                    logger.debug("Processing link: " + imageUrl);

                    if (imageUrl.contains("tos-maliva-i-photomode-us")) {
                        String downloadedPath = downloadImage(imageUrl);
                        if (downloadedPath != null) {
                            logger.info("Downloaded photo to: " + downloadedPath);
                            downloadedPhotos.add(downloadedPath);
                        } else {
                            logger.warn("Failed to download image: " + imageUrl);
                        }
                    }
                }
            } else {
                logger.warn("Received null response for URL: " + tiktokUrl);
            }
        } catch (Exception e) {
            logger.error("Error downloading slides: " + e.getMessage(), e);
        }
        logger.info("Finished downloadSlides with " + downloadedPhotos.size() + " photos downloaded.");
        return downloadedPhotos;
    }

    private String sendPostRequest(String tiktokUrl) {
        logger.info("Sending POST request to TikTok API with URL: " + tiktokUrl);
        String apiUrl = "https://tiktokio.cc/api/v1/tk-htmx";
        String prefix = "dtGslxrcdcG9raW8uY2MO0O0O";
        String vid = tiktokUrl;

        String formData = String.format("prefix=%s&vid=%s",
                URLEncoder.encode(prefix, StandardCharsets.UTF_8),
                URLEncoder.encode(vid, StandardCharsets.UTF_8));

        try {
            HttpURLConnection conn = getHttpURLConnection(apiUrl, formData);

            int responseCode = conn.getResponseCode();
            logger.info("POST request response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = conn.getInputStream();
                String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                logger.debug("Received response from TikTok API: " + response);

                logger.info("Response: " + response);
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

    private static HttpURLConnection getHttpURLConnection(String apiUrl, String formData) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "*/*");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = formData.getBytes(StandardCharsets.UTF_8);
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
            logger.info(conn.getResponseMessage());

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