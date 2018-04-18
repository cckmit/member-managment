package com.mmiholdings.member.money.service;

import com.mmiholdings.service.money.commerce.member.AccountTypeCode;
import com.mmiholdings.service.money.commerce.member.Address;
import com.mmiholdings.service.money.commerce.member.Country;
import com.mmiholdings.service.money.commerce.member.CustomerAccount;
import com.mmiholdings.service.money.commerce.member.CustomerAccountStatus;
import com.mmiholdings.service.money.commerce.member.Gender;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import com.mmiholdings.service.money.commerce.member.Language;
import com.mmiholdings.service.money.commerce.member.LegalStatus;
import com.mmiholdings.service.money.commerce.member.MultiplyCustomer;
import com.mmiholdings.service.money.commerce.member.ProductCode;
import com.mmiholdings.service.money.util.DateUtil;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by pieter on 2016/10/24.
 */
public class MultiplyCustomerTestConstants {

	protected static final Date DATE_OF_BIRTH = DateUtil.parse("1977-06-02");

	public static final String TITLE = "MR";
	public static final String NAME = "John";
	public static final String SURNAME = "Doe";
	public static final String ID_NUMBER = "7706020034083";
	public static final Country COUNTRY_ISSUED_CODE = Country.DE;
	public static final String EMAIL = "john.d@gm.com";
	public static final String PHYSICAL_LINE_1 = "PHYSICAL_LINE1";
	public static final String PHYSICAL_LINE_2 = "PHYSICAL_LINE_2";
	public static final String SUBURB = "MENLYN";
	public static final String CITY = "PRETORIA";
	public static final String POSTAL_CITY = "PRETORIA";
	public static final String COUNTRY = "RSA";
	public static final String POSTAL_COUNTRY = "SOUTH AFRICA";
	public static final String POSTAL_CODE = "0121";
	public static final String PHYSICAL_CODE = "0120";
	public static final String REGION = "GAUTENG";
	public static final String POSTAL_REGION = "GAUTENG";
	public static final String POSTAL_LINE_1 = "PO BOX 12345";
	public static final String POSTAL_LINE_2 = "WB POST OFFICE";
	public static final String POSTAL_SUBURB = "MENLYN EXT1";
	public static final String NATURAL_PERSON = "00";
	public static final String HOME_NUMBER = "+27123403483";
	public static final String WORK_NUMBER = "+2712828732";
	public static final String MOBILE_NUMBER = "+27821928239";
	public static final String BLANK_VALUE = "";
	public static final String ACCOUNT_ALIAS = "Health Saver";
	public static final String ACCOUNT_REFERENCE = "19283729801";
	public static final String BROKER_CODE = "BROKER_CODE";
	public static final String PRODUCT_CODE = "PRODUCT_CODE";
	public static final String CARD_NUMBER = "1239878732982";
	public static final String CARD_REFERENCE = "REFERENCE123456";
	public static final BigDecimal MEMBER_ID = new BigDecimal("1923879187289782");
	public static final String POSTAL_CODE2 = "0182";
	public static final String INITIALS = "JKL";
	public static final String INCOME_TAX_NUMBER = "1785415145";
	public static final String INVALID_ID = "7202125007082";
	public static final Language LANGUAGE = Language.ENGLISH;
	public static final String FICA_STATUS = "00";
	public static final String CARD_SERIAL = "592000000010";
	public static final String LAST_4_DIGITS = "9901";
	public static final String ADDITIONAL_CARD_INFO = "MVCeaf76a6767s-6576afe889";

	protected static final Date CARD_TERMS_ACCEPTED_DATE = DateUtil.parse("2017-04-27");
	protected static final Date FACULTY_DATE = DateUtil.parse("2016-01-01");
	protected static final Date ID_ISSUE_DATE = DateUtil.parse("2013-01-01");
	protected static final Date ID_EXPIRATION_DATE = DateUtil.parse("2017-06-01");

	private static final String CUSTOMER_REFERENCE = "1273872";
	public static final String ACCOUNT_NUMBER = "843923085714";

	private MultiplyCustomerTestConstants() {
	}

	public static MultiplyCustomer createCustomer() {
		CustomerAccount customerAccount = createCustomerAccount();

		IdentificationDocument identificationDocument = IdentificationDocument.createSouthAfricanId(ID_NUMBER);
		MultiplyCustomer customer = new MultiplyCustomer();
		customer.setCustomerReference(CUSTOMER_REFERENCE);
		customer.setTitle(TITLE);
		customer.setInitials(INITIALS);
		customer.setName(NAME);
		customer.setSurname(SURNAME);
		customer.setIdentificationDocument(identificationDocument);
		customer.setDateOfBirth(DATE_OF_BIRTH);
		customer.setEmail(EMAIL);
		customer.setLanguage(Language.NORTHERN_SOTHO);
		customer.setGender(Gender.FEMALE);
		customer.setLegalStatus(LegalStatus.MARRIED);
		customer.setPhysicalAddress(Address.builder()
				.line1(PHYSICAL_LINE_1)
				.line2(PHYSICAL_LINE_2)
				.suburb(SUBURB)
				.postalCode(PHYSICAL_CODE)
				.build());
		customer.setPostalAddress(Address.builder()
				.line1(POSTAL_LINE_1)
				.line2(POSTAL_LINE_2)
				.suburb(POSTAL_SUBURB)
				.postalCode(POSTAL_CODE)
				.build());
		customer.setHomePhoneNumber(HOME_NUMBER);
		customer.setWorkPhoneNumber(WORK_NUMBER);
		customer.setMobilePhoneNumber(MOBILE_NUMBER);
		customer.getCustomerAccounts().add(customerAccount);

		return customer;
	}

	/**
	 * Create default Health Savings Account
	 *
	 * @return
	 */
	public static CustomerAccount createCustomerAccount() {
		return CustomerAccount.builder().accountAlias(ACCOUNT_ALIAS).
				accountReference(ACCOUNT_REFERENCE)
				.accountTypeCode(AccountTypeCode.SAVINGS)
				.status(CustomerAccountStatus.PENDING_ACTIVATION)
				.brokerCode(BROKER_CODE).issuingProductCode(ProductCode.MULTIPLY_SAVINGS).facilityDate(FACULTY_DATE).build();
	}
}
