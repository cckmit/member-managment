package com.mmiholdings.member.money.service;

import com.google.common.base.Optional;
import com.mmiholdings.member.money.api.LinkCardRequest;
import com.mmiholdings.client.fica.FicaClient;
import com.mmiholdings.member.money.api.LoggingService;
import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.member.money.api.MoneyMemberService;
import com.mmiholdings.member.money.api.domain.ApplicationResult;
import com.mmiholdings.member.money.api.dto.CashbackDeposit;
import com.mmiholdings.member.money.api.rule.CardLinkingException;
import com.mmiholdings.member.money.service.clients.*;
import com.mmiholdings.member.money.service.clients.dto.DepositRequest;
import com.mmiholdings.member.money.service.clients.dto.MemberUpdateRequest;
import com.mmiholdings.member.money.service.rule.RuleCheckerService;
import com.mmiholdings.member.money.service.util.MemberUtility;
import com.mmiholdings.member.money.service.util.TermsAndConditionsHelper;
import com.mmiholdings.multiply.service.communications.email.SendEmailAttachment;
import com.mmiholdings.multiply.service.communications.email.SendEmailException;
import com.mmiholdings.multiply.service.communications.email.SendEmailRequest;
import com.mmiholdings.multiply.service.entity.AddPartyWebserviceException;
import com.mmiholdings.multiply.service.entity.BusinessKeyType;
import com.mmiholdings.multiply.service.entity.FootPrint;
import com.mmiholdings.multiply.service.entity.NaturalEntity;
import com.mmiholdings.multiply.service.entity.client.CDIEntityClient;
import com.mmiholdings.multiply.service.health.HealthBeneficiary;
import com.mmiholdings.multiply.service.health.HealthPolicy;
import com.mmiholdings.multiply.service.policy.Policy;
import com.mmiholdings.multiply.service.policy.plan.PlanType;
import com.mmiholdings.service.fica.dto.YesNoUnspecified;
import com.mmiholdings.service.fica.dto.server.responses.ClientFicaStatus;
import com.mmiholdings.service.fica.exceptions.FicaException;
import com.mmiholdings.service.money.commerce.member.AccountTypeCode;
import com.mmiholdings.service.money.commerce.member.Address;
import com.mmiholdings.service.money.commerce.member.BusinessException;
import com.mmiholdings.service.money.commerce.member.CardFilter;
import com.mmiholdings.service.money.commerce.member.CardHolder;
import com.mmiholdings.service.money.commerce.member.CardHolder;
import com.mmiholdings.service.money.commerce.member.CommerceException;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.CustomerAccount;
import com.mmiholdings.service.money.commerce.member.CustomerAccountStatus;
import com.mmiholdings.service.money.commerce.member.CustomerNotFound;
import com.mmiholdings.service.money.commerce.member.FicaStatus;
import com.mmiholdings.service.money.commerce.member.Gender;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import com.mmiholdings.service.money.commerce.member.MultiplyCustomer;
import com.mmiholdings.service.money.commerce.member.MultiplyMoneyAccount;
import com.mmiholdings.service.money.commerce.member.Person;
import com.mmiholdings.service.money.commerce.member.ProductCode;
import com.mmiholdings.service.money.commerce.member.SouthAfricanMobileFormatter;
import com.mmiholdings.service.money.commerce.payment.CashDepositException;
import com.mmiholdings.service.money.util.LoggingInterceptor;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

import static com.mmiholdings.service.money.commerce.member.AccountTypeCode.SAVINGS;
import static com.mmiholdings.service.money.commerce.member.AccountTypeCode.TRANSACTION;
import static com.mmiholdings.service.money.commerce.member.ProductCode.HEALTH;
import static com.mmiholdings.service.money.commerce.member.ProductCode.MULTIPLY_SAVINGS;
import static com.mmiholdings.service.money.commerce.member.ProductCode.MULTIPLY_TRANSACTIONAL;
import static com.mmiholdings.service.money.commerce.member.SouthAfricanIdNumberValidator.validateCheckDigit;
import static java.lang.Integer.valueOf;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.codehaus.plexus.util.ExceptionUtils.getRootCause;



@Log
@Setter
@Stateless
@Local(MoneyMemberService.class)
@Interceptors(LoggingInterceptor.class)
public class MoneyMemberManagementServiceImpl implements MoneyMemberService {

	protected static final String RSA = "RSA";
	protected static final String SUCCESSFULLY_CREATED_NEW_MEMBER = "Successfully created new member";
	protected static final String SUCCESSFULLY_CREATED_NEW_ACCOUNT = "Successfully created new account for member";
    public static final boolean TERMS_NOT_ACCEPTED = false;
    public static final String MOBILE_ERROR_CODE = "15";
    public static final String EMAIL_ERROR_CODE = "14";
    public static final String ID_NUMBER_ERROR_CODE = "6";

    private CommerceRelationServiceClient commerceRelationServiceClient;

	private PaymentServiceClient paymentServiceClient;

	private CDIEntityClient cdiEntityClient;

    @EJB
    private LoggingService loggingService;

	@Inject
	private RuleCheckerService ruleCheckerService;

	private PolicyClient policyClient;

    @Inject
    private MemberUtility memberUtility;

    @Inject
    private FicaFacade ficaFacade;

    private SendEmailServiceClient emailServiceClient;

	@Inject
	private HealthServiceClient healthService;

    @Inject
    private MoneyServicesConfig moneyServicesConfig;

