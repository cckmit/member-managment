package com.mmiholdings.member.money.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mmiholdings.member.money.api.domain.AccountType;
import com.mmiholdings.member.money.api.domain.ApplicationResult;
import com.mmiholdings.member.money.api.domain.AccountCreation;
import com.mmiholdings.member.money.api.LoggingService;
import com.mmiholdings.multiply.service.entity.BusinessKeyType;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.ProductCode;
import com.mmiholdings.service.money.util.LoggingInterceptor;
import lombok.extern.java.Log;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import java.util.Date;
import java.util.logging.Level;

/**
 * Created by pieter on 2017/03/27.
 */
@Interceptors(LoggingInterceptor.class)
@Local(value = LoggingService.class)
@Stateless
@Log
public class LoggingServiceImpl implements LoggingService {

    @EJB
    private MemberApplicationDaoImpl memberApplicationDao;

    private static final int MAX_ERROR_LENGTH = 4095;
    private static final String SUCCESSFULLY_VALIDATED_APPLICATION = "Successfully validated application";
    public static final String DATE_FORMAT = "yyy-MM-dd'T'HH:mm:ss.SSS";


    @Override
    public AccountCreation getLastApplicationByType(AccountType account, String name, String businessId) {
        return memberApplicationDao.getLastApplicationByType(AccountType.MULTIPLY_MONEY);
    }

    @Override
    public void logApplicationAttempt(String cmsClientNumber, Customer customer, ApplicationResult result, String userId) {
        try {
            AccountCreation application = AccountCreation.builder()
                    .cmsClientNumber(cmsClientNumber)
                    .accountType(AccountType.MULTIPLY_MONEY)
                    .idNumber(customer.getIdentificationDocument().getIdNumber())
                    .modifiedBy(userId)
                    .modifiedDate(new Date()).modifiedBy(userId)
                    .requestData(new Gson().toJson(result))
                    .build();

            Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
            String jsonData = gson.toJson(customer);

            if ("SUCCESS".equals(result.getResultCode())) {
                result.setMessage(SUCCESSFULLY_VALIDATED_APPLICATION);
            }

            log.log(Level.INFO, "Logging attempt: [{0}]", application);
            memberApplicationDao.saveApplication(application);
        } catch (RuntimeException re) {
            log.log(Level.SEVERE, "Failed to log application attempt", re);
        }
    }

    private String truncateMessage(String message) {
        int length = message.length();
        if (length > MAX_ERROR_LENGTH)
            length = MAX_ERROR_LENGTH;
        return message.substring(0, length);
    }
}
