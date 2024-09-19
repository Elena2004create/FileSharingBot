package com.example.controllers;

import com.example.services.UpdateProducer;
import com.example.utils.MessageUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.example.models.RabbitQueue.*;

@Component
@Log4j2
public class UpdateController {
    private TelegramBot telegramBot;
    private MessageUtils messageUtils;
    private UpdateProducer updateProducer;

    public UpdateController(MessageUtils messageUtils, UpdateProducer updateProducer) {
        this.messageUtils = messageUtils;
        this.updateProducer = updateProducer;
    }

    public void registerTelegramBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update) {
        if (update == null) {
            log.error("Update is null");
        }
        assert update != null;
        if (update.hasMessage()) {
            destributeMessageByType(update);
        }
        else {
            log.error("Received unsupported type: " + update);
        }
    }

    private void destributeMessageByType(Update update) {
        var message = update.getMessage();
        if (message.hasText()) {
            processTextMessage(update);
        }
        else if (message.hasDocument()){
            processDocumentMessage(update);
        }
        else if (message.hasPhoto()){
            processPhotoMessage(update);
        }
        else {
            sendUnsupportedMessageTypeView(update);
        }
    }

    private void sendUnsupportedMessageTypeView(Update update) {
        var sendMessage = messageUtils.generateSendMessage(update, "Unsupported message type");
        setView(sendMessage);
    }

    public void setView(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }

    private void setFileIsReceivedView(Update update) {
        var sendMessage = messageUtils.generateSendMessage(update, "File is processed");
        setView(sendMessage);
    }

    private void processPhotoMessage(Update update) {
        updateProducer.produce(PHOTO_MESSAGE_UPDATE, update);
        setFileIsReceivedView(update);
    }

    private void processDocumentMessage(Update update) {
        updateProducer.produce(DOC_MESSAGE_UPDATE, update);
        setFileIsReceivedView(update);
    }

    private void processTextMessage(Update update) {
        updateProducer.produce(TEXT_MESSAGE_UPDATE, update);

    }
}