    @Inject
    private BusinessProcessManagementAutoAccountFacade bpmAccountFacade;

    @PostConstruct
    public void init() {
        policyClient = new PolicyClient(moneyServicesConfig.getPolicyUrl());
        commerceRelationServiceClient = new CommerceRelationServiceClient(moneyServicesConfig.getCommerceUrl());
        paymentServiceClient = new PaymentServiceClient(moneyServicesConfig.getCommerceUrl());
        emailServiceClient = new SendEmailServiceClient(moneyServicesConfig.getCommunicationsUrl());
        cdiEntityClient = new CDIEntityClient(moneyServicesConfig.getEntityUrl());

        ficaFacade.setFicaClient(new FicaClient(moneyServicesConfig.getFicaUrl()));
        ficaFacade.setPolicyClient(policyClient);
        ficaFacade.setCommerceClient(commerceRelationServiceClient);

        this.memberUtility.setPolicyClient(policyClient);
    }

    /**
     * Validate Fica. And validation fails. Kick start the BPM process...
     * Do not fail if exception just log and continue.
     *
     * @param customer
     * @param cmsClientNumber
     * @return
     */
    @Override
    public void validateFica(Customer customer, String cmsClientNumber, String agentId) {
        ficaFacade.validateFica(customer,cmsClientNumber,agentId);
    }


    @Override
    public Optional<Customer> applyVisaCard(String cmsClientNumber, Customer customer, boolean termsAndConditions, String agentId) throws CustomerNotFound {
        Customer commerceCustomer = commerceRelationServiceClient.findMemberUsingIdNumber(customer.getIdentificationDocument().getIdNumber(),
                customer.getIdentificationDocument().getType());

        if (hasCardsNotStopped(customer.getIdentificationDocument(), commerceCustomer)) {
            throw new MemberManagementException("Customer already has a Visa Card");
        }

        if (!commerceCustomer.getCustomerAccount(ProductCode.MULTIPLY_TRANSACTIONAL).isPresent()) {
            throw new MemberManagementException("Customer does not have a Multiply Money Account");
        }

        commerceCustomer = updateMemberDetails(commerceCustomer, customer);

        CardHolder newCard = CardHolder.fromCustomer(commerceCustomer);
        if (termsAndConditions) {
            newCard.setCardTermsAndConditionsAccepted(termsAndConditions);
            newCard.setCardTermsAndConditionsAcceptedDate(new Date());
        }

        commerceCustomer.getCustomerAccount(ProductCode.MULTIPLY_TRANSACTIONAL).get().addCardHolder(newCard);
        return createOrUpdateMultiplyMember(commerceCustomer, agentId);
    }

    private Customer updateMemberDetails(Customer existingCustomer, Customer customer) {
        existingCustomer.setInitials(customer.getInitials());
        existingCustomer.setName(customer.getName());
        existingCustomer.setTitle(customer.getTitle());
        existingCustomer.setSurname(customer.getSurname());
        existingCustomer.setMaidenName(customer.getMaidenName());
        existingCustomer.setGender(customer.getGender());
        existingCustomer.setDateOfBirth(customer.getDateOfBirth());
        existingCustomer.setEmail(customer.getEmail());
        existingCustomer.setIncomeTaxNumber(customer.getIncomeTaxNumber());
        existingCustomer.setMobilePhoneNumber(customer.getMobilePhoneNumber());
        existingCustomer.setPhysicalAddress(updateAddress(existingCustomer.getPhysicalAddress(),
                customer.getPhysicalAddress()));
        existingCustomer.setPostalAddress(updateAddress(existingCustomer.getPostalAddress(),
                customer.getPostalAddress()));
        return existingCustomer;
    }

    private Address updateAddress(Address existingAddress, Address changedAddress) {
        if (changedAddress != null) {
            if (existingAddress == null) {
                existingAddress = changedAddress;
            } else {
                existingAddress.setLine1(changedAddress.getLine1());
                existingAddress.setLine2(changedAddress.getLine2());
                existingAddress.setSuburb(changedAddress.getSuburb());
                existingAddress.setPostalCode(changedAddress.getPostalCode());
            }
        }
        return existingAddress;
    }

