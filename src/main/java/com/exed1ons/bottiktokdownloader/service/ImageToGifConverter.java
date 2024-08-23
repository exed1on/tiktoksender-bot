package com.exed1ons.bottiktokdownloader.service;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

@Service
public class ImageToGifConverter {

    private static final Logger logger = LoggerFactory.getLogger(ImageToGifConverter.class);

    private static final String GIF_OUTPUT_DIR = "images";

    public InputFile createGifFromImage(BufferedImage image) {
        String gifFilePath = GIF_OUTPUT_DIR + File.pathSeparator + "output.gif";
        try (FileOutputStream outputStream = new FileOutputStream(gifFilePath)) {
            AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
            gifEncoder.setQuality(10);
            gifEncoder.setRepeat(0);
            gifEncoder.setDelay(500);
            gifEncoder.setSize(image.getWidth(), image.getHeight());

            gifEncoder.start(outputStream);
            gifEncoder.addFrame(image);
            gifEncoder.finish();

            return new InputFile(Paths.get(gifFilePath).toFile());
        } catch (IOException e) {
            logger.error("Failed to create GIF from image: ", e);
            return null;
        }
    }
}