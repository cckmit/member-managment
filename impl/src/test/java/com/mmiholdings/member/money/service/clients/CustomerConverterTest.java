package com.mmiholdings.member.money.service.clients;

import com.google.common.collect.Lists;
import com.mmiholdings.service.money.commerce.member.CardHolder;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.CustomerAccount;
import com.mmiholdings.service.money.commerce.member.CustomerAccountStatus;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import com.mmiholdings.service.money.commerce.member.MultiplyCustomer;
import com.mmiholdings.service.money.commerce.member.MultiplyMoneyAccount;
import com.mmiholdings.service.money.commerce.member.ProductCode;
import com.mmiholdings.service.money.commerce.member.SouthAfricanIdDocument;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.client.ClientBuilder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CustomerConverterTest {

    private static final String HEALTH_ACCOUNT_NUMBER = "12345";
    private static final String HEALTH_ACCOUNT_REF = "9018998892";
    public static final String ID_NUMBER = "8201200000091";
    private static final String MULTIPLY_ACCOUNT_NUMBER = "23443";
    private static final String MULTIPLY_ACCOUNT_REF = "10920920920";
    private static final String CARD_REF = "2982989282";

    @Test
    public void convertSubtypes_shouldNotConvert_whenHealthAccount() {
        Customer customer = new Customer();
        IdentificationDocument southAfricanId = SouthAfricanIdDocument.createSouthAfricanId(ID_NUMBER);

        List<CustomerAccount> accounts = new ArrayList<>();
        CustomerAccount healthAccount = createHealthCustomerAccount();
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(CARD_REF);
        cardHolder.setIdentificationDocument(southAfricanId);
        healthAccount.addCardHolder(cardHolder);

        accounts.add(healthAccount);
        customer.setCustomerAccounts(accounts);
        customer.setIdentificationDocument(southAfricanId);

        Customer convertedCustomer = CustomerConverter.convertSubtypes(customer);


        List<CustomerAccount> customerAccounts = convertedCustomer.getCustomerAccounts();
        Assert.assertEquals(1, customerAccounts.size());
        CustomerAccount customerAccount = customerAccounts.get(0);
        Assert.assertTrue(customerAccount instanceof CustomerAccount);
        Assert.assertEquals(customerAccount.getAccountNumber(), HEALTH_ACCOUNT_NUMBER);
        Assert.assertEquals(customerAccount.getAccountReference(), HEALTH_ACCOUNT_REF);
        Assert.assertEquals(customerAccount.getIssuingProductCode(), ProductCode.HEALTH);
        Assert.assertEquals(customerAccount.getStatus(), CustomerAccountStatus.ACTIVE);

        Assert.assertEquals(customerAccount.getCardholders().get(0).getCardReference(), CARD_REF);

    }

    @Test
    public void convertSubtypes_shouldNotConvert_whenNoAccount() {
        Customer customer = new Customer();
        IdentificationDocument southAfricanId = SouthAfricanIdDocument.createSouthAfricanId(ID_NUMBER);

        List<CustomerAccount> accounts = new ArrayList<>();
               customer.setCustomerAccounts(accounts);
        customer.setIdentificationDocument(southAfricanId);

        Customer convertedCustomer = CustomerConverter.convertSubtypes(customer);


        List<CustomerAccount> customerAccounts = convertedCustomer.getCustomerAccounts();
        Assert.assertEquals(0, customerAccounts.size());
    }

    @Test
    public void convertSubtypes_shouldConvertMultiplyAccount_whenHealthAccountAndMultiplyAccount() {
        Customer customer = new Customer();

        List<CustomerAccount> accounts = new ArrayList<>();
        CustomerAccount healthAccount = createHealthCustomerAccount();
        CustomerAccount multiplyCustomerAccount = createMultiplyCustomerAccount();

        accounts.add(healthAccount);
        accounts.add(multiplyCustomerAccount);
        customer.setCustomerAccounts(accounts);
        customer.setIdentificationDocument(SouthAfricanIdDocument.createSouthAfricanId(ID_NUMBER));

        Customer convertedCustomer = CustomerConverter.convertSubtypes(customer);


        List<CustomerAccount> customerAccounts = convertedCustomer.getCustomerAccounts();
        Assert.assertEquals(2, customerAccounts.size());
        CustomerAccount customerAccount = customerAccounts.get(0);
        Assert.assertTrue(customerAccount instanceof CustomerAccount);
        Assert.assertEquals(customerAccount.getAccountNumber(), HEALTH_ACCOUNT_NUMBER);
        Assert.assertEquals(customerAccount.getAccountReference(), HEALTH_ACCOUNT_REF);
        Assert.assertEquals(customerAccount.getIssuingProductCode(), ProductCode.HEALTH);
        Assert.assertEquals(customerAccount.getStatus(), CustomerAccountStatus.ACTIVE);
        CustomerAccount mulCustomerAccount = customerAccounts.get(1);
        Assert.assertTrue(mulCustomerAccount instanceof MultiplyMoneyAccount);
        Assert.assertEquals(mulCustomerAccount.getAccountNumber(), MULTIPLY_ACCOUNT_NUMBER);
        Assert.assertEquals(mulCustomerAccount.getAccountReference(), MULTIPLY_ACCOUNT_REF);
        Assert.assertEquals(mulCustomerAccount.getIssuingProductCode(), ProductCode.MULTIPLY_SAVINGS);
        Assert.assertEquals(mulCustomerAccount.getStatus(), CustomerAccountStatus.PENDING_ACTIVATION);

    }

    private CustomerAccount createHealthCustomerAccount() {
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountNumber(HEALTH_ACCOUNT_NUMBER);
        healthAccount.setAccountReference(HEALTH_ACCOUNT_REF);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthAccount.setStatus(CustomerAccountStatus.ACTIVE);
        return healthAccount;
    }

    private MultiplyMoneyAccount createMultiplyCustomerAccount() {
        MultiplyMoneyAccount multiplyMoneyAccount = new MultiplyMoneyAccount();
        multiplyMoneyAccount.setAccountNumber(MULTIPLY_ACCOUNT_NUMBER);
        multiplyMoneyAccount.setAccountReference(MULTIPLY_ACCOUNT_REF);
        multiplyMoneyAccount.setIssuingProductCode(ProductCode.MULTIPLY_SAVINGS);
        multiplyMoneyAccount.setStatus(CustomerAccountStatus.PENDING_ACTIVATION);
        return multiplyMoneyAccount;
    }
}