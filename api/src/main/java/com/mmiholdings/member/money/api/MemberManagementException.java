package com.mmiholdings.member.money.api;

import lombok.Getter;
import lombok.Setter;

import javax.ejb.ApplicationException;

/**
 * Created by pieter on 2017/03/15.
 */
@Getter
@Setter
@ApplicationException
public class MemberManagementException extends RuntimeException {

    protected String trigger;

    protected boolean canStartBPMProcess = false;

    public MemberManagementException(String message) {
        super(message);
    }

    public MemberManagementException(Throwable cause, String trigger) {
        super(cause);
        this.trigger = trigger;
    }

    public MemberManagementException(String message,boolean canStartBPMProcess) {
        super(message);
        this.canStartBPMProcess = canStartBPMProcess;
    }
}
