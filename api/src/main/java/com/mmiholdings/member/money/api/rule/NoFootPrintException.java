package com.mmiholdings.member.money.api.rule;


import com.mmiholdings.member.money.api.MemberManagementException;

import javax.ejb.ApplicationException;

@ApplicationException
public class NoFootPrintException extends MemberManagementException {
    public NoFootPrintException(String message) {
        super(message);
    }
}
