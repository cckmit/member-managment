package com.mmiholdings.member.money.service.exceptions;

import com.mmiholdings.service.money.commerce.member.CustomerNotFound;
import com.mmiholdings.service.money.commerce.payment.CashDepositException;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Log
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<CustomerNotFound> {

    @Override
    public Response toResponse(CustomerNotFound exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = ExceptionUtils.getRootCauseMessage(exception);
        }

        log.info(String.format("%s: %s", CustomerNotFound.class.getSimpleName(), message));
        return Response.status(Response.Status.NOT_FOUND).header("Reason", message).build();
    }
}
