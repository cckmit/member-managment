package com.mmiholdings.member.money.service.dto;

import com.mmiholdings.member.money.api.util.SouthAfricanMobileFormatter;
import com.mmiholdings.service.money.commerce.member.Address;
import com.mmiholdings.service.money.commerce.member.FicaStatus;
import com.mmiholdings.service.money.commerce.member.MobileStatus;
import com.mmiholdings.service.money.commerce.member.MultiplyCustomer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.java.Log;
import org.apache.commons.beanutils.BeanUtils;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

/**
 * Created by prince on 7/10/17.
 */
@Log
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement
public class Customer extends Person implements Serializable {

    private Address physicalAddress;
    private Address postalAddress;
    private String accountReference;
    private String customerReference;
    private String incomeTaxNumber;
    private FicaStatus ficaStatus;
    private Date ficaStatusChangeDateTime;
    private MobileStatus mobileStatus;
    private Boolean termsAndConditions;

    public com.mmiholdings.service.money.commerce.member.Customer toCustomer() {
        MultiplyCustomer customer = new MultiplyCustomer();

        try {
            BeanUtils.copyProperties(customer,this);
        } catch (IllegalAccessException  | InvocationTargetException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        customer.setIdentificationDocument(createIdentification());

        customer.setMobilePhoneNumber(SouthAfricanMobileFormatter
                .addSouthAfricanCodeToPhoneNumber(this.getMobilePhoneNumber()));

        return customer;
    }
}
