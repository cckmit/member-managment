package com.mmiholdings.member.money.api.rule;


import com.mmiholdings.member.money.api.MemberManagementException;
import lombok.Getter;

import javax.ejb.ApplicationException;

@Getter
@ApplicationException
public class NotEligibleException extends MemberManagementException {

    protected ErrorCode errorCode;

    public NotEligibleException(String message) {
        super(message);
    }

    public NotEligibleException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
