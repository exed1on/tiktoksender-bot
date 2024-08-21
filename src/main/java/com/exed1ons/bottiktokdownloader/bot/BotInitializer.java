package com.exed1ons.bottiktokdownloader.bot;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Getter
@Component
public class BotInitializer {

    private static final Logger logger = LoggerFactory.getLogger(BotInitializer.class);

    private final TiktokSenderBot tiktokSenderBot;

    public BotInitializer(TiktokSenderBot tiktokSenderBot) {
        this.tiktokSenderBot = tiktokSenderBot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(tiktokSenderBot);
        } catch (Exception e) {
            logger.error("Error while initializing bot", e);
        }
    }
}
