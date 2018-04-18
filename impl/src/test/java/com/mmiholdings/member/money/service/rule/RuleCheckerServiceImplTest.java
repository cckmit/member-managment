package com.mmiholdings.member.money.service.rule;

import com.mmiholdings.member.money.api.rule.ErrorCode;
import com.mmiholdings.member.money.api.rule.NoFootPrintException;
import com.mmiholdings.member.money.api.rule.NotEligibleMultiplyException;
import com.mmiholdings.member.money.api.rule.NotEligibleMultiplyNotActive;
import com.mmiholdings.member.money.service.clients.CommerceRelationServiceClient;
import com.mmiholdings.member.money.service.clients.PolicyClient;
import com.mmiholdings.multiply.service.entity.BusinessKeyType;
import com.mmiholdings.multiply.service.entity.CDIService;
import com.mmiholdings.multiply.service.entity.FootPrint;
import com.mmiholdings.multiply.service.policy.Policy;
import com.mmiholdings.multiply.service.policy.Role;
import com.mmiholdings.multiply.service.policy.entity.Entity;
import com.mmiholdings.service.money.commerce.member.*;
import com.mmiholdings.service.money.commerce.relation.CustomerTestConstants;
import lombok.extern.java.Log;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.logging.*;

import static com.mmiholdings.service.money.commerce.relation.CustomerTestConstants.ID_NUMBER;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Log
public class RuleCheckerServiceImplTest {

    @Mock
    private CDIService cdiServiceFacade;

    @Mock
    private PolicyClient policyService;

    @Mock
    private CommerceRelationServiceClient relationServiceClient;

