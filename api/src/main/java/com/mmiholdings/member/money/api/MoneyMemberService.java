package com.mmiholdings.member.money.api;

import com.google.common.base.Optional;
import com.mmiholdings.member.money.api.domain.ApplicationResult;
import com.mmiholdings.member.money.api.dto.CashbackDeposit;
import com.mmiholdings.member.money.api.rule.CardLinkingException;
import com.mmiholdings.multiply.service.entity.FootPrint;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.CustomerNotFound;
import com.mmiholdings.service.money.commerce.payment.CashDepositException;

public interface MoneyMemberService {

    /**
     * Check eligibility for Multiply Money account
     * @param footPrint  Footprint of customer
     * @param customer Needs only identification and date of birth
     */
    void checkEligibility(FootPrint footPrint, Customer customer);

    /**
     * Apply for money management account
     *
     *
     * @param cmsClientNumber
     * @param customer Customer containing account and card holders
     * @param acceptedTerms
     * @param agentId The userId of the customer api agent that captured the application
     * @return Newly created Money Member reference number
     * @throws MemberManagementException
     */
    ApplicationResult applyForNewAccount(String cmsClientNumber, Customer customer, boolean acceptedTerms, String agentId)
            throws MemberManagementException, CustomerNotFound;

    ApplicationResult applyForLightWeightAccount(String cmsClientNumber, Customer customer, boolean acceptedTerms, String userId)
            throws MemberManagementException, CustomerNotFound;

    /**
     *
     * @param cashbackDeposit
     * @throws MemberManagementException
     * @throws CashDepositException
     */
    String depositCashback(CashbackDeposit cashbackDeposit) throws MemberManagementException, CashDepositException, CustomerNotFound;


    /**
     *
     * @param customer
     * @param cmsClientNumber
     * @param agentId
     * @return
     */
    void validateFica(Customer customer, String cmsClientNumber,String agentId);

    /**
     * Auto apply for member and account
     * @param cmsClientNumber CMS client
     * @param customer Customer
     * @param termsAndConditions  Accept terms
     * @param agentId User performing task
     * @return
     */
    ApplicationResult autoApply(String cmsClientNumber, Customer customer, boolean termsAndConditions, String agentId) throws CustomerNotFound;


    /**
     * Apply for Visa Card
     * @param cmsClientNumber
     * @param customer
     * @param termsAndConditions
     * @param agentId
     * @return
     */
    Optional<Customer> applyVisaCard(String cmsClientNumber, Customer customer, boolean termsAndConditions, String agentId) throws CustomerNotFound;

    /**
     * Link card to
     *
     * @param linkCardRequest@throws BusinessNotFoundException
     * @throws CardLinkingException
     */
    void linkCard(LinkCardRequest linkCardRequest) throws CardLinkingException;
}
