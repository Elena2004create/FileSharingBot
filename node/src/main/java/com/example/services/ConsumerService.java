package com.example.services;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface ConsumerService {
    void consumeTextMessageUpdate(Update update);
    void consumePhotoMessageUpdate(Update update);
    void consumeDocumentMessageUpdate(Update update);
}
