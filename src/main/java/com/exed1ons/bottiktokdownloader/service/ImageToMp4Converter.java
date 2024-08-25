package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class ImageToMp4Converter {

    private static final Logger logger = LoggerFactory.getLogger(ImageToMp4Converter.class);

    private static final String GIF_OUTPUT_DIR = "gif_output";
    private static final String OUTPUT_FILE = GIF_OUTPUT_DIR + File.separator + "output.mp4";

    public InputFile createMp4FromImage(BufferedImage image) {

        try {
            File directory = new File(GIF_OUTPUT_DIR);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    logger.info("Download directory created: " + GIF_OUTPUT_DIR);
                } else {
                    logger.error("Failed to create download directory: " + GIF_OUTPUT_DIR);
                    throw new IOException("Unable to create download directory: " + GIF_OUTPUT_DIR);
                }
            }

            File tempImageFile = File.createTempFile("temp_image", ".png");
            ImageIO.write(image, "png", tempImageFile);

            String[] command = {
                    "ffmpeg",
                    "-loop", "1",
                    "-i", tempImageFile.getAbsolutePath(),
                    "-c:v", "libx264",
                    "-t", "1",
                    "-pix_fmt", "yuv420p",
                    OUTPUT_FILE
            };

            Process process = new ProcessBuilder(command).inheritIO().start();
            process.waitFor();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                }
            }

            tempImageFile.delete();
            return new InputFile(new File(OUTPUT_FILE));

        } catch (IOException | InterruptedException e) {
            logger.error("Error during image to mp4 conversion: " + e.getMessage());
        }
        return null;
    }

    public void deleteOutputFile(String filePath) {
        File file = new File(GIF_OUTPUT_DIR + File.separator + filePath);
        if (file.exists()) {
            if (file.delete()) {
                logger.info("File deleted successfully: " + filePath);
            } else {
                logger.error("Failed to delete file: " + filePath);
            }
        }
    }
}