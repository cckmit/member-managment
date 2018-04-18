package com.mmiholdings.member.money.service;

import com.mmiholdings.client.fica.FicaClient;
import com.mmiholdings.member.money.api.LinkCardRequest;
import com.mmiholdings.member.money.api.LoggingService;
import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.member.money.api.rule.CardLinkingException;
import com.mmiholdings.member.money.api.rule.NoFootPrintException;
import com.mmiholdings.member.money.service.clients.CommerceRelationServiceClient;
import com.mmiholdings.member.money.service.clients.HealthPolicyNotFoundException;
import com.mmiholdings.member.money.service.clients.HealthServiceClient;
import com.mmiholdings.member.money.service.clients.PolicyClient;
import com.mmiholdings.member.money.service.clients.dto.MemberUpdateRequest;
import com.mmiholdings.member.money.service.clients.PolicyClient;
import com.mmiholdings.member.money.service.rule.RuleCheckerService;
import com.mmiholdings.member.money.service.rule.RuleCheckerServiceImpl;
import com.mmiholdings.member.money.service.util.MemberUtility;
import com.mmiholdings.member.money.service.util.MockObjectCreator;
import com.mmiholdings.multiply.service.entity.BusinessKeyType;
import com.mmiholdings.multiply.service.entity.FootPrint;
import com.mmiholdings.multiply.service.entity.client.CDIEntityClient;
import com.mmiholdings.multiply.service.health.HealthBeneficiary;
import com.mmiholdings.multiply.service.health.HealthPolicy;
import com.mmiholdings.multiply.service.policy.Policy;
import com.mmiholdings.multiply.service.policy.PolicyNumber;
import com.mmiholdings.multiply.service.policy.entity.Entity;
import com.mmiholdings.service.fica.dto.YesNoUnspecified;
import com.mmiholdings.service.fica.dto.server.responses.ClientFicaStatus;
import com.mmiholdings.service.money.commerce.member.*;
import com.mmiholdings.service.money.commerce.relation.CustomerTestConstants;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import java.util.Collections;
import java.util.List;

import static com.mmiholdings.service.money.commerce.member.AccountTypeCode.SAVINGS;
import static com.mmiholdings.service.money.commerce.member.IdentificationDocument.createSouthAfricanId;
import static com.mmiholdings.service.money.commerce.member.ProductCode.MULTIPLY_SAVINGS;
import static com.mmiholdings.service.money.commerce.relation.CustomerTestConstants.CARD_REFERENCE;
import static com.mmiholdings.service.money.commerce.relation.CustomerTestConstants.ID_NUMBER;
import static com.mmiholdings.service.money.commerce.relation.CustomerTestConstants.MOBILE_NUMBER;
import static com.mmiholdings.service.money.commerce.relation.CustomerTestConstants.PHYSICAL_LINE_1;
import static com.mmiholdings.service.money.commerce.relation.CustomerTestConstants.POSTAL_CODE;
import static com.mmiholdings.service.money.commerce.relation.CustomerTestConstants.SUBURB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Log
@RunWith(MockitoJUnitRunner.Silent.class)
public class MoneyMemberManagementServiceImplTest {

    private static final String ID_NUMBER_SPOUSE = "880100150001081";
    public static final String ID_NUMBER = "7706020034083";

    @Mock
    private LoggingService loggingService;

    @Mock
    private CDIEntityClient cdiEntityClient;

    @Mock
    private CommerceRelationServiceClient commerceRelationServiceClient;

    @Mock
    private BusinessProcessManagementAutoAccountFacade bpmFacade;

    @Mock
    private PolicyClient policyClient;

    @Mock
    private FicaClient ficaClient;

    private RuleCheckerService ruleCheckerService;

    @InjectMocks
    private MoneyMemberManagementServiceImpl managementService;

	@Mock
	private HealthServiceClient healthService;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void init() {
        ruleCheckerService = new RuleCheckerServiceImpl();
	    managementService.setRuleCheckerService(ruleCheckerService);
    }

    @Test
    public void linkCard_shouldCallCommerceLinkCardWithHeathAccRef_whenCardHolderIsNotMainOnHealthAndPassesPrerequisite() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        //Cardholder under Multiply Money
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        transactionalAccount.setCardholders(Collections.singletonList(cardHolder)); //Card holder who IS a dependent on health policy
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Cardholder must be on health policy
        HealthPolicy healthPolicy = new HealthPolicy();
        HealthBeneficiary healthBeneficiary = new HealthBeneficiary();
        healthBeneficiary.setIdNumber(ID_NUMBER);
        healthPolicy.setBeneficiaries(Collections.singletonList(healthBeneficiary)); //Health dependent must match cardholder
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenReturn(healthPolicy);
        //Main member on health policy
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        //When
        managementService.linkCard(new LinkCardRequest(healthAccountReference, mmaAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
        verify(commerceRelationServiceClient).linkCard(cardReference, healthAccountReference, userId);
        ArgumentCaptor<String> accountArgCaptur = ArgumentCaptor.forClass(String.class);
        verify(commerceRelationServiceClient, times(2)).findMemberUsingAccountReference(accountArgCaptur.capture(),eq(userId));
        List<String> argumentValues = accountArgCaptur.getAllValues();
        assertTrue(argumentValues.contains(healthAccountReference));
        assertTrue(argumentValues.contains(mmaAccountReference));
    }

    @Test
    public void linkCard_shouldThrowException_whenHealthPolicyIsNotFoundForGivenHealthAccountRefToLinkCardTo() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        //Cardholder under Multiply Money
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference); //Card holder IS a dependent on health policy
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        transactionalAccount.setCardholders(Collections.singletonList(cardHolder));
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenThrow(HealthPolicyNotFoundException.class);
        //Main member on health policy
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
        expectedEx.expectMessage("Health policy with number ["+healthAccountReference+"] not found");

