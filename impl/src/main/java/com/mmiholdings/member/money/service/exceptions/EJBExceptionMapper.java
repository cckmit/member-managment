package com.mmiholdings.member.money.service.exceptions;

import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.member.money.api.rule.NotEligibleException;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Log
@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBTransactionRolledbackException> {

    @Override
    public Response toResponse(EJBTransactionRolledbackException exception) {
        Throwable cause = exception.getCause();

        if (cause instanceof NotEligibleException) {
            NotEligibleException rootCause = (NotEligibleException) cause;
            String message =  ExceptionUtils.getRootCauseMessage(exception);

            if (message == null) {
                message = exception.getMessage();
            }
            log.info(String.format("%s: %s", MemberManagementException.class.getSimpleName(), message));
            return Response.status(Response.Status.PRECONDITION_FAILED).header("Reason", message).header("ErrorCode", rootCause.getErrorCode()).build();
        } else {
            String message = exception.getMessage();

            if (message == null) {
                message = ExceptionUtils.getRootCauseMessage(exception);

            }

            log.info(String.format("%s: %s", EJBException.class.getSimpleName(), message));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(message).header("Reason", message).build();
        }
    }

}
