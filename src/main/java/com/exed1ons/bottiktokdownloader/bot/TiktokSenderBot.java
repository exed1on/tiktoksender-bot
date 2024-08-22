package com.exed1ons.bottiktokdownloader.bot;

import com.exed1ons.bottiktokdownloader.service.SendReelService;
import com.exed1ons.bottiktokdownloader.service.SendVideoService;
import com.exed1ons.bottiktokdownloader.service.TikTokLinkConverter;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Setter
@Getter
@Component
public class TiktokSenderBot extends TelegramLongPollingBot {

    private String botName;
    private String botToken;

    private final SendVideoService sendVideoService;
    private final TikTokLinkConverter tikTokLinkConverter;
    private final SendReelService sendReelService;

    private static final Logger logger = LoggerFactory.getLogger(TiktokSenderBot.class);

    public TiktokSenderBot(@Value("${bot.username}") String botName, @Value("${bot.token}") String botToken, SendVideoService sendVideoService, TikTokLinkConverter tikTokLinkConverter, SendReelService sendReelService) {

        super(botToken);
        this.botName = botName;
        this.botToken = botToken;
        this.sendVideoService = sendVideoService;
        this.tikTokLinkConverter = tikTokLinkConverter;
        this.sendReelService = sendReelService;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            logger.info("Received message: " + message.getText());
            logger.info("From: " + message.getChatId());

            processMessage(message);
        }
    }

    private void processMessage(Message message) {
        if (message.hasText()) {
            String text = message.getText();
            String link = null;

            Pattern shortUrlPattern = Pattern.compile("https://vm.tiktok.com/[A-Za-z0-9]+");
            Matcher shortUrlMatcher = shortUrlPattern.matcher(text);
            if (shortUrlMatcher.find()) {
                link = shortUrlMatcher.group();

                try {
                    link = tikTokLinkConverter.expandUrlUsingApi(link);
                } catch (IOException e) {
                    logger.error("Failed to resolve short URL: " + link);
                }
            }

            Pattern longUrlPattern = Pattern.compile("https://www.tiktok.com/@[^/]+/video/[0-9]+");
            Matcher longUrlMatcher = longUrlPattern.matcher(text);
            if (longUrlMatcher.find()) {
                link = longUrlMatcher.group();
            }

            Pattern instagramReelPattern = Pattern.compile("https://www.instagram.com/reel/[A-Za-z0-9-_]+");
            Matcher instagramReelMatcher = instagramReelPattern.matcher(text);
            if (instagramReelMatcher.find()) {
                link = instagramReelMatcher.group();
            }

            if (link != null) {
                if (link.contains("tiktok.com")) {
                    String videoId = sendVideoService.extractVideoId(link);
                    if (videoId != null) {
                        sendVideo(message.getChatId().toString(), sendVideoService.getVideo(link));
                    } else {
                        logger.error("Failed to extract video ID from link: " + link);
                    }
                }

                else if (link.contains("instagram.com/reel")) {
                    InputFile reelVideo = sendReelService.getVideo(link);
                    if (reelVideo != null) {
                        sendVideo(message.getChatId().toString(), reelVideo);
                    } else {
                        logger.error("Failed to download Instagram Reel from link: " + link);
                    }
                }
            } else {
                logger.warn("No valid TikTok or Instagram URL found in message: " + text);
            }
        }
    }


    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error while sending message", e);
        }
    }

    public void sendAudio(String chatId, InputFile audioFile) {
        SendAudio message = new SendAudio();
        message.setChatId(chatId);
        message.setAudio(audioFile);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error while sending message", e);
        }
    }

    public void sendVideo(String chatId, InputFile videoFile) {
        SendVideo message = new SendVideo();
        message.setChatId(chatId);
        message.setVideo(videoFile);

        try {
            execute(message);

            String fileName = videoFile.getMediaName();
            logger.info("Deleting video file: " + fileName);
            if (fileName != null) {
                sendVideoService.deleteVideoFile(fileName);
            }
        } catch (TelegramApiException e) {
            logger.error("Error while sending message", e);
        }
    }

}
