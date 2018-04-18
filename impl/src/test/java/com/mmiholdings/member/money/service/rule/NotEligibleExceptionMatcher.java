package com.mmiholdings.member.money.service.rule;

import com.mmiholdings.member.money.api.rule.ErrorCode;
import com.mmiholdings.member.money.api.rule.NotEligibleException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class NotEligibleExceptionMatcher extends BaseMatcher<NotEligibleException> {
    private final ErrorCode expectedCode;

    public NotEligibleExceptionMatcher(ErrorCode expectedCode) {
        this.expectedCode = expectedCode;
    }

    @Override
    public boolean matches(Object item) {
        NotEligibleException e = (NotEligibleException) item;
        return expectedCode.equals(e.getErrorCode());
    }

    @Override
    public void describeTo(Description description) {

    }
}
