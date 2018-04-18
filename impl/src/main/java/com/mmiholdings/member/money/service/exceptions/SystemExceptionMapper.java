package com.mmiholdings.member.money.service.exceptions;

import com.mmiholdings.member.money.api.MemberManagementException;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Log
public class SystemExceptionMapper implements ExceptionMapper<Exception> {

    public Response toResponse(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = ExceptionUtils.getStackTrace(exception);
        }

        log.info(String.format("%s: %s", MemberManagementException.class.getSimpleName(), message));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header("Reason", message).build();
    }
}