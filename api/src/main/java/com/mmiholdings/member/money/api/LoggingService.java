package com.mmiholdings.member.money.api;

import com.mmiholdings.member.money.api.domain.ApplicationResult;
import com.mmiholdings.member.money.api.domain.AccountCreation;
import com.mmiholdings.member.money.api.domain.AccountType;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.ProductCode;

/**
 * Created by pieter on 2017/03/27.
 */
public interface LoggingService {

    AccountCreation getLastApplicationByType(AccountType account, String name, String businessId);

	void logApplicationAttempt(String cmsClientNumber, Customer customer, ApplicationResult result, String userId);
}
