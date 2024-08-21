package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class TikTokLinkConverter {

    private static final Logger logger = LoggerFactory.getLogger(TikTokLinkConverter.class);

    private static final String API_URL = "https://unshorten.me/s/";

    public String expandUrlUsingApi(String shortenedUrl) throws IOException {
        logger.info("Attempting to expand shortened URL using API: {}", shortenedUrl);

        String apiUrl = API_URL + shortenedUrl;
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = connection.getResponseCode();
        logger.info("Received response code: {}", responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                String expandedUrl = content.toString();
                logger.info("Expanded URL: {}", expandedUrl);
                return expandedUrl;
            }
        } else {
            logger.error("Failed to expand URL. Response code: {}", responseCode);
            return null;
        }
    }
}