    @InjectMocks
    private RuleCheckerServiceImpl ruleChecker;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %3$s [%4$-5s] %5$s%6$s%n");
        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.FINE);
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.ALL);
                handler.setFormatter(new SimpleFormatter());
            }
        }
    }

    private String memberReference = "26776728";
    private String passportNumber = "A12939829";

    @Test
    public void checkMemberIsAnAdult_shouldThrowException_ifMemberIsLessThanEighteenYearsOld() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -15);
        Date fifteenYearsAgo = calendar.getTime();

        expectedEx.expectMessage("Must be older than 18 or older");
        expectedEx.expect(NotEligibleMultiplyException.class);
        expectedEx.expect(new NotEligibleExceptionMatcher(ErrorCode.UNDERAGE_CUSTOMER));

        ruleChecker.checkMemberIsAnAdult(fifteenYearsAgo);
    }

    @Test
    public void checkMemberIsAnAdult_shouldNotThrowException_ifMemberIsEighteenYearsOld() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -18);
        Date EighteenYearsAgo = calendar.getTime();

        ruleChecker.checkMemberIsAnAdult(EighteenYearsAgo);
    }

    @Test
    public void checkMemberIsAnAdult_shouldNotThrowException_ifMemberIsOlderThanEighteenYears() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -19);
        Date NineteenYearsAgo = calendar.getTime();

        ruleChecker.checkMemberIsAnAdult(NineteenYearsAgo);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldPassAllEligibilityChecks_whenCustomerHasAccountOtherThanMMTA() throws Exception {
        // Given
        IdentificationDocument customerIdDoc = IdentificationDocument.createSouthAfricanId("8102045007082");
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setAge(29);
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);
        //Has HSA
        Customer customer = new Customer();
        customer.setIdentificationDocument(customerIdDoc);
        customer.setDateOfBirth(getDate(1981, 2, 4));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.getCustomerAccounts().add(customerAccount);
        when(relationServiceClient.findMemberUsingIdNumber(customerIdDoc.getIdNumber(), customerIdDoc.getType()))
                .thenReturn(customer);

        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldPassAllEligibilityChecks_whenPassportHolderHasAccountOtherThanMMTA() throws Exception {
        // Given
        IdentificationDocument customerIdDoc = IdentificationDocument.createPassport(passportNumber, Country.GB,
                new Date(), new Date());
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        footPrint.putKey(BusinessKeyType.MONEY_CDI_KEY, memberReference);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setAge(29);
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);
        //Has HSA
        Customer customer = new Customer();
        customer.setIdentificationDocument(customerIdDoc);
        customer.setDateOfBirth(getDate(1964, 3, 21));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.getCustomerAccounts().add(customerAccount);
        when(relationServiceClient.findMemberUsingMemberReference(memberReference)).thenReturn(customer);

        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldPassAllEligibilityChecks_whenPassportHolderHasNoMemberInFootprint() throws Exception {
        // Given
        IdentificationDocument customerIdDoc = IdentificationDocument.createPassport(passportNumber, Country.GB,
                new Date(), new Date());
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setAge(29);
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);
        //Has HSA
        Customer customer = new Customer();
        customer.setIdentificationDocument(customerIdDoc);
        customer.setDateOfBirth(getDate(1964, 3, 21));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.getCustomerAccounts().add(customerAccount);

        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }


    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldNotPassAllEligibilityChecks_whenPassportHolderHasNoFootprint()
            throws Exception {
        // Given
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setAge(29);
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        //Has HSA
        Customer customer = new Customer();
        IdentificationDocument customerIdDoc = IdentificationDocument.createPassport(passportNumber, Country.GB,
                new Date(), new Date());
        customer.setIdentificationDocument(customerIdDoc);
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.getCustomerAccounts().add(customerAccount);

        expectedEx.expect(NoFootPrintException.class);

        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(null, customer);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldPassAllEligibilityChecks_whenPassportHolderIsNotCurrentMember() throws Exception {
        // Given
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        footPrint.putKey(BusinessKeyType.MONEY_CDI_KEY, memberReference);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);

        //Has HSA
        Customer customer = CustomerTestConstants.createCustomer();
        IdentificationDocument customerIdDoc = IdentificationDocument.createPassport(passportNumber, Country.GB,
                new Date(), new Date());
        customer.setIdentificationDocument(customerIdDoc);
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.getCustomerAccounts().add(customerAccount);
        when(relationServiceClient.findMemberUsingMemberReference(memberReference)).thenReturn(customer);

        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldPassAllEligibilityChecks_whenPolicyIsNotActive() throws Exception {
        // Given
        IdentificationDocument customerIdDoc = IdentificationDocument.createSouthAfricanId("8102045007082");
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setAge(29);
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("25SUSPEND");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);
        //Has HSA
        Customer customer = new Customer();
        customer.setIdentificationDocument(customerIdDoc);
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.getCustomerAccounts().add(customerAccount);

        expectedEx.expect(new NotEligibleExceptionMatcher(ErrorCode.INACTIVE_POLICY));
        expectedEx.expect(NotEligibleMultiplyNotActive.class);
        expectedEx.expectMessage("Multiply policy not active");
        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldPassAllEligibilityChecks_whenCustomerHasNoAccount() throws Exception {
        // Given
        IdentificationDocument customerIdDoc = IdentificationDocument.createSouthAfricanId("8102045007082");
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setAge(29);
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);
        //NO Account
        when(relationServiceClient.findMemberUsingIdNumber(customerIdDoc.getIdNumber(), customerIdDoc.getType()))
                .thenThrow(CustomerNotFound.class);
        Customer customer = new Customer();
        customer.setIdentificationDocument(customerIdDoc);
        customer.setDateOfBirth(getDate(1981, 2, 4));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        customer.addAccount(customerAccount);
        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldFailEligibility_whenCustomerAlreadyHasAccount() throws Exception {
        // Given
        String idNumber = ID_NUMBER;
        IdentificationDocument customerIdDoc = IdentificationDocument.createSouthAfricanId(idNumber);
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);

        //Has Multiply Money Account (Both Savings and Transactional Wallets)
        Customer customer = CustomerTestConstants.createCustomer();
        customer.setIdentificationDocument(customerIdDoc);

        CustomerAccount transactionalWallet = new CustomerAccount();
        transactionalWallet.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);

        CustomerAccount savingsWallet = new CustomerAccount();
        savingsWallet.setIssuingProductCode(ProductCode.MULTIPLY_SAVINGS);

        customer.addAccount(transactionalWallet);
        customer.addAccount(savingsWallet);

        when(relationServiceClient.findMemberUsingIdNumber(customerIdDoc.getIdNumber(), customerIdDoc.getType()))
                .thenReturn(customer);


        expectedEx.expect(new NotEligibleExceptionMatcher(ErrorCode.EXISTING_ACCOUNT));
        expectedEx.expect(NotEligibleMultiplyException.class);
        expectedEx.expectMessage("Member already has a Multiply Money Account");
        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }

    @Test
    public void checkEligibleForLightweightMultiplyMoneyAccount_shouldFailEligibility_whenCustomerAlreadyHasAccount() throws Exception {
        // Given
        String idNumber = ID_NUMBER;
        IdentificationDocument customerIdDoc = IdentificationDocument.createSouthAfricanId(idNumber);
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setRole(new Role(Role.POLICY_HOLDER));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);

        //Has Multiply Money Account (Both Savings and Transactional Wallets)
        Customer customer = CustomerTestConstants.createCustomer();
        customer.setIdentificationDocument(customerIdDoc);


        when(relationServiceClient.findMemberUsingIdNumber(customerIdDoc.getIdNumber(), customerIdDoc.getType()))
                .thenReturn(customer);

        expectedEx.expect(NotEligibleMultiplyException.class);
        expectedEx.expectMessage("Member already has a Multiply Money Member Profile");
        // When
        ruleChecker.checkEligibleForLightWeightMultiplyMoneyAccount(footPrint, customer);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldFailEligibility_whenCustomerHasNoPolicy() throws Exception {
        // Given
        IdentificationDocument customerIdDoc = IdentificationDocument.createSouthAfricanId("8102045007082");
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        //NO Policy
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(null);
        //Has HSA
        Customer customer = new Customer();
        customer.setIdentificationDocument(customerIdDoc);
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.addAccount(customerAccount);


        expectedEx.expect(new NotEligibleExceptionMatcher(ErrorCode.UNKNOWN_MEMBER));
        expectedEx.expect(NotEligibleMultiplyException.class);
        expectedEx.expectMessage("Client does not have a role on multiply policy");
        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }

    @Test
    public void checkEligibleForMultiplyMoneyAccount_shouldFailEligibility_whenApplicantIsYoungerThan18() throws Exception {
        // Given
        IdentificationDocument customerIdDoc = IdentificationDocument.createSouthAfricanId("8102045007082");
        //Has Footprint
        FootPrint footPrint = new FootPrint();
        String cmsClientNumber = "2017123456789";
        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        //Has Policy
        Policy policy = new Policy();
        Entity member = new Entity();
        member.setAge(17);
        member.setRole(new Role(Role.POLICY_HOLDER));
        member.setClientNumber(Long.valueOf(cmsClientNumber));
        policy.setMembers(Collections.singletonList(member));
        policy.setStatus("10INFPPAY");
        when(policyService.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);
        //Has HSA
        Customer customer = new Customer();
        customer.setDateOfBirth(getDate(2017, 2, 1));
        customer.setIdentificationDocument(customerIdDoc);
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.addAccount(customerAccount);


        expectedEx.expect(new NotEligibleExceptionMatcher(ErrorCode.UNDERAGE_CUSTOMER));
        expectedEx.expect(NotEligibleMultiplyException.class);
        expectedEx.expectMessage("Must be older than 18 or older");
        // When
        ruleChecker.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }

    private Date getDate(int year, int month, int date) {
        Calendar dateOfBirth = Calendar.getInstance();
        dateOfBirth.set(year, month, date);
        return dateOfBirth.getTime();
    }
}