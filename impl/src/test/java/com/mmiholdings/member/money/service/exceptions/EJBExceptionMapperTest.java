package com.mmiholdings.member.money.service.exceptions;

import com.google.common.collect.Lists;
import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.member.money.api.rule.ErrorCode;
import com.mmiholdings.member.money.api.rule.NotEligibleMultiplyException;
import com.mmiholdings.member.money.api.rule.NotEligibleMultiplyNotActive;
import org.junit.Ignore;
import org.junit.Test;

import javax.ejb.EJBTransactionRolledbackException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EJBExceptionMapperTest {

    @Test
    @Ignore
    public void toResponse_shouldSetErrorCode_whenNotEligibleMultiplyNotActive() {
        EJBExceptionMapper mapper = new EJBExceptionMapper();

        String message = "NotEligibleMultiplyException: Member not active";
        Response response = mapper.toResponse(new EJBTransactionRolledbackException(message,new NotEligibleMultiplyNotActive(message)));

        assertNotNull(response);
        MultivaluedMap<String, Object> metadata = response.getMetadata();
        assertEquals(Lists.newArrayList( message),
                metadata.get("Reason"));
        assertEquals(Lists.newArrayList(ErrorCode.INACTIVE_POLICY), metadata.get("ErrorCode"));
    }

    @Test
    @Ignore
    public void toResponse_shouldSetErrorCode_whenNotEligibleMultiplyException() {
        EJBExceptionMapper mapper = new EJBExceptionMapper();

        String message = "Member not active";
        Response response = mapper.toResponse(new EJBTransactionRolledbackException(message, new NotEligibleMultiplyException(message, ErrorCode.UNDERAGE_CUSTOMER)));

        assertNotNull(response);
        MultivaluedMap<String, Object> metadata = response.getMetadata();
        assertEquals(Lists.newArrayList(message),
                metadata.get("Reason"));
        assertEquals(Lists.newArrayList(ErrorCode.UNDERAGE_CUSTOMER), metadata.get("ErrorCode"));
    }

}