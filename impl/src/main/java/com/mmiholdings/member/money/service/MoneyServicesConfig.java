package com.mmiholdings.member.money.service;

import com.mmiholdings.member.money.service.clients.*;
import com.mmiholdings.multiply.service.entity.client.CDIEntityClient;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;

@Singleton
@Getter
@ToString
@Log
public class MoneyServicesConfig {
    private String commerceUrl = null;
    private String ficaUrl= null;
    private String entityUrl = null;
    private String policyUrl = null;
    private String communicationsUrl=null;
    private String bpmAccountCreationUrl = null;

    @PostConstruct
    public void init() {
        policyUrl = System.getProperty(PropertiesKeys.KEY_POLICY_URL);
        commerceUrl = System.getProperty(PropertiesKeys.KEY_COMMERCE_URL);
        communicationsUrl = System.getProperty(PropertiesKeys.KEY_COMMUNICATIONS_URL);
        entityUrl = System.getProperty(PropertiesKeys.KEY_ENTITY_URL);
        ficaUrl = System.getProperty(PropertiesKeys.KEY_FICA_URL);
        bpmAccountCreationUrl = System.getProperty(PropertiesKeys.KEY_BPM_ACCOUNT_URL);
    }
}
