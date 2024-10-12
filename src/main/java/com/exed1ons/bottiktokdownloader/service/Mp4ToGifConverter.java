package com.exed1ons.bottiktokdownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class Mp4ToGifConverter {

    private static final Logger logger = LoggerFactory.getLogger(Mp4ToGifConverter.class);

    private static final String GIF_OUTPUT_DIR = "gif_output";
    private static final String TEMP_DIR = "temp";
    private static final String OUTPUT_FILE_PREFIX = "output_";
    private static final String OUTPUT_FILE_SUFFIX = ".gif";

    public Mp4ToGifConverter() {
        File outputDir = new File(GIF_OUTPUT_DIR);
        if (!outputDir.exists()) {
            if (outputDir.mkdirs()) {
                logger.info("GIF output directory created: " + GIF_OUTPUT_DIR);
            } else {
                logger.error("Failed to create GIF output directory: " + GIF_OUTPUT_DIR);
            }
        }

        File tempDir = new File(TEMP_DIR);
        if (!tempDir.exists()) {
            if (tempDir.mkdirs()) {
                logger.info("Temporary directory created: " + TEMP_DIR);
            } else {
                logger.error("Failed to create temporary directory: " + TEMP_DIR);
            }
        }
    }

    public InputFile convertMp4ToGif(InputFile mp4File) {
        if (mp4File == null) {
            logger.error("Invalid InputFile provided for conversion.");
            return null;
        }

        File inputFile = mp4File.getNewMediaFile();
        if (!inputFile.exists() || !inputFile.isFile()) {
            logger.error("Input MP4 file does not exist or is not a valid file: " + inputFile.getAbsolutePath());
            return null;
        }

        String uniqueId = UUID.randomUUID().toString();
        String outputFileName = OUTPUT_FILE_PREFIX + uniqueId + OUTPUT_FILE_SUFFIX;
        File outputFile = new File(GIF_OUTPUT_DIR, outputFileName);

        String[] command = {
                "ffmpeg",
                "-y",
                "-i", inputFile.getAbsolutePath(),
                "-vf", "fps=10,scale=320:-1:flags=lanczos",
                "-gifflags", "+transdiff",
                "-pix_fmt", "rgba",
                "-f", "gif",
                outputFile.getAbsolutePath()
        };

        logger.info("Starting MP4 to GIF conversion: " + inputFile.getAbsolutePath() + " -> " + outputFile.getAbsolutePath());

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("ffmpeg: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("ffmpeg conversion failed with exit code: " + exitCode);
                return null;
            }

            if (!outputFile.exists()) {
                logger.error("ffmpeg did not create the output GIF file.");
                return null;
            }

            logger.info("MP4 to GIF conversion successful: " + outputFile.getAbsolutePath());

            return new InputFile(outputFile);

        } catch (IOException | InterruptedException e) {
            logger.error("Error during MP4 to GIF conversion: ", e);
            return null;
        }
    }

    public void deleteGifFile(File gifFile) {
        if (gifFile != null && gifFile.exists()) {
            if (gifFile.delete()) {
                logger.info("Deleted GIF file: " + gifFile.getAbsolutePath());
            } else {
                logger.error("Failed to delete GIF file: " + gifFile.getAbsolutePath());
            }
        }
    }

    public void cleanupGifOutputDirectory() {
        File dir = new File(GIF_OUTPUT_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith(OUTPUT_FILE_PREFIX) && name.endsWith(OUTPUT_FILE_SUFFIX));
        if (files != null) {
            for (File file : files) {
                deleteGifFile(file);
            }
        }
    }
}