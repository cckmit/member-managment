package com.mmiholdings.member.money.service.exceptions;

import com.mmiholdings.member.money.api.rule.CardLinkingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class CardLinkingExceptionMapper implements ExceptionMapper<CardLinkingException> {
    @Override
    public Response toResponse(CardLinkingException cle) {
        return Response.status(Response.Status.BAD_REQUEST).header("Reason", cle.getMessage()).build();
    }
}