        //When
        managementService.linkCard(new LinkCardRequest(healthAccountReference, mmaAccountReference, cardReference, userId)); //Card IS linked to Multiply Money

        //Then
    }

    @Test
    public void linkCard_shouldThrowException_whenCustomerWithHealthAccountRefIsNotFoundOnTraderoot() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        //Cardholder under Multiply Money
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference); //Card holder IS a dependent on health policy
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        transactionalAccount.setCardholders(Collections.singletonList(cardHolder));
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Cardholder must be on health policy
        HealthPolicy healthPolicy = new HealthPolicy();
        HealthBeneficiary healthBeneficiary = new HealthBeneficiary();
        healthBeneficiary.setIdNumber(cardHolderIdNumber);
        healthPolicy.setBeneficiaries(Collections.singletonList(healthBeneficiary)); //Health dependent MATCHES cardholder
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenReturn(healthPolicy);
        //Main member on health policy
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenThrow(CustomerNotFound.class);

        expectedEx.expect(CardLinkingException.class);
        expectedEx.expectMessage("Customer with account reference ["+healthAccountReference+"] not found");

        //When
        managementService.linkCard(new LinkCardRequest(healthAccountReference, mmaAccountReference, cardReference, userId)); //Card NOT linked to Multiply Money

        //Then
    }

    @Test
    public void linkCard_shouldThrowException_whenCustomerWithMmaAccountRefIsNotFoundOnTraderoot() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        //Cardholder under Multiply Money
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference); //Card holder IS a dependent on health policy
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        transactionalAccount.setCardholders(Collections.singletonList(cardHolder));
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenThrow(CustomerNotFound.class);
        //Cardholder must be on health policy
        HealthPolicy healthPolicy = new HealthPolicy();
        HealthBeneficiary healthBeneficiary = new HealthBeneficiary();
        healthBeneficiary.setIdNumber(cardHolderIdNumber);
        healthPolicy.setBeneficiaries(Collections.singletonList(healthBeneficiary)); //Health dependent MATCHES cardholder
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenReturn(healthPolicy);
        //Main member on health policy
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
        expectedEx.expectMessage("Customer with account reference ["+mmaAccountReference+"] not found");

        //When
        managementService.linkCard(new LinkCardRequest(healthAccountReference, mmaAccountReference, cardReference, userId)); //Card NOT linked to Multiply Money

        //Then
    }

    @Test
    public void linkCard_shouldThrowException_whenCardHolderIsNotLinkedToGivenMmaForLinkingToHealth() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        //Cardholder under Multiply Money
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference("232156691253543"); //Card holder who IS a dependent on health policy
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        transactionalAccount.setCardholders(Collections.singletonList(cardHolder));
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Cardholder must be on health policy
        HealthPolicy healthPolicy = new HealthPolicy();
        HealthBeneficiary healthBeneficiary = new HealthBeneficiary();
        healthBeneficiary.setIdNumber(cardHolderIdNumber);
        healthPolicy.setBeneficiaries(Collections.singletonList(healthBeneficiary)); //Health dependent MATCHES cardholder
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenReturn(healthPolicy);
        //Main member on health policy
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
        expectedEx.expectMessage("Card holder not found for card reference ["+cardReference+"]");

        //When
        managementService.linkCard(new LinkCardRequest(healthAccountReference, mmaAccountReference, cardReference, userId)); //Card NOT linked to Multiply Money

        //Then
    }

    @Test
    public void linkCard_shouldThrowException_whenCardHolderIsNotLinkedToGivenHealthForLinkingToExistingMma() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        //Cardholder under Multiply Money
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Cardholder must be on health policy
        HealthPolicy healthPolicy = new HealthPolicy();
        HealthBeneficiary healthBeneficiary = new HealthBeneficiary();
        healthBeneficiary.setIdNumber(cardHolderIdNumber);
        healthPolicy.setBeneficiaries(Collections.singletonList(healthBeneficiary)); //Health dependent MATCHES cardholder
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenReturn(healthPolicy);
        //Main member on health policy
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference("232156691253543"); //Card holder who IS linked to Health Saver Shadow Account
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        healthAccount.setCardholders(Collections.singletonList(cardHolder));
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
        expectedEx.expectMessage("Card holder not found for card reference ["+cardReference+"]");

        //When
        managementService.linkCard(new LinkCardRequest(mmaAccountReference, healthAccountReference, cardReference, userId)); //Card NOT linked to Health Saver Shadow Account

        //Then
    }

    @Test
    public void linkCard_shouldThrowException_whenCardHolderOnMmaAboutToBeLinkedToHealthIsStopped() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        //Cardholder under Multiply Money
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(cardHolderIdNumber));
        cardHolder.setStatus(CardStatus.IN_ACTIVE); //Card IS stopped
        transactionalAccount.setCardholders(Collections.singletonList(cardHolder)); //Card holder who IS a dependent on health policy
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Cardholder must be on health policy
        HealthPolicy healthPolicy = new HealthPolicy();
        HealthBeneficiary healthBeneficiary = new HealthBeneficiary();
        healthBeneficiary.setIdNumber(cardHolderIdNumber);
        healthPolicy.setBeneficiaries(Collections.singletonList(healthBeneficiary)); //Health dependent MATCHES cardholder
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenReturn(healthPolicy);
        //Main member on health policy
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
        expectedEx.expectMessage("Card requested for linking must not be stopped");

        //When
        managementService.linkCard(new LinkCardRequest(healthAccountReference, mmaAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
    }

    @Test
    public void linkCard_shouldThrowException_whenCardHolderOnHealthAboutToBeLinkedIsStopped() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        //Cardholder under Multiply Money
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Cardholder must be on health policy
        HealthPolicy healthPolicy = new HealthPolicy();
        HealthBeneficiary healthBeneficiary = new HealthBeneficiary();
        healthBeneficiary.setIdNumber(cardHolderIdNumber);
        healthPolicy.setBeneficiaries(Collections.singletonList(healthBeneficiary)); //Health dependent MATCHES cardholder
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenReturn(healthPolicy);
        //Main member on health policy
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.IN_ACTIVE); //Card IS stopped
        healthAccount.setCardholders(Collections.singletonList(cardHolder)); //Card holder who IS linked to Health Saver Shadow Account
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
        expectedEx.expectMessage("Card requested for linking must not be stopped");

        //When
        managementService.linkCard(new LinkCardRequest(mmaAccountReference, healthAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
    }

	@Test
	public void linkCard_shouldThrowException_whenCardHolderIsNotDependentOnHealth() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(cardHolderIdNumber);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(cardHolderIdNumber));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        transactionalAccount.setCardholders(Collections.singletonList(cardHolder)); //Cardholder UNDER Multiply Money
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Cardholder on health policy
        HealthPolicy healthPolicy = new HealthPolicy();
        HealthBeneficiary healthBeneficiary = new HealthBeneficiary();
        healthBeneficiary.setIdNumber("8103055630083");
        healthPolicy.setBeneficiaries(Collections.singletonList(healthBeneficiary)); //Health dependent DOES NOT match cardholder
        when(healthService.getHealthMemberDetails(Long.parseLong(healthAccountReference))).thenReturn(healthPolicy);
        //Money management customer on health policy (main member)
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
        expectedEx.expectMessage("Cardholder with Id [7712155060081] not a dependent on health policy");
        //When
        managementService.linkCard(new LinkCardRequest(healthAccountReference, mmaAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
    }

    @Test
	public void linkCard_shouldNotCallHealthService_whenAccountToLinkToIsNotHealth() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        String cardHolderIdNumber = "7712155060081";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
	    transactionalAccount.addCardHolder(cardHolder);
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Money management customer on health policy (main member)
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setCardholders(Collections.singletonList(cardHolder)); //Cardholder IS under Health Saver Shadow Account
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        //When
        managementService.linkCard(new LinkCardRequest(mmaAccountReference, healthAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
        verify(healthService,never()).getHealthMemberDetails(anyLong());
    }

    @Test
	public void linkCard_shouldThrowException_whenAccountToLinkToMultiplyMoneyButNotTransanctional() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_SAVINGS);
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
	    transactionalAccount.addCardHolder(cardHolder);
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Money management customer on health policy (main member)
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        healthAccount.setCardholders(Collections.singletonList(cardHolder)); //Cardholder IS under Health Saver Shadow Account
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
	    expectedEx.expectMessage("Not allowed to link card to Savings Payment Wallet");
        //When
        managementService.linkCard(new LinkCardRequest(mmaAccountReference, healthAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
        verify(healthService,never()).getHealthMemberDetails(anyLong());
    }

    @Test
	public void linkCard_shouldThrowException_whenCardHoldersIdDocIsNull() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Money management customer on health policy (main member)
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(null);
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        healthAccount.setCardholders(Collections.singletonList(cardHolder)); //Cardholder IS under Health Saver Shadow Account
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
	    expectedEx.expectMessage("Invalid Identification Document for cardholder with card reference [02099309093093]");
        //When
        managementService.linkCard(new LinkCardRequest(mmaAccountReference, healthAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
        verify(healthService,never()).getHealthMemberDetails(anyLong());
    }

    @Test
	public void linkCard_shouldThrowException_whenCustomerToLinkToIdDocIsNull() throws Exception {
        //Given
	    String multiplyMoneyCustomerReference = "123456";
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
        String userId = "TestAgent";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        multiplyMoneyHolder.setIdentificationDocument(null);
	    multiplyMoneyHolder.setCustomerReference(multiplyMoneyCustomerReference);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Money management customer on health policy (main member)
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(ID_NUMBER));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        healthAccount.setCardholders(Collections.singletonList(cardHolder)); //Cardholder IS under Health Saver Shadow Account
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
	    expectedEx.expectMessage("Invalid Identification Document for customer with member reference [123456]");
        //When
        managementService.linkCard(new LinkCardRequest(mmaAccountReference, healthAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
        verify(healthService,never()).getHealthMemberDetails(anyLong());
    }

    @Test
	public void linkCard_shouldThrowException_whenCardHoldersIdDoesNotMatchCustomerOfMmaAccToLinkTo() throws Exception {
        //Given
        String healthAccountReference = "9010920902";
        String mmaAccountReference = "198989289892";
        String cardReference = "02099309093093";
	    String cardHolderIdNumber = "7712155060081";
        String userId = "TestAgent";
        //Customer with multiply money
        Customer multiplyMoneyHolder = createMultiplyCustomerWithId(ID_NUMBER);
        //Transactional account of Multiply Money Acc
        CustomerAccount transactionalAccount = new MultiplyMoneyAccount();
        transactionalAccount.setAccountReference(mmaAccountReference);
		transactionalAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        multiplyMoneyHolder.addAccount(transactionalAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(mmaAccountReference, userId)).thenReturn(multiplyMoneyHolder);
        //Money management customer on health policy (main member)
        Customer healthCustomer = new Customer();
        //Health Saver Shadow Account
        CustomerAccount healthAccount = new CustomerAccount();
        CardHolder cardHolder = new CardHolder();
        cardHolder.setCardReference(cardReference);
        cardHolder.setIdentificationDocument(IdentificationDocument.createSouthAfricanId(cardHolderIdNumber));
        cardHolder.setStatus(CardStatus.ACTIVE); //Card IS NOT stopped
        healthAccount.addCardHolder(cardHolder); //Cardholder IS under Health Saver Shadow Account
        healthAccount.setAccountReference(healthAccountReference);
        healthAccount.setIssuingProductCode(ProductCode.HEALTH);
        healthCustomer.addAccount(healthAccount);
        when(commerceRelationServiceClient.findMemberUsingAccountReference(healthAccountReference, userId)).thenReturn(healthCustomer);

        expectedEx.expect(CardLinkingException.class);
	    expectedEx.expectMessage("Customer with account[198989289892] does not match card holder of card reference[02099309093093]");
        //When
        managementService.linkCard(new LinkCardRequest(mmaAccountReference, healthAccountReference, cardReference, userId)); //Card MUST be linked to Multiply Money

        //Then
        verify(healthService,never()).getHealthMemberDetails(anyLong());
    }

    private Customer createMultiplyCustomerWithId(String otherIdNumber) {
        Customer customerToLinkTo = new MultiplyCustomer();
        IdentificationDocument otherId = SouthAfricanIdDocument.createSouthAfricanId(otherIdNumber);
        customerToLinkTo.setIdentificationDocument(otherId);
        return customerToLinkTo;
    }

    /**
     * This just tests the flow. That when you call autoApply with no customer footprint we go into
     * <code>applyForLightWeightAccount</code>
     * @throws Exception
     */
    @Test
    public void autoApply_applyForLightWeightAccount() throws Exception {
        String cmsClientNumber = "123456789";
        final String name = "Isaac";
        final String surname = "Newton";

        Customer customer = this.createTestCustomer();
        customer.setName(name);
        customer.setSurname(surname);

        Entity policyEntity = MockObjectCreator.createPolicyEntity();

        expectedEx.expect(NoFootPrintException.class);
        expectedEx.expectMessage("No footprint");

        CDIEntityClient cdiEntityClient = Mockito.mock(CDIEntityClient.class);
        when(cdiEntityClient.getFootPrintWithBusinessKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber)).thenReturn(null);

        MemberUtility memberUtility = new MemberUtility();
        memberUtility.setPolicyClient(MockObjectCreator.createPolicyClientMock());
        managementService.setMemberUtility(memberUtility);
        managementService.setCdiEntityClient(cdiEntityClient);

        MoneyMemberManagementServiceImpl managementServiceSpy = Mockito.spy(managementService);
        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(Customer.class);

        try {
            managementServiceSpy.autoApply(cmsClientNumber, customer, false, "TestAgent");
        }
        finally {
            verify(managementServiceSpy).applyForLightWeightAccount(Mockito.anyString(), customerArgumentCaptor.capture(), Mockito.anyBoolean(), Mockito.anyString());
            Customer customerAsParameter = customerArgumentCaptor.getValue();
            assertNotEquals(policyEntity.getSurname(),customerAsParameter.getSurname());
            assertEquals(customer.getSurname(),customerAsParameter.getSurname());
            //makes sure that the policy does in fact supplement the original customer object
            assertEquals(policyEntity.getContactDetails().getResidentialAddress().getLine(1),customerAsParameter.getPhysicalAddress().getLine1());
        }
    }

    /**
     * This just tests the flow. That when you call autoApply with no customer footprint we go into
     * <code>applyForNewAccount</code>
     * @throws Exception
     */
    @Test
    public void autoApply_applyForNewAccount() throws Exception {
        String cmsClientNumber = "123456789";

        Customer customer = this.createTestCustomer();

        expectedEx.expect(IllegalArgumentException.class);

        FootPrint footPrint = FootPrint.builder().build();
        footPrint.putKey(BusinessKeyType.MONEY_CDI_KEY,cmsClientNumber);
        CDIEntityClient cdiEntityClient = Mockito.mock(CDIEntityClient.class);

        when(cdiEntityClient.getFootPrintWithBusinessKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber)).thenReturn(footPrint);

        MemberUtility memberUtility = new MemberUtility();
        memberUtility.setPolicyClient(MockObjectCreator.createPolicyClientMock());
        managementService.setMemberUtility(memberUtility);
        managementService.setCdiEntityClient(cdiEntityClient);

        MoneyMemberManagementServiceImpl managementServiceSpy = Mockito.spy(managementService);

        try {
            managementServiceSpy.autoApply(cmsClientNumber, customer, false, "TestAgent");
        }
        finally {
            verify(managementServiceSpy).applyForNewAccount(Mockito.any(), Mockito.any(Customer.class), Mockito.anyBoolean(), Mockito.anyString());
        }
    }

    @Test
    public void autoApply_autoCreateFromPolicy() throws Exception {
        String cmsClientNumber = "123456789";

        Customer customer = this.createTestCustomer();

        expectedEx.expect(NoFootPrintException.class);
        expectedEx.expectMessage("No footprint");

        CDIEntityClient cdiEntityClient = Mockito.mock(CDIEntityClient.class);
        when(cdiEntityClient.getFootPrintWithBusinessKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber)).thenReturn(null);

        MemberUtility memberUtility = new MemberUtility();
        memberUtility.setPolicyClient(MockObjectCreator.createPolicyClientMock());
        managementService.setMemberUtility(memberUtility);
        managementService.setCdiEntityClient(cdiEntityClient);

        MoneyMemberManagementServiceImpl managementServiceSpy = Mockito.spy(managementService);

        try {
            managementServiceSpy.autoApply (cmsClientNumber, null,false,"TestAgent");
        }
        finally {
            verify(managementServiceSpy).applyForLightWeightAccount(Mockito.any(), Mockito.any(Customer.class), Mockito.anyBoolean(), Mockito.anyString());
        }
    }

    @Test
    public void applyForLightWeightAccount_shouldThrowMemberException_onIdValidationFailure() throws Exception {
        String invalidRsaIdNumber = "456947";

        expectedEx.expect(MemberManagementException.class);
        expectedEx.expectMessage(String.format("Invalid South African ID received: %s", invalidRsaIdNumber));

        Customer customer = CustomerTestConstants.createCustomer();
        customer.setIdentificationDocument(createSouthAfricanId(invalidRsaIdNumber));

        allowStartBpmToRunWithoutThrowingException(customer, "");
        managementService.applyForLightWeightAccount("20012309202", customer, false, "TestAgent");
        Mockito.verify(bpmFacade).startBPMProcess(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
    }

    @Test
    public void duplicateEmailAddress() throws Exception {
        expectedEx.expect(MemberManagementException.class);
        MultiplyCustomer customer = new MultiplyCustomer();
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        customer.setMobilePhoneNumber("+27728081000");
        CommerceRelationServiceClient commerceRelationServiceClient = Mockito.mock(CommerceRelationServiceClient.class);
        managementService.setCommerceRelationServiceClient(commerceRelationServiceClient);

        Map<String,String> errorMap = new HashMap<>();
        errorMap.put("15","com.mmiholdings.service.money.commerce.member.CommerceException: PMCRME:Duplicate Cellphone Number found");
        errorMap.put("E102","Add Member unsuccessful");

        CommerceException commerceException = new CommerceException("blah","blah",errorMap);
        Mockito.when(commerceRelationServiceClient.createOrUpdateMember(Mockito.any())).thenThrow(commerceException);
        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(Mockito.any(),Mockito.any())).thenThrow(new CustomerNotFound());

        FootPrint footPrint = new FootPrint();
        final String cmsClientNumber = "20012309202";

        ((RuleCheckerServiceImpl) this.ruleCheckerService).setPolicyClient(policyClient);
        ((RuleCheckerServiceImpl) this.ruleCheckerService).setRelationServiceClient(commerceRelationServiceClient);

        footPrint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        Mockito.when(cdiEntityClient.getFootPrintWithBusinessKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber)).thenReturn(footPrint);
        allowStartBpmToRunWithoutThrowingException(customer, cmsClientNumber);


        MoneyMemberManagementServiceImpl managementServiceSpy = Mockito.spy(managementService);

        try {
            managementServiceSpy.applyForLightWeightAccount(cmsClientNumber, customer, false, "TestAgent");
        }
        finally {
            ArgumentCaptor<String> capturedpolicyNumber = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> capturedidNumber = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> capturedcmsClientNumber = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> capturedexceptionType = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> capturedexceptionDetails = ArgumentCaptor.forClass(String.class);
            Mockito.verify(bpmFacade).startBPMProcess(
                    capturedpolicyNumber.capture(),
                    capturedidNumber.capture(),
                    capturedcmsClientNumber.capture(),
                    capturedexceptionType.capture(),
                    capturedexceptionDetails.capture());
            //assertEquals("PMCRME:Duplicate Cellphone Number found : +27999999999",capturedexceptionDetails.getValue());
        }
    }

    @Test
    public void setterFromField_shouldAddSetToName_whenFieldNameIsPassed() {
        String fieldName = "idNumber";
        String expectedSetterName = "setIdNumber";

        String actualResult = managementService.setterFromField(fieldName);

        assertEquals(expectedSetterName, actualResult);
    }

    @Test
    public void validateInput_shouldDefaulValues_whenNotProvided() {
        Customer customer = new Customer();
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        MultiplyMoneyAccount customerAccount = new MultiplyMoneyAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(MULTIPLY_SAVINGS);
        customer.addAccount(customerAccount);
        managementService.defaultMissingCustomerDetails(customer);
        assertEquals(null, customer.getMobilePhoneNumber());
    }

    @Test
    public void applyForNewCard_shouldTrowException_whenNoMultiplyAccount() throws CustomerNotFound {
        String cmsClientNumber = "20012309202";

        Customer customer = new Customer();
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.addAccount(customerAccount);

        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(ID_NUMBER, IdentificationDocument.Type.NATIONAL_ID))
                .thenReturn(customer);

        expectedEx.expect(MemberManagementException.class);
        expectedEx.expectMessage("Customer does not have a Multiply Money Account");

        managementService.applyVisaCard(cmsClientNumber, customer, true, "agentId");
    }

    @Test
    public void applyForNewCard_shouldTrowException_whenHealthContainsCardHolderAlready() throws CustomerNotFound {
        String cmsClientNumber = "20012309202";

        Customer customer = new Customer();
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(ProductCode.HEALTH);
        customer.addAccount(customerAccount);
        CardHolder cardHolder = CardHolder.fromCustomer(customer);
        cardHolder.setStatus(CardStatus.ACTIVE);
        cardHolder.setCardReference(CARD_REFERENCE);
        customerAccount.addCardHolder(cardHolder);

        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(ID_NUMBER, IdentificationDocument.Type.NATIONAL_ID))
                .thenReturn(customer);

        expectedEx.expect(MemberManagementException.class);
        expectedEx.expectMessage("Customer already has a Visa Card");

        managementService.applyVisaCard(cmsClientNumber, customer, true, "agentId");
    }


    @Test
    public void applyForNewCard_shouldSucceed_whenTransactionalAccountNoCards() throws CustomerNotFound, CommerceException {
        String cmsClientNumber = "20012309202";

        Customer customer = new Customer();
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        customer.addAccount(customerAccount);
        String customerRef = "29834989383";

        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(ID_NUMBER, IdentificationDocument.Type.NATIONAL_ID))
                .thenReturn(customer);
        Mockito.when(commerceRelationServiceClient.createOrUpdateMember(Mockito.any(MemberUpdateRequest.class))).thenReturn(customerRef);
        Mockito.when(commerceRelationServiceClient.findMemberUsingMemberReference(customerRef)).thenReturn(customer);

        managementService.applyVisaCard(cmsClientNumber, customer, true, "agentId");

        Mockito.verify(commerceRelationServiceClient).createOrUpdateMember(Mockito.any(MemberUpdateRequest.class));

    }


    @Test
    public void applyForNewCard_shouldSucceed_whenHealthOnlyCardForSpouse() throws CustomerNotFound, CommerceException {
        String cmsClientNumber = "20012309202";

        Customer customer = new Customer();
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        customer.addAccount(customerAccount);
        CardHolder cardHolder = CardHolder.fromCustomer(customer);
        cardHolder.setIdentificationDocument(createSouthAfricanId(ID_NUMBER_SPOUSE));
        cardHolder.setStatus(CardStatus.ACTIVE);
        cardHolder.setCardReference(CARD_REFERENCE);
        customerAccount.addCardHolder(cardHolder);
        String customerRef = "29834989383";

        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(ID_NUMBER, IdentificationDocument.Type.NATIONAL_ID))
                .thenReturn(customer);
        Mockito.when(commerceRelationServiceClient.createOrUpdateMember(Mockito.any(MemberUpdateRequest.class))).thenReturn(customerRef);
        Mockito.when(commerceRelationServiceClient.findMemberUsingMemberReference(customerRef)).thenReturn(customer);

        managementService.applyVisaCard(cmsClientNumber, customer, true, "agentId");

        Mockito.verify(commerceRelationServiceClient).createOrUpdateMember(Mockito.any(MemberUpdateRequest.class));

    }

    @Test
    public void applyForNewCard_shouldSetTerms_whenTermsPassed() throws CustomerNotFound, CommerceException {
        String cmsClientNumber = "20012309202";

        Customer customer = new Customer();
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        customer.addAccount(customerAccount);
        CardHolder cardHolder = CardHolder.fromCustomer(customer);
        cardHolder.setIdentificationDocument(createSouthAfricanId(ID_NUMBER_SPOUSE));
        cardHolder.setStatus(CardStatus.ACTIVE);
        cardHolder.setCardReference(CARD_REFERENCE);
        customerAccount.addCardHolder(cardHolder);
        String customerRef = "29834989383";

        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(ID_NUMBER, IdentificationDocument.Type.NATIONAL_ID))
                .thenReturn(customer);
        Mockito.when(commerceRelationServiceClient.createOrUpdateMember(Mockito.any(MemberUpdateRequest.class))).thenReturn(customerRef);
        Mockito.when(commerceRelationServiceClient.findMemberUsingMemberReference(customerRef)).thenReturn(customer);

        //Test
        managementService.applyVisaCard(cmsClientNumber, customer, true, "agentId");

        ArgumentCaptor<MemberUpdateRequest> captor = ArgumentCaptor.forClass(MemberUpdateRequest.class);

        Mockito.verify(commerceRelationServiceClient).createOrUpdateMember(captor.capture());
        MemberUpdateRequest memberUpdateRequest = captor.getValue();
        CustomerAccount customerAccountUpdated = memberUpdateRequest.getCustomer().getCustomerAccount(ProductCode.MULTIPLY_TRANSACTIONAL).get();

        Assert.assertTrue(customerAccountUpdated.getCardholders().get(1).isCardTermsAndConditionsAccepted());
        Assert.assertNotNull(customerAccountUpdated.getCardholders().get(1).getCardTermsAndConditionsAcceptedDate());

    }

    @Test
    public void applyForNewCard_shouldSucceed_whenHealthContainsNotActiveCardHolderAlready() throws CustomerNotFound, CommerceException {
        String cmsClientNumber = "20012309202";

        Customer customer = new Customer();
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(ProductCode.MULTIPLY_TRANSACTIONAL);
        customer.addAccount(customerAccount);
        CardHolder cardHolder = CardHolder.fromCustomer(customer);
        cardHolder.setStatus(CardStatus.DAMAGED);
        cardHolder.setCardReference(CARD_REFERENCE);
        customerAccount.addCardHolder(cardHolder);
        String customerRef = "29834989383";

        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(ID_NUMBER, IdentificationDocument.Type.NATIONAL_ID))
                .thenReturn(customer);
        Mockito.when(commerceRelationServiceClient.createOrUpdateMember(Mockito.any(MemberUpdateRequest.class))).thenReturn(customerRef);
        Mockito.when(commerceRelationServiceClient.findMemberUsingMemberReference(customerRef)).thenReturn(customer);

        managementService.applyVisaCard(cmsClientNumber, customer, true, "agentId");

        Mockito.verify(commerceRelationServiceClient).createOrUpdateMember(Mockito.any(MemberUpdateRequest.class));

    }

    @Test
    public void applyForNewAccount_fail_whenNoFootprint() throws CustomerNotFound {
        Customer customer = new MultiplyCustomer();
        customer.setTitle(CustomerTestConstants.TITLE);
        customer.setName(CustomerTestConstants.NAME);
        customer.setInitials(CustomerTestConstants.INITIALS);
        customer.setSurname(CustomerTestConstants.SURNAME);
        customer.setDateOfBirth(CustomerTestConstants.DATE_OF_BIRTH);
        customer.setGender(Gender.MALE);
        customer.setMobilePhoneNumber(MOBILE_NUMBER);
        Address address = Address.builder().line1(PHYSICAL_LINE_1).suburb(SUBURB).postalCode(POSTAL_CODE).build();
        customer.setPhysicalAddress(address);
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        MultiplyMoneyAccount customerAccount = new MultiplyMoneyAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(MULTIPLY_SAVINGS);
        customer.addAccount(customerAccount);
        String cmsClientNumber = "20012309202";

        Mockito.when(cdiEntityClient.getFootPrintWithCms(cmsClientNumber)).thenReturn(null);

        expectedEx.expect(NoFootPrintException.class);
        expectedEx.expectMessage("No footprint");

        allowStartBpmToRunWithoutThrowingException(customer, cmsClientNumber);
        managementService.applyForNewAccount(cmsClientNumber, customer, false, "userId");
        Mockito.verify(bpmFacade).startBPMProcess(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString());

    }

    @Test
    public void applyForNewAccount_fail_whenCmsFootprint() throws CustomerNotFound {
        Customer customer = new MultiplyCustomer();
        customer.setTitle(CustomerTestConstants.TITLE);
        customer.setName(CustomerTestConstants.NAME);
        customer.setInitials(CustomerTestConstants.INITIALS);
        customer.setSurname(CustomerTestConstants.SURNAME);
        customer.setDateOfBirth(CustomerTestConstants.DATE_OF_BIRTH);
        customer.setGender(Gender.MALE);
        customer.setMobilePhoneNumber(MOBILE_NUMBER);
        Address address = Address.builder().line1(PHYSICAL_LINE_1).suburb(SUBURB).postalCode(POSTAL_CODE).build();
        customer.setPhysicalAddress(address);
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        MultiplyMoneyAccount customerAccount = new MultiplyMoneyAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setIssuingProductCode(MULTIPLY_SAVINGS);
        customer.addAccount(customerAccount);
        String cmsClientNumber = "20012309202";

        allowStartBpmToRunWithoutThrowingException(customer, cmsClientNumber);
        Mockito.when(cdiEntityClient.getFootPrintWithCms(cmsClientNumber)).thenReturn(FootPrint.builder().build());

        expectedEx.expect(NoFootPrintException.class);
        expectedEx.expectMessage("No footprint");

        managementService.applyForNewAccount(cmsClientNumber, customer, false, "userId");
        Mockito.verify(bpmFacade).startBPMProcess(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
    }

    private void allowStartBpmToRunWithoutThrowingException(Customer customer, String cmsClientNumber) {
        Mockito.when(policyClient.getPolicy(!StringUtils.isEmpty(cmsClientNumber)?Long.valueOf(cmsClientNumber):Mockito.anyLong())).thenReturn(new Policy(){
            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public PolicyNumber getPolicyNumber() {
                PolicyNumber policyNumber = new PolicyNumber();
                policyNumber.setNumber(123456L);
                return policyNumber;
            }
            @Override
            public Entity getPolicyHolder() {
                Entity holder = new Entity();
                holder.setIdNumber(customer.getIdentificationDocument().getIdNumber());
            return holder;
        }});
    }

    @Test
    public void validateFica_success() throws CommerceException, CustomerNotFound {

        if(!FeatureFlags.FICA_VERIFICATION){
            assertFalse(FeatureFlags.FICA_VERIFICATION);
            return;
        }

        Customer customer = createTestCustomer();
        String cmsClientNumber = "20012309202";
        String testAgent = "Agent 007";
        String memberReference = "12345678";
        String idNumber = "7401020005083";
        customer.setIdentificationDocument(SouthAfricanIdDocument.createSouthAfricanId(idNumber));

        Policy policy = generateFicaPolicy();

        ClientFicaStatus ficaStatus = new ClientFicaStatus();
        ficaStatus.getPersonVerified().setVerified(YesNoUnspecified.Y);
        ficaStatus.getPersonVerified().setTokenId("abcdefghijklmnop");
        ficaStatus.getCellphoneVerified().setVerified(YesNoUnspecified.Y);
        ficaStatus.getCellphoneVerified().setTokenId("abcdefghijklmnop");

        Mockito.when(policyClient.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);
        String mobileNumber = customer.getMobilePhoneNumber().replaceAll("\\+27", "0");
        Mockito.when(ficaClient.verifyMobileAndPerson(idNumber, customer.getName(), customer.getSurname(), mobileNumber)).thenReturn(ficaStatus);
        Mockito.when(commerceRelationServiceClient.createOrUpdateMember(Mockito.any(MemberUpdateRequest.class))).thenReturn(memberReference);
        Mockito.when(commerceRelationServiceClient.findMemberUsingMemberReference(memberReference)).thenReturn(customer);
        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(idNumber,IdentificationDocument.Type.NATIONAL_ID)).thenReturn(customer);

        FicaFacade ficaFacade = new FicaFacade();
        ficaFacade.setCommerceClient(commerceRelationServiceClient);
        ficaFacade.setFicaClient(ficaClient);
        ficaFacade.setPolicyClient(policyClient);
        this.managementService.setFicaFacade(ficaFacade);

        this.managementService.validateFica(customer, cmsClientNumber, testAgent);

        ArgumentCaptor<MemberUpdateRequest> memberUpdateRequestArgumentCaptor = ArgumentCaptor.forClass(MemberUpdateRequest.class);
        Mockito.verify(policyClient, Mockito.times(0)).getPolicy(Mockito.anyLong());
        Mockito.verify(commerceRelationServiceClient, Mockito.times(1)).createOrUpdateMember(memberUpdateRequestArgumentCaptor.capture());

        MemberUpdateRequest request = memberUpdateRequestArgumentCaptor.getValue();

        request.getCustomer().getCustomerAccounts().stream().map(acc->(MultiplyMoneyAccount) acc).forEach(acc-> {
            assertEquals(CustomerAccountStatus.ACTIVE,acc.getStatus());
        });
    }

    @Test
    public void validateFica_failed() throws CommerceException, CustomerNotFound {

        if(!FeatureFlags.FICA_VERIFICATION){
            assertFalse(FeatureFlags.FICA_VERIFICATION);
            return;
        }

        Customer customer = createTestCustomer();
        String cmsClientNumber = "20012309202";
        String testAgent = "Agent 007";
        String memberReference = "12345678";
        String idNumber = "7401020005083";
        customer.setIdentificationDocument(SouthAfricanIdDocument.createSouthAfricanId(idNumber));

        Policy policy = generateFicaPolicy();


        PolicyClient policyClient = Mockito.mock(PolicyClient.class);
        FicaClient ficaClient = Mockito.mock(FicaClient.class);
        CommerceRelationServiceClient commerceRelationServiceClient =
                Mockito.mock(CommerceRelationServiceClient.class);


        ClientFicaStatus ficaStatus = new ClientFicaStatus();
        ficaStatus.getPersonVerified().setVerified(YesNoUnspecified.N);
        ficaStatus.getPersonVerified().setTokenId("abcdefghijklmnop");
        ficaStatus.getCellphoneVerified().setVerified(YesNoUnspecified.Y);
        ficaStatus.getCellphoneVerified().setTokenId("abcdefghijklmnop");

        Mockito.when(policyClient.getPolicy(Long.valueOf(cmsClientNumber))).thenReturn(policy);
        Mockito.when(ficaClient.verifyMobileAndPerson(idNumber, customer.getName(), customer.getSurname(), customer.getMobilePhoneNumber().replaceAll("\\+27", "0"))).thenReturn(ficaStatus);
        Mockito.when(ficaClient.startProcessToRetrieveFicaDetailsFromClient(Mockito.anyString(), Mockito.anyString(), Mockito.any(ClientFicaStatus.class))).thenReturn(true);
        Mockito.when(commerceRelationServiceClient.createOrUpdateMember(Mockito.any(MemberUpdateRequest.class))).thenReturn(memberReference);
        Mockito.when(commerceRelationServiceClient.findMemberUsingMemberReference(memberReference)).thenReturn(customer);
        IdentificationDocument id = customer.getIdentificationDocument();
        Mockito.when(commerceRelationServiceClient.findMemberUsingIdNumber(id.getIdNumber(), id.getType())).thenReturn(customer);

        FicaFacade ficaFacade = new FicaFacade();
        ficaFacade.setCommerceClient(commerceRelationServiceClient);
        ficaFacade.setFicaClient(ficaClient);
        ficaFacade.setPolicyClient(policyClient);

        this.managementService.setFicaFacade(ficaFacade);
        this.managementService.setPolicyClient(policyClient);
        this.managementService.setCommerceRelationServiceClient(commerceRelationServiceClient);

        this.managementService.validateFica(customer, cmsClientNumber, testAgent);

        ArgumentCaptor<MemberUpdateRequest> memberUpdateRequestArgumentCaptor = ArgumentCaptor.forClass(MemberUpdateRequest.class);
        Mockito.verify(policyClient, Mockito.times(1)).getPolicy(Mockito.anyLong());
        Mockito.verify(commerceRelationServiceClient, Mockito.times(1)).createOrUpdateMember(memberUpdateRequestArgumentCaptor.capture());

        MemberUpdateRequest request = memberUpdateRequestArgumentCaptor.getValue();

        request.getCustomer().getCustomerAccounts().stream().map(acc->(MultiplyMoneyAccount) acc).forEach(acc-> {
            assertEquals(CustomerAccountStatus.IN_ACTIVE,acc.getStatus());
        });
    }

    private Policy generateFicaPolicy() {
        Policy policy = new Policy();
        PolicyNumber policyNumber = new PolicyNumber();
        policyNumber.setNumber(512239030L);
        policyNumber.setProductCode("MB");
        policy.setPolicyNumber(policyNumber);
        return policy;
    }

    private Customer createTestCustomer() {
        Customer customer = new MultiplyCustomer();
        customer.setTitle(CustomerTestConstants.TITLE);
        customer.setName(CustomerTestConstants.NAME);
        customer.setInitials(CustomerTestConstants.INITIALS);
        customer.setSurname(CustomerTestConstants.SURNAME);
        customer.setDateOfBirth(CustomerTestConstants.DATE_OF_BIRTH);
        customer.setGender(Gender.MALE);
        customer.setMobilePhoneNumber(MOBILE_NUMBER);
        Address address = Address.builder().line1(PHYSICAL_LINE_1).suburb(SUBURB).postalCode(POSTAL_CODE).build();
        customer.setPhysicalAddress(address);
        customer.setIdentificationDocument(createSouthAfricanId(ID_NUMBER));
        MultiplyMoneyAccount customerAccount = new MultiplyMoneyAccount();
        customerAccount.setAccountTypeCode(SAVINGS);
        customerAccount.setStatus(CustomerAccountStatus.IN_ACTIVE);
        customerAccount.setIssuingProductCode(MULTIPLY_SAVINGS);
        customer.addAccount(customerAccount);
        return customer;
    }

}