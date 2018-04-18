package com.mmiholdings.member.money.api.rule;

import javax.ejb.ApplicationException;

@ApplicationException
public class NotEligibleMultiplyNotActive extends NotEligibleException {
	public NotEligibleMultiplyNotActive(String message) {
		super(message, ErrorCode.INACTIVE_POLICY);
	}
}
