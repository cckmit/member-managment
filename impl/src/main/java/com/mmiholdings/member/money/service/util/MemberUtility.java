package com.mmiholdings.member.money.service.util;

import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.multiply.service.policy.Policy;
import com.mmiholdings.multiply.service.policy.entity.ContactDetails;
import com.mmiholdings.multiply.service.policy.entity.Entity;
import com.mmiholdings.service.money.commerce.member.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import com.mmiholdings.member.money.service.clients.PolicyClient;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.util.logging.Level;

@Dependent
@Data
@NoArgsConstructor
@AllArgsConstructor
@Log
public class MemberUtility {
    public static final String MALE = "m";

    private PolicyClient policyClient;

    public MultiplyCustomer getCustomer(String cmsClientNumber) throws MemberManagementException {
        Policy policy = policyClient.getPolicy(Long.valueOf(cmsClientNumber));
        if(policy == null){
            throw new MemberManagementException("No policy found");
        }
        Entity policyHolder = policy.getPolicyHolder();

        if (policyHolder==null) {
            throw new MemberManagementException("No policy holder information was found. Cannot create customer.");
        }

        MultiplyCustomer customer = null;

        try {
            customer = toCustomer(policyHolder);
        } catch (IdentificationDocument.InvalidRsaIdCheckDigit invalidRsaIdCheckDigit) {
            log.log(Level.SEVERE,"Invalid id",invalidRsaIdCheckDigit);
            throw new MemberManagementException(invalidRsaIdCheckDigit.getMessage());
        }
        return customer;
    }

    private MultiplyCustomer toCustomer(Entity policyHolder) throws IdentificationDocument.InvalidRsaIdCheckDigit {
        MultiplyCustomer customer = new MultiplyCustomer();
        String names = "";
        if (policyHolder.getNames() != null) {
            for (String name : policyHolder.getNames()) {
                names += name + " ";
            }
        }
        customer.setName(names.trim());
        customer.setSurname(policyHolder.getSurname());
        customer.setInitials(policyHolder.getInitials());
        customer.setTitle(policyHolder.getTitle());
        if (policyHolder.getDateOfBirth() != null) {
            customer.setDateOfBirth(policyHolder.getDateOfBirth().getTime());
        }
        if (policyHolder.getGender() != null) {
            String gender = policyHolder.getGender().toString().toLowerCase();
            customer.setGender(gender.startsWith(MALE) ? Gender.MALE : Gender.FEMALE);
        }


        String idNumber = policyHolder.getIdNumber();
        String passportNumber = policyHolder.getPassportNumber();

        if (idNumber != null && idNumber.trim().length() > 0) {

            if (!SouthAfricanIdNumberValidator.validateCheckDigit(idNumber) || Long.valueOf(idNumber) == 0) {
                if (passportNumber != null && passportNumber.trim().length() > 0) {
                    customer.setIdentificationDocument(Passport.createPassport(passportNumber,
                            null, null, null));
                } else {
                    throw new IdentificationDocument.InvalidRsaIdCheckDigit("Invalid South African Identification number");
                }
            } else {
                customer.setIdentificationDocument(SouthAfricanIdDocument.createSouthAfricanId(idNumber));
            }
        } else {
            customer.setIdentificationDocument(Passport.createPassport(passportNumber,
                    null, null, null));
        }


        ContactDetails contactDetails = policyHolder.getContactDetails();
        if (contactDetails != null) {
            customer.setEmail(contactDetails.getPersonalEmail());

            if (StringUtils.isBlank(customer.getEmail())) {
                customer.setEmail(contactDetails.getWorkEmail());
            }

            customer.setMobilePhoneNumber(SouthAfricanMobileFormatter.addSouthAfricanCodeToPhoneNumber(contactDetails.getCellphone()));
            customer.setHomePhoneNumber(contactDetails.getHomePhone());
            customer.setWorkPhoneNumber(contactDetails.getWorkPhone());
            //Physical address
            com.mmiholdings.multiply.service.policy.entity.Address residentialAddress = contactDetails.getResidentialAddress();
            Address physicalAddress = new Address();
            if (residentialAddress != null) {
                String line1 = residentialAddress.getLine(1);
                String line2 = residentialAddress.getLines().size() > 1 ? residentialAddress.getLine(2) : null;
                String suburb = residentialAddress.getLine(residentialAddress.getLines().size() - 1);
                String postalCode = residentialAddress.getCode();
                physicalAddress.setLine1(StringUtils.isNotBlank(line1)?line1:"DEFAULT999");
                physicalAddress.setLine2(StringUtils.isNotBlank(line2)?line2:"DEFAULT999");
                physicalAddress.setSuburb(StringUtils.isNotBlank(suburb)?suburb:"DEFAULT999");
                physicalAddress.setPostalCode(StringUtils.isNotBlank(postalCode)?postalCode:"9999");
                customer.setPhysicalAddress(physicalAddress);
            }

            //Postal address
            com.mmiholdings.multiply.service.policy.entity.Address policyPostalAddress = contactDetails.getPostalAddress();
            Address postalAddress = new Address();
            if (policyPostalAddress != null) {
                postalAddress.setLine1(policyPostalAddress.getLine(1));
                postalAddress.setLine2(policyPostalAddress.getLines().size() > 1 ? policyPostalAddress.getLine(2) : null);
                postalAddress.setSuburb(policyPostalAddress.getLine(policyPostalAddress.getLines().size() - 1));
                postalAddress.setPostalCode(policyPostalAddress.getCode());
                customer.setPostalAddress(postalAddress);
            }
        }

        return customer;
    }
}
