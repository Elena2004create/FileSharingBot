package com.example.services.impl;

import com.example.dao.RawDataDAO;
import com.example.models.RawData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashSet;
import java.util.Set;

@SpringBootTest
public class MainServiceImplTest {
    @Autowired
    private RawDataDAO rawDataDAO;
    @Test
    public void testSaveData(){
        Update update = new Update();
        Message message = new Message();
        message.setText("test");
        update.setMessage(message);
        RawData rawData = RawData.builder().event(update).build();
        Set<RawData> testData = new HashSet<>();
        testData.add(rawData);
        rawDataDAO.save(rawData);

        Assert.isTrue(testData.contains(rawData), "Entity is not found");
    }
}
