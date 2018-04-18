package com.mmiholdings.member.money.service.util;

import lombok.extern.java.Log;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Log
@Stateless
public class ResourceLoader {

    private static final String COMMS_MESSAGES_PROPERTIES = "comms_messages.properties";
    private final Properties properties = new Properties();

    @PostConstruct
    public void init () {
        try (InputStream is = ResourceLoader.class.getClassLoader().getResourceAsStream(COMMS_MESSAGES_PROPERTIES)) {
            properties.load(is);
        } catch (IOException e) {
            log.warning(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getSmsMessagesProperty(String key) {
        return properties.getProperty(key);
    }
}
