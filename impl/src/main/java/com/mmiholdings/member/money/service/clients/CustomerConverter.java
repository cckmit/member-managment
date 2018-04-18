package com.mmiholdings.member.money.service.clients;

import com.mmiholdings.service.money.commerce.member.CardHolder;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.CustomerAccount;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import com.mmiholdings.service.money.commerce.member.MultiplyCustomer;
import com.mmiholdings.service.money.commerce.member.MultiplyMoneyAccount;
import com.mmiholdings.service.money.commerce.member.ProductCode;
import com.mmiholdings.service.money.util.NullableCollection;
import lombok.extern.java.Log;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Log
public class CustomerConverter {

    public static Customer convertSubtypes(Customer customer) {
        Customer multiplyCustomer = cloneCustomer(customer);

        multiplyCustomer.setIdentificationDocument(createIdDocument(customer.getIdentificationDocument()));

        List<CustomerAccount> customerAccounts = multiplyCustomer.getCustomerAccounts();
        for (int index = 0; index < customerAccounts.size(); index++) {
            CustomerAccount customerAccount = customerAccounts.get(index);
            customerAccount = cloneAccount(customerAccount);
            customerAccounts.remove(index);
            customerAccounts.add(index, customerAccount);

            for (CardHolder cardHolder : NullableCollection.nullToEmpty(customerAccount.getCardholders())) {
                cardHolder.setIdentificationDocument(createIdDocument(cardHolder.getIdentificationDocument()));
            }
        }
        return multiplyCustomer;
    }

    private static Customer cloneCustomer(Customer customer) {
        Customer customerConverted = null;
        if(customer != null){
            customerConverted = new MultiplyCustomer();
            try {
                BeanUtils.copyProperties(customerConverted, customer);
                return customerConverted;
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.severe("Unable to convert customer to correct type");
                throw new AccountConvertException("Unable to convert customer to correct type : "+customer);
            }
        }
        return customerConverted;
    }



    private static CustomerAccount cloneAccount(CustomerAccount customerAccount) {
        CustomerAccount correctTypeAccount = null;
        if(customerAccount != null && !customerAccount.getIssuingProductCode().equals(ProductCode.HEALTH)){
            correctTypeAccount = new MultiplyMoneyAccount();
            try {
                BeanUtils.copyProperties(correctTypeAccount, customerAccount);
                return correctTypeAccount;
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.severe("Unable to convert account to correct type");
                throw new AccountConvertException("Unable to convert account to correct type : "+customerAccount);
            }
        }
        return customerAccount;
    }


    private static IdentificationDocument createIdDocument(IdentificationDocument identificationDocument) {
        IdentificationDocument idDoc;
        if (identificationDocument.isNationalId()) {
            idDoc = IdentificationDocument.createSouthAfricanId(identificationDocument.getIdNumber());
        } else {
            return identificationDocument;
        }

        return idDoc;
    }

    private static class AccountConvertException extends RuntimeException {
        public AccountConvertException(String message) {
            super(message);
        }
    }

}
