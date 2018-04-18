package com.mmiholdings.member.money.service.rule;

import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.multiply.service.entity.FootPrint;
import com.mmiholdings.service.money.commerce.member.Customer;

import javax.ejb.Local;

/**
 * Rule checker to enforce eligibility checks.
 */
@Local
public interface RuleCheckerService {

    /**
     * Check for eligibility for Multiply VISA account.  Only supports ID number now.
     * @param footPrint  ID number of applicant
     * @param customer
     * @throws MemberManagementException
     */
    void checkEligibleForMultiplyMoneyAccount(FootPrint footPrint, Customer customer) throws MemberManagementException;

    void checkEligibleForLightWeightMultiplyMoneyAccount(FootPrint footPrint,
                                                         Customer customer) throws MemberManagementException;
}