    private boolean hasCardsNotStopped(IdentificationDocument identificationDocument, Customer customer) {
        for (CustomerAccount customerAccount : customer.getCustomerAccounts()) {
            for (CardHolder cardHolder : customerAccount.getCardholders()) {
                if (cardHolder.isNotStopped() && cardHolder.getIdentificationDocument().equals(identificationDocument)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void checkEligibility(FootPrint footPrint, Customer customer) {
        ruleCheckerService.checkEligibleForMultiplyMoneyAccount(footPrint, customer);
    }


    /**
     * Apply for money management account
     *
     * @param cmsClientNumber
     * @param customer        Customer containing account and card holders
     * @param acceptedTerms
     * @param userId
     * @return Newly created Money Member reference number
     */
    @Override
    public ApplicationResult applyForNewAccount(String cmsClientNumber, Customer customer, boolean acceptedTerms, String userId)
            throws MemberManagementException, CustomerNotFound {

        ApplicationResult result = ApplicationResult.builder()
                .isValidationOnly(false)
                .build();

        FootPrint footPrint = getFootPrint(cmsClientNumber);
        String responseMessage;

        try {
            customer = addCustomerReferenceToCustomer(customer, footPrint);
            customer = addMultiplyAccountIfExistingMember(customer, acceptedTerms);

            validateApplication(customer);

            log.log(Level.FINER, "Checking eligibility for customer {0}", customer);
            if (!isEmpty(customer.getCustomerReference())) {
                responseMessage = SUCCESSFULLY_CREATED_NEW_ACCOUNT;
            } else {
                responseMessage = SUCCESSFULLY_CREATED_NEW_MEMBER;
            }

            Optional<Customer> customerReturned;

            ruleCheckerService.checkEligibleForMultiplyMoneyAccount(footPrint, customer);

            customer = applyAccountFeesFlag(customer, cmsClientNumber);

            customerReturned = createOrUpdateMultiplyMember(customer, userId);

            writeCdiFootprint(cmsClientNumber, customer, customerReturned, result);

            evaluateResults(cmsClientNumber, customer, userId, result, customerReturned);
        } catch (MemberManagementException e) {
            loggingService.logApplicationAttempt(cmsClientNumber, customer, result, userId);
            if (e.isCanStartBPMProcess()) {
                startBpmAccountOpeningExceptionProcess(cmsClientNumber,customer,e);
            }
            throw e;
        }

        if (FeatureFlags.SEND_TERMS_AND_CONDITIONS && result.getResultCode().equals("SUCCESS") && isNotEmpty(customer.getEmail())) {
            emailTermsAndConditions(customer);
        }

        result.setMessage(responseMessage);
        return result;
    }

    private void writeCdiFootprint(String cmsClientNumber, Customer customer, Optional<Customer> customerReturned, ApplicationResult result) {

        if (customerReturned.isPresent() && FeatureFlags.SAVE_CDI_FOOTPRINT) {
            customer.setCustomerReference(customerReturned.get().getCustomerReference());
            addBusinessTypeToCDI(cmsClientNumber, customer, result);
        }
    }

    private void validateApplication(Customer customer) throws MemberManagementException {
        try {
            log.log(Level.INFO, "Validating application for customer {0}", customer);
            customer.validate();
        } catch (BusinessException e) {
            throw new MemberManagementException(e.getMessage(),true);
        }
    }

    private Customer addCustomerReferenceToCustomer(Customer customer, FootPrint footPrint) {
        if (footPrint != null && !StringUtils.isEmpty(footPrint.getBusinessKey(BusinessKeyType.MONEY_CDI_KEY))) {
            customer.setCustomerReference(footPrint.getBusinessKey(BusinessKeyType.MONEY_CDI_KEY));
        } else {
            customer.setCustomerReference(null);
        }
        return customer;
    }

    protected MultiplyCustomer addMultiplyAccountIfExistingMember(Customer customer, boolean acceptedTerms) throws MemberManagementException, CustomerNotFound {

        MultiplyCustomer incomingCustomer = (MultiplyCustomer) customer;
        if (incomingCustomer.getCustomerReference() != null) {
            try {
                Customer existingCustomer = commerceRelationServiceClient.findMemberUsingMemberReference(incomingCustomer.getCustomerReference());

                MultiplyCustomer returnedCustomer = cloneCustomer(existingCustomer);
                returnedCustomer.setIncomeTaxNumber(incomingCustomer.getIncomeTaxNumber());

                CustomerAccountStatus accountStatus = ((existingCustomer.getFicaStatus() == FicaStatus.FULL_FICA ||
                        existingCustomer.getFicaStatus() == FicaStatus.ID_NUMBER_VERIFIED) && acceptedTerms)
                        ? CustomerAccountStatus.ACTIVE : CustomerAccountStatus.PENDING_ACTIVATION;

                addMultiplyMoneyAccount(returnedCustomer, accountStatus, acceptedTerms);

                return returnedCustomer;
            } catch (CustomerNotFound customerNotFound) {
                throw new CustomerNotFound("Customer not found for MONEY_CDI_KEY " + incomingCustomer.getCustomerReference());
            }
        }
        addMultiplyMoneyAccount(incomingCustomer, CustomerAccountStatus.PENDING_ACTIVATION, acceptedTerms);

        return incomingCustomer;
    }

    private void addMultiplyMoneyAccount(MultiplyCustomer returnedCustomer, CustomerAccountStatus accountStatus, boolean acceptedTerms) {
        boolean transactionExists = returnedCustomer.getCustomerAccount(MULTIPLY_TRANSACTIONAL).isPresent();
        boolean savingsExists = returnedCustomer.getCustomerAccount(MULTIPLY_SAVINGS).isPresent();
        if (!transactionExists) {
            returnedCustomer.addAccount(createMultiplyMoneyWallet(MULTIPLY_TRANSACTIONAL, TRANSACTION, accountStatus, acceptedTerms));
        }
        if (!savingsExists) {
            returnedCustomer.addAccount(createMultiplyMoneyWallet(MULTIPLY_SAVINGS, SAVINGS, accountStatus, acceptedTerms));
        }
    }

    private MultiplyCustomer cloneCustomer(Customer existingCustomer) {
        MultiplyCustomer existingMultiplyCustomer = new MultiplyCustomer();
        try {
            BeanUtils.copyProperties(existingMultiplyCustomer, existingCustomer);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.warning(e.getMessage());
        }
        return existingMultiplyCustomer;
    }

    private void emailTermsAndConditions(Customer customer) {
        try {
            String receiversName = customer.getTitle() + " " + customer.getInitials() + " " + customer.getSurname();
            SendEmailRequest sendEmailRequest = new SendEmailRequest();
            sendEmailRequest.setFromAddress("multiplyvisacard@multiply.co.za");
            sendEmailRequest.setFromName("Multiply Visa® Card");
            sendEmailRequest.setToAddress(customer.getEmail());
            sendEmailRequest.setSubject("Terms and conditions of use for the Multiply Visa ® Card");
            sendEmailRequest.setToName(receiversName);
            SendEmailAttachment termsAndConditions = new SendEmailAttachment();
            termsAndConditions.setBytes(loadFile("/TermsAndConditions.pdf"));
            termsAndConditions.setMimetype("application/pdf");
            termsAndConditions.setFilename("TermsAndConditions.pdf");
            sendEmailRequest.getAttachments().add(termsAndConditions);
            sendEmailRequest.setHtmlBody(TermsAndConditionsHelper.getEmailTermsAndConditions(receiversName));

            emailServiceClient.sendEmail(sendEmailRequest);
        } catch (SendEmailException e) {
            log.log(Level.SEVERE, "Failed to email Ts and Cs to customer with Id: {0}", customer.getIdentificationDocument().getIdNumber());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to email Ts and Cs: {0}", e);
        }
    }

    private byte[] loadFile(String path) throws IOException {
        byte[] bytes;
        try (InputStream is = this.getClass().getResourceAsStream(path)) {
            bytes = IOUtils.toByteArray(is);
        }
        return bytes;
    }

    @Override
    public String depositCashback(CashbackDeposit cashbackDeposit) throws CashDepositException, CustomerNotFound {
        Customer customer = commerceRelationServiceClient.findMemberUsingMemberReference(cashbackDeposit.getMemberReference());
        Optional<CustomerAccount> savingsAccount = customer.getCustomerAccount(MULTIPLY_SAVINGS);

        if (savingsAccount.isPresent()) {
            return depositIntoExistingSavingsAccount(cashbackDeposit, savingsAccount);
        } else {
            return depositIntoNewSavingsAccount(cashbackDeposit, customer);
        }
    }


    private String depositIntoExistingSavingsAccount(CashbackDeposit cashbackDeposit, Optional<CustomerAccount> savingsAccount)
            throws CashDepositException {
        if (!(savingsAccount.get().getStatus() == CustomerAccountStatus.ACTIVE
                || savingsAccount.get().getStatus() == CustomerAccountStatus.PENDING_ACTIVATION)) {
            throw new CashDepositException("Cashbacks can only be applied to accounts in account statuses ACTIVE and PENDING ACTIVATION");
        }
        return paymentServiceClient.cashDeposit(createDepositRequest(savingsAccount.get().getAccountReference(),
                cashbackDeposit.getAmount(), cashbackDeposit.getTransactionId(),
                cashbackDeposit.getDescription(), cashbackDeposit.getUserId()));
    }

    private DepositRequest createDepositRequest(String accountReference, long amount,
                                                String transactionId, String description, String userId) {
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAccountReference(accountReference);
        depositRequest.setAmount(amount);
        depositRequest.setDescription(description);
        depositRequest.setTransactionId(transactionId);
        depositRequest.setUserId(userId);

        return depositRequest;
    }

    private String depositIntoNewSavingsAccount(CashbackDeposit cashbackDeposit, Customer customer)
            throws MemberManagementException, CustomerNotFound, CashDepositException {
        FootPrint footprint = cdiEntityClient.getFootPrintWithBusinessKey(BusinessKeyType.MONEY_CDI_KEY,
                cashbackDeposit.getMemberReference());

        String cmsClientNumber = footprint.getBusinessKey(BusinessKeyType.CMS_CDI_KEY);

        if (cmsClientNumber == null) {
            throw new MemberManagementException(String.format("Footprint not found for member reference: %s",
                    cashbackDeposit.getMemberReference()));
        }

        ApplicationResult applicationResult = applyForNewAccount(cmsClientNumber, customer, TERMS_NOT_ACCEPTED,
                cashbackDeposit.getUserId());

        if ("SUCCESS".equals(applicationResult.getResultCode())) {
            return depositCashback(cashbackDeposit);
        } else {
            throw new MemberManagementException(applicationResult.getFullErrorMessage());
        }
    }

    private MultiplyMoneyAccount createMultiplyMoneyWallet(ProductCode productCode,
                                                           AccountTypeCode accountTypeCode,
                                                           CustomerAccountStatus status, boolean acceptedTerms) {
        MultiplyMoneyAccount customerAccount = new MultiplyMoneyAccount();
        customerAccount.setIssuingProductCode(productCode);
        customerAccount.setAccountTypeCode(accountTypeCode);
        customerAccount.setAccountTermsConditionsAccepted(acceptedTerms);
        customerAccount.setAccountTermsConditionsAcceptedDateTime(new Date());
        customerAccount.setStatus(status);

        return customerAccount;
    }

    @Override
    public ApplicationResult autoApply(String cmsClientNumber, Customer customer, boolean termsAndConditions, String agentId) throws CustomerNotFound {
        ApplicationResult result = null;

        String customerReference = getCustomerReferenceFor(cmsClientNumber);

        customer = updateOrCreateCustomerFromPolicy(cmsClientNumber,customer);

        if (StringUtils.isEmpty(customerReference)) {
            result = applyForLightWeightAccount(cmsClientNumber, customer, termsAndConditions, agentId);
            customer.setCustomerReference(result.getApplicationId());
        } else {
            result = applyForNewAccount(cmsClientNumber, customer, termsAndConditions, agentId);
        }
        validateFica(customer, cmsClientNumber, agentId);

        return result;
    }

    private Customer updateOrCreateCustomerFromPolicy(String cmsClientNumber,Customer customer) throws MemberManagementException {
        Customer policyHolder = null;

        try {
            policyHolder = this.memberUtility.getCustomer(cmsClientNumber);
        }
        catch(MemberManagementException e) {
            log.log(Level.WARNING,"Could not retrieve policy details",e);

            if (customer==null) {
                //This means, we cannot continue. No customer details provided from event queue, and could not
                //create from policy
                throw e;
            }
            else {
                return customer;
            }
        }

        if (customer!=null) {
            if (StringUtils.isNotBlank(customer.getName())) {
                  policyHolder.setName(customer.getName());
            }
            if (StringUtils.isNotBlank(customer.getSurname())) {
                policyHolder.setSurname(customer.getSurname());
            }
            if (StringUtils.isNotBlank(customer.getInitials())) {
                policyHolder.setInitials(customer.getInitials());
            }
            if (StringUtils.isNotBlank(customer.getTitle())) {
                policyHolder.setTitle(customer.getTitle());
            }
            if (StringUtils.isNotBlank(customer.getMobilePhoneNumber())) {
                policyHolder.setMobilePhoneNumber(
                        SouthAfricanMobileFormatter.addSouthAfricanCodeToPhoneNumber(customer.getMobilePhoneNumber()));
            }
            if (StringUtils.isNotBlank(customer.getEmail())) {
                policyHolder.setEmail(customer.getEmail());
            }
            if (customer.getDateOfBirth()!=null) {
                policyHolder.setDateOfBirth(customer.getDateOfBirth());
            }
            if (customer.getIdentificationDocument()!=null) {
                policyHolder.setIdentificationDocument(customer.getIdentificationDocument());
            }
            if (customer.getGender()!=null) {
                policyHolder.setGender(customer.getGender());
            }
            if (StringUtils.isNotBlank(customer.getIncomeTaxNumber())) {
                policyHolder.setIncomeTaxNumber(customer.getIncomeTaxNumber());
            }
        }

        return policyHolder;
    }

    private String getCustomerReferenceFor(String cmsClientNumber) {
        FootPrint footPrintWithCms = getFootPrint(cmsClientNumber);
        String customerReference = null;
        if (footPrintWithCms != null) {
            customerReference = footPrintWithCms.getBusinessKey(BusinessKeyType.MONEY_CDI_KEY);
        }
        return customerReference;
    }

    public ApplicationResult applyForLightWeightAccount(String cmsClientNumber, Customer customer, boolean acceptedTerms, String userId)
            throws CustomerNotFound {
        ApplicationResult result = ApplicationResult.builder()
                .isValidationOnly(false)
                .build();
        try {
            validateIdNumber(customer);

            FootPrint footPrint = getFootPrint(cmsClientNumber);

            customer = addCustomerReferenceToCustomer(customer, footPrint);
            customer = addMultiplyAccountIfExistingMember(customer, acceptedTerms);

            defaultMissingCustomerDetails(customer);
            validateApplication(customer);

            ruleCheckerService.checkEligibleForLightWeightMultiplyMoneyAccount(footPrint, customer);
            customer = applyAccountFeesFlag(customer, cmsClientNumber);
            Optional<Customer> customerReturned = createOrUpdateMultiplyMember(customer, userId);

            writeCdiFootprint(cmsClientNumber, customer, customerReturned, result);

            evaluateResults(cmsClientNumber, customer, userId, result, customerReturned);

        } catch (MemberManagementException e) {
            loggingService.logApplicationAttempt(cmsClientNumber, customer, result, userId);
            if (e.isCanStartBPMProcess()) {
                startBpmAccountOpeningExceptionProcess(cmsClientNumber,customer,e);
            }

            throw e;
        }

        return result;
    }

    private void validateIdNumber(Customer customer) {
        String idNumber = customer.getIdentificationDocument().getIdNumber();
        if (!validateCheckDigit(idNumber)) {
            throw new MemberManagementException(String.format("Invalid South African ID received: %s", idNumber),true);
        }
    }

    protected void defaultMissingCustomerDetails(Customer customer) {
        try {
            for (String fieldName : customer.mandatoryFieldsNotPopulated()) {
                Method method = findMethodOnCustomer(setterFromField(fieldName));
                if (method != null) {
                    setDefaultValue(customer, method);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to default mandatory field: " + e.getMessage());
        }
    }

    protected Date extractDateOfBirth(String rsaIdNumber) {
        if (!validateCheckDigit(rsaIdNumber)) {
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd");
        try {
            return dateFormat.parse(rsaIdNumber.substring(0, 6));
        } catch (ParseException e) {
        }
        return null;
    }

    protected Gender extractGender(String rsaIdNumber) {
        if (!validateCheckDigit(rsaIdNumber)) {
            return null;
        }
        return valueOf(rsaIdNumber.charAt(6)) > 4 ? Gender.MALE : Gender.FEMALE;
    }

    protected String getDefaultTitle(String rsaIdNumber) {
        if (extractGender(rsaIdNumber) == Gender.MALE) {
            return "MR";
        }
        return "MRS";
    }

    protected Method findMethodOnCustomer(String methodName) {
        List<Method> methodList = new ArrayList<>();
        methodList.addAll(Arrays.asList(MultiplyCustomer.class.getMethods()));
        methodList.addAll(Arrays.asList(Customer.class.getMethods()));
        methodList.addAll(Arrays.asList(Person.class.getMethods()));
        for (Method method : methodList) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    protected void setDefaultValue(Customer customer, Method method) throws IllegalAccessException, InvocationTargetException {
        if (method.getParameterTypes().length > 0
                && method.getParameterTypes()[0] == String.class) {
            if (method.getName().equals("setTitle")) {
                customer.setTitle(getDefaultTitle(customer.getIdentificationDocument().getIdNumber()));
            } else if (method.getName().equals("setMobilePhoneNumber")) {
                //DO nothing...
            } else {
                method.invoke(customer, "DEFAULT999");
            }
        } else if (method.getParameterTypes().length > 0
                && method.getParameterTypes()[0] == int.class
                && method.getParameterTypes()[0] == long.class
                && method.getParameterTypes()[0] == Integer.class
                && method.getParameterTypes()[0] == Long.class) {
            method.invoke(customer, method.getParameterTypes()[0].cast(99999));
        } else if (method.getParameterTypes().length > 0
                && method.getParameterTypes()[0] == Date.class) {
            method.invoke(customer, extractDateOfBirth(customer.getIdentificationDocument().getIdNumber()));
        } else if (method.getParameterTypes().length > 0
                && method.getParameterTypes()[0] == Gender.class) {
            method.invoke(customer, extractGender(customer.getIdentificationDocument().getIdNumber()));
        } else if (method.getParameterTypes().length > 0
                && method.getParameterTypes()[0] == Address.class) {
            method.invoke(customer, defaultAddress());
        }
    }

    protected String setterFromField(String fieldName) {
        return String.format("set%s%s", String.valueOf(fieldName.charAt(0)).toUpperCase(), fieldName.substring(1));
    }

    protected Address defaultAddress() {
        Address address = new Address();
        address.setLine1("DEFAULT999");
        address.setSuburb("DEFAULT999");
        address.setPostalCode("9999");
        return address;
    }

    protected FootPrint getFootPrint(String cmsClientNumber) {
        if (!isEmpty(cmsClientNumber)) {
            return cdiEntityClient.getFootPrintWithBusinessKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        }

        return null;
    }

    protected void evaluateResults(String cmsClientNumber, Customer customer, String userId,
                                   ApplicationResult result, Optional<Customer> customerReturned) {
        if (!customerReturned.isPresent()) {
            result.setMessage(result.getFullErrorMessage());
            loggingService.logApplicationAttempt(cmsClientNumber, customer, result, userId);
            throw new RuntimeException(String.format("Failed to create member: %s", result.getFullErrorMessage()));
        }

        if (customerReturned.isPresent()) {
            result.setApplicationId(customerReturned.get().getCustomerReference());
        }
        loggingService.logApplicationAttempt(cmsClientNumber, customer, result, userId);
    }

    private void addBusinessTypeToCDI(String clientNumber, Customer customer, ApplicationResult result) throws MemberManagementException {
        try {
            if (customer.getCustomerAccounts() == null || customer.getCustomerAccounts().size() > 2) {
                log.log(Level.WARNING, "No customer account information for customer {0}", customer);
                return;
            }

            addNaturalParty(clientNumber, customer, BusinessKeyType.MONEY_CDI_KEY, customer.getCustomerReference());
        } catch (Exception e) {
            result.setMessage(e.getMessage());
            Throwable rootCause = getRootCause(e);
            throw new MemberManagementException(rootCause == null ? e.getMessage(): rootCause.getMessage());
        }
    }

    private void addNaturalParty(String clientNumber, Customer customer, BusinessKeyType businessKeyType, String keyValue) throws AddPartyWebserviceException {
        NaturalEntity entity = createNaturalEntityForCustomer(clientNumber, customer);
        cdiEntityClient.addOrUpdateBusinessKey(entity, businessKeyType, keyValue);
    }

    private NaturalEntity createNaturalEntityForCustomer(String clientNumber, Customer customer) {
        NaturalEntity entity = new NaturalEntity();

        entity.setTitle(customer.getTitle());
        entity.setSurname(customer.getSurname());
        entity.setInitials(customer.getInitials());
        entity.setNames(new String[]{customer.getName()});
        entity.setClientNumber(Long.valueOf(clientNumber));
        IdentificationDocument id = customer.getIdentificationDocument();

        if (id.getType().equals(IdentificationDocument.Type.NATIONAL_ID)) {
            entity.setTypeOfId(RSA);
            entity.setIdNumber(id.getIdNumber());
        } else {
            entity.setPassportNumber(id.getIdNumber());
            entity.setPassportCountryOfIssue(id.getCountryIssued().getIsoCode());
        }

        if (Gender.MALE.equals(customer.getGender())) {
            entity.setGender(com.mmiholdings.multiply.service.entity.Gender.male);
        } else {
            entity.setGender(com.mmiholdings.multiply.service.entity.Gender.female);
        }

        if (customer.getDateOfBirth() != null) {
            DateTime jodaTime = new DateTime(customer.getDateOfBirth());
            entity.setDateOfBirth(new GregorianCalendar(jodaTime.getYear(), jodaTime.getMonthOfYear() - 1, jodaTime.getDayOfMonth()));
        }
        return entity;
    }

    /**
     * Set a flag to apply fees on accounts based on these rule (assumes default as false):
     * - A customer that has a Health policy and a Multiply Starter policy with a card linked to the HSA must be
     * charged a monthly fee of R10 against the linked HSA.
     * - An account fee should be only be charged when the HSA account is in status 00-Active
     * - A customer that has a Health policy and a Multiply Provider or Premier policy with a card linked to
     * the HSA will not be charged a monthly fee.
     *
     * @param customer
     * @param cmsClientNumber
     */
    protected Customer applyAccountFeesFlag(Customer customer, String cmsClientNumber) {
        if (customer.getCustomerAccount(HEALTH).isPresent()) {
            Policy multiplyPolicy = policyClient.getPolicy(Long.valueOf(cmsClientNumber));
            boolean feeChargeable = PlanType.MULTIPLY_STARTER.equals(multiplyPolicy.getPlan().getPlanType().getType());

            CustomerAccount account = customer.getCustomerAccount(HEALTH).get();
            account.setFeeChargeable(feeChargeable);
        }
        return customer;
    }

    private void startBpmAccountOpeningExceptionProcess(String cmsClientNumber,Customer customer,MemberManagementException exception) {
        if (!FeatureFlags.BPM_ACCOUNT_CREATION_FAILURE) {
            return;
        }

        try {
            log.info(String.format("An error occurred while creating an account. Creating bpm account creation process for customer with cms %s",cmsClientNumber));

            String message = exception.getMessage();

            if (exception.getCause() instanceof CommerceException) {

                CommerceException commerceException = (CommerceException) exception.getCause();

                StringBuilder commerceExceptions = formatCommerceException(customer, commerceException.getErrors());

                if (commerceExceptions.length()==0) {
                    log.warning("Cannot start Commerce process for the following commerce exception: "+message);
                    return;
                }
                else {
                    message = commerceExceptions.toString();
                }
            }

            Policy policy =
                    this.policyClient.getPolicy(Long.valueOf(cmsClientNumber));

            if (policy!=null && policy.getPolicyNumber()!=null) {
                String policyNumber = String.format("%s%d",policy.getPolicyNumber().getProductCode(),policy.getPolicyNumber().getNumber());
                String idNumber = policy.getPolicyHolder().getIdNumber();
                this.bpmAccountFacade.startBPMProcess(policyNumber,idNumber,cmsClientNumber,"Exception",message);
                log.info("The BPM account creation process was successful.");
            }
            else {
                log.log(Level.SEVERE,"Policy not found. Cannot start BPM process.");
            }
        }
        catch(Exception e) {
            log.log(Level.SEVERE,"Could not start BPM account creation process",e);
        }
    }

    private StringBuilder formatCommerceException(Customer customer, Map<String,String> errors) {
        StringBuilder commerceExceptions = new StringBuilder();

        if (errors!=null && errors.size()>0) {

            final String textToRemove = "com.mmiholdings.service.money.commerce.member.CommerceException: ";

            String mobileError = errors.get(MOBILE_ERROR_CODE);
            String emailError = errors.get(EMAIL_ERROR_CODE);
            String idNumberError = errors.get(ID_NUMBER_ERROR_CODE);

            if (StringUtils.isNotBlank(mobileError)) {
                commerceExceptions.append(mobileError.replaceAll(textToRemove,"")).append(" : ").append(customer.getMobilePhoneNumber());
            }
            if (StringUtils.isNotBlank(emailError)) {
                commerceExceptions.append(emailError.replaceAll(textToRemove,"")).append(" : ").append(customer.getEmail());
            }
            if (StringUtils.isNotBlank(idNumberError)) {
                commerceExceptions.append(idNumberError.replaceAll(textToRemove,"")).append(" : ").append(customer.getIdentificationDocument().getIdNumber());
            }
        }

        return commerceExceptions;
    }

    private Optional<Customer> createOrUpdateMultiplyMember(Customer customer, String agentId) throws MemberManagementException {
        try {
            log.log(Level.INFO, "Creating new account in Traderoot for [{0}]... by {1}", new Object[]{customer, agentId});
            MemberUpdateRequest memberUpdateRequest = new MemberUpdateRequest();
            memberUpdateRequest.setCustomer(customer);
            memberUpdateRequest.setUserId(agentId);
            String memberReference = commerceRelationServiceClient.createOrUpdateMember(memberUpdateRequest);

            Customer customerReturned = commerceRelationServiceClient.findMemberUsingMemberReference(memberReference);
            log.log(Level.INFO, "Created new account with reference [{0}]", customerReturned.getCustomerReference());
            return Optional.of(customerReturned);
        } catch (CommerceException e) {
            Throwable rootCause = getRootCause(e);
            String errorMessage = e.getMessage();
            log.log(Level.WARNING, "CommerceException: Creating new account failed: {0}", e.getMessage());
            MemberManagementException memberManagementException = new MemberManagementException(e,"Create Or Update Error");
            memberManagementException.setCanStartBPMProcess(true);
            throw memberManagementException;
        } catch (BusinessException e) {
            log.log(Level.WARNING, "BusinessException: Creating new account failed: {0}", e.getMessage());
            throw new MemberManagementException(e.getMessage());
        }
    }

	@Override
	public void linkCard(LinkCardRequest linkCardRequest) throws CardLinkingException {
		final String userId = linkCardRequest.getUserId();
		final String accountReferenceToLinkCardTo = linkCardRequest.getAccountReferenceToLinkCardTo();
		final String accountReferenceCardIsLinkedTo = linkCardRequest.getAccountReferenceCardIsLinkedTo();
		final String cardReference = linkCardRequest.getCardReference();

		Customer customerToLinkTo = findCustomer(accountReferenceToLinkCardTo, userId);
		CustomerAccount accountToLinkCardTo = findAccountToLinkCardTo(accountReferenceToLinkCardTo, customerToLinkTo.getCustomerAccounts());
		Customer customerWithAccountCurrentlyLinkedToCard = findCustomer(accountReferenceCardIsLinkedTo, userId);
		CardHolder cardHolder = getCardHolderFromCustomer(customerWithAccountCurrentlyLinkedToCard, cardReference);
		if (ProductCode.MULTIPLY_TRANSACTIONAL.equals(accountToLinkCardTo.getIssuingProductCode()) && isCardHolderCustomer(cardHolder, customerToLinkTo)){
			throw new CardLinkingException("Customer with account["+accountReferenceToLinkCardTo+"] does not match card holder of card reference["+cardReference+"]");
		}
		verifyEligibiltyOfCardToAccountLinking(cardHolder, accountToLinkCardTo);
		commerceRelationServiceClient.linkCard(cardReference, accountReferenceToLinkCardTo, userId);
	}

	private boolean isCardHolderCustomer(CardHolder cardHolder, Customer customerToLinkTo) throws CardLinkingException {
		IdentificationDocument customerIdDoc = customerToLinkTo.getIdentificationDocument();
		IdentificationDocument cardHolderIdDoc = cardHolder.getIdentificationDocument();
		if (customerIdDoc == null) {
			throw new CardLinkingException("Invalid Identification Document for customer with member reference ["
					+ customerToLinkTo.getCustomerReference() + "]");
		}
		if (cardHolderIdDoc == null) {
			throw new CardLinkingException("Invalid Identification Document for cardholder with card reference ["
					+ cardHolder.getCardReference() + "]");
		}
		return !StringUtils.equals(customerIdDoc.getIdNumber(),cardHolderIdDoc.getIdNumber());
	}

	private CardHolder getCardHolderFromCustomer(Customer customerWithAccountLinkedToCard, String cardReference) throws CardLinkingException {
		Optional<CardHolder> cardHolderOptional = new CardFilter(customerWithAccountLinkedToCard).filterCardReferenceOptional(cardReference);
		if (!cardHolderOptional.isPresent()) {
			throw new CardLinkingException("Card holder not found for card reference [" + cardReference + "]");
		}
		return cardHolderOptional.get();
	}

	private CustomerAccount findAccountToLinkCardTo(String accountReferenceToLinkCardTo, List<CustomerAccount> customerAccounts) throws CardLinkingException {
		return customerAccounts.stream()
				.filter(customerAccount -> accountReferenceToLinkCardTo.equals(customerAccount.getAccountReference()))
				.findFirst().get();
	}

	private void verifyEligibiltyOfCardToAccountLinking(CardHolder cardHolder, CustomerAccount customerAccountToLinkTo) throws CardLinkingException {
		String cardHolderIdNumber = cardHolder.getIdentificationDocument().getIdNumber();
		if (ProductCode.HEALTH.equals(customerAccountToLinkTo.getIssuingProductCode())) {
			HealthPolicy healthPolicy = getHealthPolicyFor(customerAccountToLinkTo.getAccountReference());
			verifyCardHolderIsHealthBeneficiary(cardHolderIdNumber, healthPolicy.getBeneficiaries());
		} else if (ProductCode.MULTIPLY_SAVINGS.equals(customerAccountToLinkTo.getIssuingProductCode())) {
			throw new CardLinkingException("Not allowed to link card to Savings Payment Wallet");
		}
		if (!cardHolder.isNotStopped()) {
			throw new CardLinkingException("Card requested for linking must not be stopped");
		}
	}

	private void verifyCardHolderIsHealthBeneficiary(String cardHolderIdNumber, List<HealthBeneficiary> beneficiaries) throws CardLinkingException {
		beneficiaries.stream()
				.filter(healthBeneficiary1 -> cardHolderIdNumber.equals(healthBeneficiary1.getIdNumber()))
				.findFirst().orElseThrow(() -> new CardLinkingException("Cardholder with Id [" + cardHolderIdNumber + "] not a dependent on health policy"));
	}

	private HealthPolicy getHealthPolicyFor(String accountReferenceToLinkTo) throws CardLinkingException {
		try {
			return healthService.getHealthMemberDetails(Long.parseLong(accountReferenceToLinkTo));
		} catch (HealthPolicyNotFoundException hpnfe) {
			log.log(Level.SEVERE, "No health policy found for health policy number [" + accountReferenceToLinkTo + "]", hpnfe);
			throw new CardLinkingException("Health policy with number [" + accountReferenceToLinkTo + "] not found");
		}
	}

	private Customer findCustomer(String accountReference, String userId) throws CardLinkingException {
		try {
			return commerceRelationServiceClient.findMemberUsingAccountReference(accountReference, userId);
		} catch (CustomerNotFound cnf) {
			log.log(Level.SEVERE, "Customer with account reference [" + accountReference + "]", cnf);
			throw new CardLinkingException("Customer with account reference [" + accountReference + "] not found");
		}
	}
}
