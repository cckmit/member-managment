package com.mmiholdings.member.money.api.rule;

import javax.ejb.ApplicationException;

@ApplicationException
public class NotEligibleMultiplyException extends NotEligibleException {

    public NotEligibleMultiplyException(String message) {
        super(message);
    }
    //TODO: TERRIBLE!!! Use Bean Validation to solve this
    public NotEligibleMultiplyException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
