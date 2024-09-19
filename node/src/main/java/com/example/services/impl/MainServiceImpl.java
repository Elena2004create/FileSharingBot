package com.example.services.impl;

import com.example.dao.AppUserDAO;
import com.example.dao.RawDataDAO;
import com.example.models.AppUser;
import com.example.models.RawData;
import com.example.models.enums.UserState;
import com.example.services.MainService;
import com.example.services.ProducerService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static com.example.models.enums.UserState.BASIC_STATE;
import static com.example.models.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static com.example.services.enums.ServiceCommands.*;

@Log4j2
@Service
public class MainServiceImpl implements MainService {

    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerService producerService, AppUserDAO appUserDAO) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";
        
        if (CANCEL.equals(text)){
            output = cancelProcess(appUser);
        }
        else if (BASIC_STATE.equals(userState)){
            output = processServiceCommand(appUser, text);
        }
        else if (WAIT_FOR_EMAIL_STATE.equals(userState)){
            //TODO process email
        }
        else{
            log.error("Unsupported state: " + userState);
            output = "Unknown error, write /cancel";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    @Override
    public void processDocumentMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)){
            return;
        }
        var answer = "Document received";

        sendAnswer(answer, chatId);
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()){
            var error = "Register please";
            sendAnswer(error, chatId);
            return true;
        }
        else if (!BASIC_STATE.equals(userState)){
            var error = "Wait for email";
            sendAnswer(error, chatId);
            return true;
        }
        return false;
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)){
            return;
        }
        var answer = "Photo received";

        sendAnswer(answer, chatId);
    }

    private void sendAnswer(String output, Long chatId) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.producerAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String text) {
        if (REGISTRATION.equals(text)) {
            return "Temporarily unavailable";
        } else  if (HELP.equals(text)) {
            return help();
        }
        else if (START.equals(text)){
            return "Good! You start))";
        }
        else {
            return "Unknown command";
        }
    }

    private String help() {
        return "List of available commands:\n"
                + "/cancel - отмена выполнения текущей команды;\n"
                + "/registration - регистрация пользователя.";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Process canceled";
    }

    private AppUser findOrSaveAppUser(Update update) {
        User telegramUser = update.getMessage().getFrom();
        AppUser persistantAppUser = appUserDAO.findAppUserByTelegramId(telegramUser.getId());
        if (persistantAppUser == null) {
            AppUser newAppUser = AppUser.builder()
                    .telegramId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(true)
                    .state(BASIC_STATE)
                    .build();
            return appUserDAO.save(newAppUser);
        }
        return persistantAppUser;
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                                .event(update)
                                .build();

        rawDataDAO.save(rawData);
    }
}
