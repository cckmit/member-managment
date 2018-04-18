package com.mmiholdings.member.money.service;

import com.google.common.base.Optional;
import com.mmiholdings.client.fica.FicaClient;
import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.member.money.service.clients.CommerceRelationServiceClient;
import com.mmiholdings.member.money.service.clients.PolicyClient;
import com.mmiholdings.member.money.service.clients.dto.MemberUpdateRequest;
import com.mmiholdings.multiply.service.policy.Policy;
import com.mmiholdings.service.fica.dto.server.responses.ClientFicaStatus;
import com.mmiholdings.service.fica.exceptions.FicaException;
import com.mmiholdings.service.money.commerce.member.*;
import lombok.Data;
import lombok.extern.java.Log;

import javax.enterprise.context.Dependent;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;

import static org.codehaus.plexus.util.ExceptionUtils.getRootCause;

@Data
@Log
@Dependent
public class FicaFacade {

    private FicaClient ficaClient;

    private PolicyClient policyClient;

    private CommerceRelationServiceClient commerceClient;

    public void validateFica(Customer customer, String cmsClientNumber, String agentId) {
        log.info(String.format("Customer[%s] fica status: %s", customer.getCustomerReference(), customer.getFicaStatus()));

        if (shouldPerformFicaValidation(customer)) {
            ClientFicaStatus ficaStatus =
                    ficaClient.verifyMobileAndPerson(
                            customer.getIdentificationDocument().getIdNumber(),
                            customer.getName(),
                            customer.getSurname(),
                            removeCountryCode(customer.getMobilePhoneNumber()));

            log.info(String.format("The fica response is %s", ficaStatus));

            updateFicaStatus(customer, agentId, ficaStatus);

            if (ficaStatus.containsFailure()) {
                startBPMFicaProcess(customer, cmsClientNumber, ficaStatus);
            }
        }
    }

    private String removeCountryCode(String mobileNumber) {
        if (mobileNumber != null || mobileNumber.startsWith("+27")) {
            return mobileNumber.replaceAll("\\+27", "0");
        }

        return mobileNumber;
    }

    private void updateFicaStatus(Customer customer, String agentId, ClientFicaStatus ficaStatus) {

        if (ficaStatus.containsSuccess()) {
            customer = getCustomer(customer.getIdentificationDocument());

            if (ficaStatus.getPersonVerified().passedVerification()) {
                if (customer.getFicaStatus()!=FicaStatus.FULL_FICA) {
                    customer.setFicaStatus(FicaStatus.ID_NUMBER_VERIFIED);
                    customer.setFicaStatusChangeDateTime(new Date());
                }
            }

            if (ficaStatus.getCellphoneVerified().passedVerification()) {
                customer.setMobileStatus(MobileStatus.PASSED_VERIFICATION);
            }

            updateMoneyManagementAccountStatus(customer);

            Optional<Customer> result = createOrUpdateMultiplyMember(customer, agentId);
            if (!result.isPresent()) {
                throw new FicaException(String.format("Failed to update member: %s", customer));
            }
        }
    }

    private void updateMoneyManagementAccountStatus(Customer customer) {

        if (customer!=null && isMobileNotValidated(customer.getMobileStatus()) && isCustomerValidated(customer.getFicaStatus())) {
            customer.getCustomerAccounts()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(MultiplyMoneyAccount.class::isInstance)
                    .map(superAccount->(MultiplyMoneyAccount) superAccount)
                    .forEach(multiplyAccount->multiplyAccount.setStatus( CustomerAccountStatus.ACTIVE));

        }
    }

    private boolean shouldPerformFicaValidation(Customer customer) {
        MobileStatus customerMobileStatus = customer.getMobileStatus();

        boolean flagEnabled = FeatureFlags.FICA_VERIFICATION;

        boolean ficaNotValidated =  !isCustomerValidated(customer.getFicaStatus());

        boolean mobileNotValidated = !isMobileNotValidated(customerMobileStatus);

        return flagEnabled && (ficaNotValidated || mobileNotValidated);
    }

    private boolean isMobileNotValidated(MobileStatus customerMobileStatus) {
        return customerMobileStatus==MobileStatus.PASSED_VERIFICATION;
    }

    private boolean isCustomerValidated(FicaStatus customerFicaStatus) {
        return customerFicaStatus==FicaStatus.FULL_FICA ||
                customerFicaStatus==FicaStatus.ID_NUMBER_VERIFIED ||
                customerFicaStatus==FicaStatus.ID_RESIDENCE_VERIFIED ||
                customerFicaStatus==FicaStatus.ID_DOC_VERIFIED;
    }

    private void startBPMFicaProcess(Customer customer, String cmsClientNumber, ClientFicaStatus ficaStatus) {
        Policy policy = this.policyClient.getPolicy(Long.valueOf(cmsClientNumber));

        if (!ficaClient.startProcessToRetrieveFicaDetailsFromClient(
                String.format("%s%d",
                        policy.getPolicyNumber().getProductCode(), policy.getPolicyNumber().getNumber()),
                customer.getIdentificationDocument().getIdNumber(),
                ficaStatus)) {
            throw new RuntimeException("Failed to kick start the BPM eFica process.");
        }
    }

    private Optional<Customer> createOrUpdateMultiplyMember(Customer customer, String agentId) throws MemberManagementException {
        try {
            log.log(Level.INFO, "Creating new account in Traderoot for [{0}]... by {1}", new Object[]{customer, agentId});
            MemberUpdateRequest memberUpdateRequest = new MemberUpdateRequest();
            memberUpdateRequest.setCustomer(customer);
            memberUpdateRequest.setUserId(agentId);
            String memberReference = commerceClient.createOrUpdateMember(memberUpdateRequest);

            Customer customerReturned = commerceClient.findMemberUsingMemberReference(memberReference);
            log.log(Level.INFO, "Created new account with reference [{0}]", customerReturned.getCustomerReference());
            return Optional.of(customerReturned);
        } catch (CommerceException e) {
            Throwable rootCause = getRootCause(e);
            String errorMessage = e.getMessage();
            log.log(Level.WARNING, "CommerceException: Creating new account failed: {0}", e.getMessage());
            throw new MemberManagementException(errorMessage);
        } catch (BusinessException e) {
            log.log(Level.WARNING, "BusinessException: Creating new account failed: {0}", e.getMessage());
            throw new MemberManagementException(e.getMessage());
        }
    }

    private Customer getCustomer(IdentificationDocument identificationDocument) {
        Customer customerToUpdate = null;
        try {
            customerToUpdate = commerceClient.findMemberUsingIdNumber(identificationDocument.getIdNumber(),
                    identificationDocument.getType());
        } catch (CustomerNotFound customerNotFound) {
            log.warning("Customer not found to update FICA status for "+identificationDocument);
        }
        return customerToUpdate;
    }
}
