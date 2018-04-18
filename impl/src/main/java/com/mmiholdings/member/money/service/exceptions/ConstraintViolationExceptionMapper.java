package com.mmiholdings.member.money.service.exceptions;

import com.mmiholdings.member.money.api.ConstraintViolationException;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Log
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = ExceptionUtils.getRootCauseMessage(exception);
        }

        log.info(String.format("%s: %s", ConstraintViolationException.class.getSimpleName(), message));
        return Response.status(Response.Status.CONFLICT).header("Reason", message).build();
    }
}
