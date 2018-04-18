package com.mmiholdings.member.money.api.rule;

public enum ErrorCode {
	//TODO: TERRIBLE!!! Use Bean Validation to solve this
	EXISTING_ACCOUNT,
	UNDERAGE_CUSTOMER,
	INACTIVE_POLICY,
	UNKNOWN_MEMBER
}
