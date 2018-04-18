package com.mmiholdings.member.money.service.rule;

import com.google.common.base.Optional;
import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.member.money.api.rule.ErrorCode;
import com.mmiholdings.member.money.api.rule.NoFootPrintException;
import com.mmiholdings.member.money.api.rule.NotEligibleMultiplyException;
import com.mmiholdings.member.money.api.rule.NotEligibleMultiplyNotActive;
import com.mmiholdings.member.money.service.clients.CommerceRelationServiceClient;
import com.mmiholdings.member.money.service.clients.PolicyClient;
import com.mmiholdings.member.money.service.clients.PropertiesKeys;
import com.mmiholdings.multiply.service.entity.BusinessKeyType;
import com.mmiholdings.multiply.service.entity.FootPrint;
import com.mmiholdings.multiply.service.policy.Policy;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.CustomerNotFound;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import com.mmiholdings.service.money.commerce.member.ProductCode;
import com.mmiholdings.service.money.util.LoggingInterceptor;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import java.util.Calendar;
import java.util.Date;

@Log
@Setter
@Stateless
@Interceptors(LoggingInterceptor.class)
public class RuleCheckerServiceImpl implements RuleCheckerService {

    private PolicyClient policyClient;

    private CommerceRelationServiceClient relationServiceClient;

    @PostConstruct
    private void init() {
        String url = System.getProperty(PropertiesKeys.KEY_POLICY_URL);
        policyClient = new PolicyClient(url);
        url = System.getProperty(PropertiesKeys.KEY_COMMERCE_URL);
        relationServiceClient = new CommerceRelationServiceClient(url);
    }

    @Override
    public void checkEligibleForMultiplyMoneyAccount(FootPrint footPrint,
                                                     Customer customer) throws MemberManagementException {
        checkEligibleMultiplyMoneyAccount(footPrint, customer);
        checkDoesNotHaveExistingAccount(footPrint, customer.getIdentificationDocument());

    }

    private void checkEligibleMultiplyMoneyAccount(FootPrint footPrint,
                                                   Customer customer) throws MemberManagementException {
        if (footPrint == null) throw new NoFootPrintException("No footprint");

        String cmsClientNumber = getCmsClientNumber(footPrint);

        Policy multiplyPolicy = retrieveMultiplyPolicy(cmsClientNumber);

        checkPolicyIsActive(multiplyPolicy);

        checkMemberIsAnAdult(customer.getDateOfBirth());
    }

    private String getCmsClientNumber(FootPrint footPrint) {
        String businessKey = footPrint.getBusinessKey(BusinessKeyType.CMS_CDI_KEY);
        if(StringUtils.isEmpty(businessKey)){
            throw new NoFootPrintException("No CMS client number in footprint");
        }
        return businessKey;
    }


    @Override
    public void checkEligibleForLightWeightMultiplyMoneyAccount(FootPrint footPrint,
                                                                Customer customer) throws MemberManagementException {
        checkEligibleMultiplyMoneyAccount(footPrint, customer);
        checkIfExistingCustomer(footPrint, customer.getIdentificationDocument());

    }


    protected void checkMemberIsAnAdult(Date dateOfBirth) throws NotEligibleMultiplyException {
        Calendar dateOfBirthCal = Calendar.getInstance();
        dateOfBirthCal.setTime(dateOfBirth);

        Calendar today = Calendar.getInstance();
        dateOfBirthCal.add(Calendar.YEAR, 18);

        if (dateOfBirthCal.getTime().getTime() > today.getTime().getTime()) {
            throw new NotEligibleMultiplyException("Must be older than 18 or older", ErrorCode.UNDERAGE_CUSTOMER);
        }
    }

    private Policy retrieveMultiplyPolicy(String cmsClientNumber) throws NotEligibleMultiplyException {
        Policy multiplyPolicy = policyClient.getPolicy(Long.valueOf(cmsClientNumber));
        if (multiplyPolicy == null) {
            throw new NotEligibleMultiplyException("Client does not have a role on multiply policy",ErrorCode.UNKNOWN_MEMBER);
        }
        return multiplyPolicy;
    }

    private void checkDoesNotHaveExistingAccount(FootPrint footprint, IdentificationDocument identificationDocument) throws MemberManagementException {
        Optional<Customer> customer = findCustomer(footprint, identificationDocument);

        if (customer.isPresent() && customer.get().getCustomerAccount(ProductCode.MULTIPLY_TRANSACTIONAL).isPresent()
                && customer.get().getCustomerAccount(ProductCode.MULTIPLY_SAVINGS).isPresent()) {
            throw new NotEligibleMultiplyException("Member already has a Multiply Money Account",ErrorCode.EXISTING_ACCOUNT);
        }
    }


    private void checkIfExistingCustomer(FootPrint footprint, IdentificationDocument identificationDocument) throws MemberManagementException {
        Optional<Customer> customer = findCustomer(footprint, identificationDocument);

        if (customer.isPresent()){
            throw new NotEligibleMultiplyException("Member already has a Multiply Money Member Profile");
        }
    }

    private Optional<Customer> findCustomer(FootPrint footprint, IdentificationDocument identificationDocument) throws NoFootPrintException {
        if (identificationDocument.isSouthAfrican()) {
            try {
                return Optional.of(relationServiceClient.findMemberUsingIdNumber(identificationDocument.getIdNumber(),
                        identificationDocument.getType()));
            } catch (CustomerNotFound customerNotFound) {
                return Optional.absent();
            }
        } else {

            if (footprint == null) {
                throw new NoFootPrintException("No footprint for [" + footprint + " " + identificationDocument + "]");
            }

            String memberReference = footprint.getBusinessKey(BusinessKeyType.MONEY_CDI_KEY);

            if (memberReference == null) {
                return Optional.absent();
            }

            try {
                return Optional.of(relationServiceClient.findMemberUsingMemberReference(memberReference));
            } catch (CustomerNotFound customerNotFound) {
                return Optional.absent();
            }
        }
    }

    private void checkPolicyIsActive(Policy policy) throws NotEligibleMultiplyNotActive {
        if (!policy.isActive())
            throw new NotEligibleMultiplyNotActive("Multiply policy not active");
    }
}
