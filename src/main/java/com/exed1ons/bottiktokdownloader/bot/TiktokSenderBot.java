package com.exed1ons.bottiktokdownloader.bot;

import com.exed1ons.bottiktokdownloader.persistence.entity.GroupMember;
import com.exed1ons.bottiktokdownloader.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Setter
@Getter
@Component
public class TiktokSenderBot extends TelegramLongPollingBot {

    private final String botName;
    private final String botToken;

    private final SendTikTokService sendTikTokService;
    private final TikTokLinkConverter tikTokLinkConverter;
    private final SendReelService sendReelService;
    private final ImageToMp4Converter imageToMp4Converter;
    private final SendSongService sendSongService;
    private final TikTokSlideDownloadService tikTokSlideDownloadService;
    private final Mp4ToGifConverter mp4ToGifConverter;
    private final GroupMemberService groupMemberService;
    private final RoleNameService roleNameService;

    private static final Logger logger = LoggerFactory.getLogger(TiktokSenderBot.class);

    public TiktokSenderBot(@Value("${bot.username}") String botName, @Value("${bot.token}") String botToken,
                           SendTikTokService sendTikTokService, TikTokLinkConverter tikTokLinkConverter,
                           SendReelService sendReelService, ImageToMp4Converter imageToMp4Converter,
                           SendSongService sendSongService, TikTokSlideDownloadService tikTokSlideDownloadService,
                           Mp4ToGifConverter mp4ToGifConverter, GroupMemberService groupMemberService,
                           RoleNameService roleNameService) {

        super(botToken);
        this.botName = botName;
        this.botToken = botToken;
        this.sendTikTokService = sendTikTokService;
        this.tikTokLinkConverter = tikTokLinkConverter;
        this.sendReelService = sendReelService;
        this.imageToMp4Converter = imageToMp4Converter;
        this.sendSongService = sendSongService;
        this.tikTokSlideDownloadService = tikTokSlideDownloadService;
        this.mp4ToGifConverter = mp4ToGifConverter;
        this.groupMemberService = groupMemberService;
        this.roleNameService = roleNameService;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String messageText = message.getText();
                logger.info("Received message: " + messageText);
                logger.info("From: " + message.getChatId());
                String chatId = message.getChatId().toString();

                if (messageText.equals("/initrole")) {
                    initializeGroupMembers(chatId);
                } else if (messageText.matches("@\\w+")) {
                    String roleName = messageText.substring(1);
                    tagMembersByRole(chatId, roleName);
                } else if (messageText.equals("/gif") && message.isReply()) {
                    handleGifCommand(message);
                } else if (messageText.startsWith("/addrole")) {
                    addRole(messageText, chatId);
                } else if (messageText.startsWith("/setrole")) {
                    assignRole(message, messageText, chatId);
                } else if (messageText.startsWith("/removerole")) {
                    removeRole(messageText, chatId);
                } else if (messageText.startsWith("/unsetrole")) {
                    unsetRole(message, messageText, chatId);
                } else {
                    processMessage(message);
                }
            }
        }
    }

    private void addRole(String messageText, String chatId) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length > 1) {
            String roleName = parts[1].trim();
            roleNameService.addRole(roleName);
        } else {
            sendMessage(chatId, "Usage: /addrole <role>");
        }
    }

    private void assignRole(Message message, String messageText, String chatId) {
        List<MessageEntity> entities = message.getEntities();
        String[] parts = messageText.split(" ", 3);

        if (parts.length > 2 && entities != null) {
            String roleName = parts[1].trim();
            Optional<Long> userIdOpt = extractUserIdFromMessage(message, entities);

            if (userIdOpt.isPresent()) {
                Long userId = userIdOpt.get();
                try {
                    groupMemberService.assignRoleToMember(userId, roleName);
                    sendMessage(chatId, "Role '" + roleName + "' has been assigned to the mentioned user.");
                } catch (EntityNotFoundException e) {
                    sendMessage(chatId, e.getMessage());
                }
            } else {
                sendMessage(chatId, "User not found or mention is invalid.");
            }
        } else {
            sendMessage(chatId, "Usage: /setrole <role> <@username>");
        }
    }

    private void removeRole(String messageText, String chatId) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length > 1) {
            String roleName = parts[1].trim();
            if (roleNameService.removeRoleByName(roleName).isPresent()) {
                sendMessage(chatId, "Role '" + roleName + "' has been removed.");
            } else {
                sendMessage(chatId, "Role '" + roleName + "' not found.");
            }
        }
    }

    private void unsetRole(Message message, String messageText, String chatId) {
        List<MessageEntity> entities = message.getEntities();
        String[] parts = messageText.split(" ", 3);

        if (parts.length > 2 && entities != null) {
            String roleName = parts[1].trim();
            Optional<Long> userIdOpt = extractUserIdFromMessage(message, entities);

            if (userIdOpt.isPresent()) {
                Long userId = userIdOpt.get();
                try {
                    groupMemberService.removeRoleFromMember(userId, roleName);
                    sendMessage(chatId, "Role '" + roleName + "' has been removed from the mentioned user.");
                } catch (EntityNotFoundException e) {
                    sendMessage(chatId, e.getMessage());
                }
            } else {
                sendMessage(chatId, "User not found or mention is invalid.");
            }
        } else {
            sendMessage(chatId, "Usage: /unsetrole <role> <@username>");
        }
    }

    private Optional<Long> extractUserIdFromMessage(Message message, List<MessageEntity> entities) {
        for (MessageEntity entity : entities) {
            if (entity.getType().equals("text_mention")) {
                return Optional.of(entity.getUser().getId());
            } else if (entity.getType().equals("mention")) {
                String username = message.getText().substring(entity.getOffset(), entity.getOffset() + entity.getLength()).substring(1);
                return groupMemberService.findByUserName(username).map(GroupMember::getId);
            }
        }
        return Optional.empty();
    }

    public void initializeGroupMembers(String chatId) {
        try {
            List<ChatMember> members = execute(new GetChatAdministrators(chatId));
            for (ChatMember member : members) {
                User user = member.getUser();
                if (groupMemberService.findById(user.getId()).isEmpty()) {
                    groupMemberService.addMember(user.getId(), user.getUserName(), user.getFirstName());
                }
            }
            logger.info("Group members initialized and added to the database.");
        } catch (TelegramApiException e) {
            logger.error("Failed to initialize group members", e);
        }
    }

    private void handleGifCommand(Message message) {
        Message repliedMessage = message.getReplyToMessage();
        if (repliedMessage.hasPhoto()) {
            PhotoSize photo = repliedMessage.getPhoto().get(repliedMessage.getPhoto().size() - 1);
            sendGif(message.getChatId().toString(),
                    imageToMp4Converter.createMp4FromImage(
                            downloadImage(photo.getFileId())));
        } else if (repliedMessage.hasVideo()) {
            Video video = repliedMessage.getVideo();
            sendGif(message.getChatId().toString(),
                    mp4ToGifConverter.convertMp4ToGif(downloadVideo(video.getFileId())));
        } else {
            sendMessage(message.getChatId().toString(),
                    "/gif command should be used with a photo reply only");
        }
    }

    private void tagMembersByRole(String chatId, String roleName) {
        List<GroupMember> membersWithRole;
        if (roleName.equals("all") || roleName.equals("everyone")) {
            membersWithRole = groupMemberService.findAll();
        } else {
            membersWithRole = groupMemberService.findByRoleName(roleName);
        }

        StringBuilder mentionText = new StringBuilder();
        for (GroupMember member : membersWithRole) {
            if (member.getUserName() != null) {
                mentionText.append("@").append(member.getUserName()).append(" ");
            } else {
                mentionText.append("<a href=\"tg://user?id=")
                        .append(member.getId())
                        .append("\">")
                        .append(Optional.ofNullable(member.getFirstName()).orElse("incognito"))
                        .append("</a> ");
            }
        }
        sendHtmlMessage(chatId, mentionText.toString());
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

            Pattern tiktokPhotoPattern = Pattern.compile("https://www.tiktok.com/@[^/]+/photo/[0-9]+");
            Matcher tiktokPhotoMatcher = tiktokPhotoPattern.matcher(text);
            if (tiktokPhotoMatcher.find()) {
                link = tiktokPhotoMatcher.group();
            }

            Pattern instagramReelPattern = Pattern.compile("https://www.instagram.com/reel/[A-Za-z0-9-_]+");
            Matcher instagramReelMatcher = instagramReelPattern.matcher(text);
            if (instagramReelMatcher.find()) {
                link = instagramReelMatcher.group();
            }

            Pattern spotifyTrackPattern = Pattern.compile("https://open.spotify.com/track/[A-Za-z0-9]+(\\?si=[A-Za-z0-9]+)?");
            Matcher spotifyTrackMatcher = spotifyTrackPattern.matcher(text);
            if (spotifyTrackMatcher.find()) {
                link = spotifyTrackMatcher.group();
            }

            if (link == null) {
                logger.warn("Link is null in message: " + text);
                return;
            }

            String chatId = message.getChatId().toString();

            if (link.contains("tiktok.com/@")) {
                handleTikTokLink(link, chatId);
            } else if (link.contains("instagram.com/reel")) {
                handleInstagramReel(link, chatId);
            } else if (link.contains("open.spotify.com/track")) {
                handleSpotifyTrack(link, chatId);
            } else {
                logger.warn("No valid URL found in message: " + text);
            }
        }
    }

    private void handleTikTokLink(String link, String chatId) {
        if (link.contains("/photo/")) {
            logger.info("Processing TikTok photo link: " + link);
            List<String> downloadedPhotos = tikTokSlideDownloadService.downloadSlides(link);
            String downloadedAudio = tikTokSlideDownloadService.downloadAudio(link);

            if (downloadedPhotos != null && !downloadedPhotos.isEmpty()) {
                processTikTokPhotos(chatId, downloadedPhotos);
            } else {
                logger.warn("No photos were downloaded from the TikTok photo link: " + link);
            }

            if (downloadedAudio != null && !downloadedAudio.isEmpty()) {
                java.io.File file = new java.io.File(downloadedAudio);
                InputFile audioFile = new InputFile(file);
                sendTikTokPhotoAudio(chatId, audioFile);
            } else {
                logger.warn("No audio was downloaded from the TikTok photo link: " + link);
            }

        } else if (link.contains("/video/")) {
            String videoId = sendTikTokService.extractVideoId(link);

            if (videoId != null) {
                logger.info("Extracted video ID: " + videoId + " from link: " + link);
                InputFile videoFile = sendTikTokService.getVideo(link);
                if (videoFile != null) {
                    sendTikTokVideo(chatId, videoFile);
                } else {
                    logger.error("Failed to get video file from link: " + link);
                }
            } else {
                logger.error("Failed to extract video ID from link: " + link);
            }
        } else {
            logger.warn("Unrecognized TikTok link format: " + link);
        }
    }

    private void handleInstagramReel(String link, String chatId) {
        logger.info("Processing Instagram Reel link: " + link);
        InputFile reelVideo = sendReelService.getVideo(link);
        if (reelVideo != null) {
            sendReelVideo(chatId, reelVideo);
        } else {
            logger.error("Failed to download Instagram Reel from link: " + link);
        }
    }

    private void handleSpotifyTrack(String link, String chatId) {
        logger.info("Processing Spotify track link: " + link);
        InputFile trackAudio = sendSongService.getSong(link);
        if (trackAudio != null) {
            sendAudio(chatId, trackAudio);
        } else {
            logger.error("Failed to get audio file from Spotify link: " + link);
        }
    }

    private void processTikTokPhotos(String chatId, List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            sendMessage(chatId, "No photos were found or downloaded.");
            return;
        }

        List<InputMediaPhoto> mediaGroup = new ArrayList<>();
        List<String> messageIds = new ArrayList<>();
        int mediaGroupCapacity = 10;

        for (String imagePath : imagePaths) {
            java.io.File photoFile = new java.io.File(imagePath);

            if (photoFile.exists() && photoFile.isFile()) {
                try {
                    assignImageToMediaAlbum(chatId, photoFile, mediaGroup, messageIds);
                    if (mediaGroup.size() == mediaGroupCapacity) {
                        sendMediaGroup(chatId, mediaGroup);
                        deleteMessages(chatId, messageIds);
                        mediaGroup.clear();
                        messageIds.clear();
                    }
                } catch (Exception e) {
                    logger.error("Failed to upload photo: " + imagePath, e);
                    sendMessage(chatId, "Failed to upload photo: " + photoFile.getName());
                }
            } else {
                logger.error("File does not exist or is not a valid file: " + imagePath);
                sendMessage(chatId, "File does not exist or is not valid: " + photoFile.getName());
            }
        }

        if (mediaGroup.size() > 1) {
            sendMediaGroup(chatId, mediaGroup);

            deleteMessages(chatId, messageIds);
        } else {
            logger.warn("Only one photo was found. Sending as a single photo.");
        }

        deleteMediaAlbum(imagePaths);
    }

    private void assignImageToMediaAlbum(String chatId, java.io.File
            photoFile, List<InputMediaPhoto> mediaGroup, List<String> messageIds) throws TelegramApiException {
        InputFile inputFile = new InputFile(photoFile);
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(inputFile);

        Message sentMessage = execute(sendPhoto);
        String fileId = sentMessage.getPhoto().get(sentMessage.getPhoto().size() - 1).getFileId();
        String messageId = sentMessage.getMessageId().toString();

        mediaGroup.add(new InputMediaPhoto(fileId));
        messageIds.add(messageId);

        logger.info("Uploaded photo and obtained file ID: " + fileId);
    }

    private static void deleteMediaAlbum(List<String> imagePaths) {
        for (String imagePath : imagePaths) {
            java.io.File photoFile = new java.io.File(imagePath);
            if (photoFile.exists() && photoFile.isFile()) {
                if (photoFile.delete()) {
                    logger.info("Deleted file: " + imagePath);
                } else {
                    logger.error("Failed to delete file: " + imagePath);
                }
            }
        }
    }

    private void deleteMessages(String chatId, List<String> messageIds) {
        for (String messageId : messageIds) {
            try {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId);
                deleteMessage.setMessageId(Integer.parseInt(messageId));

                execute(deleteMessage);
                logger.info("Deleted message with ID: " + messageId);
            } catch (TelegramApiException e) {
                logger.error("Failed to delete message with ID: " + messageId, e);
            }
        }
    }

    private void sendMediaGroup(String chatId, List<InputMediaPhoto> mediaGroup) {
        SendMediaGroup sendMediaGroup = new SendMediaGroup();
        sendMediaGroup.setChatId(chatId);
        sendMediaGroup.setMedias(new ArrayList<>(mediaGroup));

        try {
            execute(sendMediaGroup);
            logger.info("Media group sent to " + chatId);

        } catch (TelegramApiException e) {
            logger.error("Failed to send media group to " + chatId, e);
            for (InputMediaPhoto media : mediaGroup) {
                logger.error("Media file details: " + media.getMedia() + " | Type: " + media.getClass().getName());
            }
        }
    }

    public BufferedImage downloadImage(String fileId) {
        try {
            File telegramFile = execute(new GetFile(fileId));
            String filePath = telegramFile.getFilePath();

            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;

            return ImageIO.read(new URL(fileUrl));
        } catch (TelegramApiException | IOException e) {
            logger.error("Failed to download image from Telegram: ", e);
            return null;
        }
    }

    public InputFile downloadVideo(String fileId) {
        try {
            File telegramFile = execute(new GetFile(fileId));
            String filePath = telegramFile.getFilePath();

            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;

            java.io.File tempVideoFile = java.io.File.createTempFile("temp_video", ".mp4");

            try (InputStream inputStream = new URL(fileUrl).openStream();
                 OutputStream outputStream = new FileOutputStream(tempVideoFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            logger.info("Video downloaded successfully: " + tempVideoFile.getAbsolutePath());

            return new InputFile(tempVideoFile);
        } catch (TelegramApiException | IOException e) {
            logger.error("Failed to download video from Telegram: ", e);
            return null;
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

    public void sendHtmlMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error while sending message", e);
        }
    }

    public void sendTikTokPhotoAudio(String chatId, InputFile audioFile) {
        SendAudio message = new SendAudio();
        message.setChatId(chatId);
        message.setAudio(audioFile);

        try {
            execute(message);
            tikTokSlideDownloadService.deleteFile(audioFile.getMediaName());
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
            sendSongService.deleteFile(audioFile.getMediaName());
        } catch (TelegramApiException e) {
            logger.error("Error while sending message", e);
        }
    }

    public String sendVideo(String chatId, InputFile videoFile) {
        SendVideo message = new SendVideo();
        message.setChatId(chatId);
        message.setVideo(videoFile);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error while sending message", e);
        }

        return videoFile.getMediaName();
    }

    public void sendTikTokVideo(String chatId, InputFile videoFile) {
        String fileName = sendVideo(chatId, videoFile);

        try {
            logger.info("Deleting video file: " + fileName);
            if (fileName != null) {
                sendTikTokService.deleteVideoFile(fileName);
            }
        } catch (Exception e) {
            logger.error("Error while deleting video file", e);
        }
    }

    public void sendReelVideo(String chatId, InputFile videoFile) {
        String fileName = sendVideo(chatId, videoFile);

        try {
            logger.info("Deleting video file: " + fileName);
            if (fileName != null) {
                sendReelService.deleteVideoFile(fileName);
            }
        } catch (Exception e) {
            logger.error("Error while deleting video file", e);
        }
    }

    public void sendGif(String chatId, InputFile mp4File) {
        SendAnimation message = new SendAnimation();
        message.setChatId(chatId);
        message.setAnimation(mp4File);

        try {
            execute(message);
            logger.info("Deleting video file...");
            imageToMp4Converter.deleteOutputFile();
        } catch (TelegramApiException e) {
            logger.error("Error while sending GIF", e);
        }
    }
}
