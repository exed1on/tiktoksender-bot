package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class TikTokLinkConverter {

    private static final Logger logger = LoggerFactory.getLogger(TikTokLinkConverter.class);

    public String expandUrlUsingApi(String shortenedUrl) throws IOException {
        logger.info("Attempting to expand shortened URL: {}", shortenedUrl);

        URL url = new URL(shortenedUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(true);

        try {
            int responseCode = connection.getResponseCode();
            logger.info("Received response code: {}", responseCode);

            String expandedUrl = connection.getURL().toString();
            logger.info("Expanded URL: {}", expandedUrl);
            return expandedUrl;
        } finally {
            connection.disconnect();
        }
    }
}
