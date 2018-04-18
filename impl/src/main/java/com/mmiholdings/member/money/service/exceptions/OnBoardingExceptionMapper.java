package com.mmiholdings.member.money.service.exceptions;

import com.mmiholdings.member.money.api.MemberManagementException;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Log
@Provider
public class OnBoardingExceptionMapper implements ExceptionMapper<MemberManagementException> {

    @Override
    public Response toResponse(MemberManagementException exception) {
        String message = ExceptionUtils.getRootCauseMessage(exception);
        if (message == null) {
            exception.getMessage();
        }

        log.info(String.format("%s: %s", MemberManagementException.class.getSimpleName(), message));
        return Response.status(Response.Status.PRECONDITION_FAILED).entity(message).header("Reason", message).build();
    }
}
