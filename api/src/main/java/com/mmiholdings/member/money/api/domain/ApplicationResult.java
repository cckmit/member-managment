package com.mmiholdings.member.money.api.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class ApplicationResult implements Serializable{

    private Boolean isValidationOnly;
    private String applicationId;
    private String message;

    private final List<String> errorMessages = new LinkedList<>();

    public void addErrorMessage(String error) {
        errorMessages.add(error);
    }

    public String getResultCode() {
        if(errorMessages.size() > 0) {
            return "FAILURE";
        }

        return "SUCCESS";
    }

    public String getFullErrorMessage() {
        StringBuilder message = new StringBuilder();
        String sep = "";
        for (String errorMessage : errorMessages) {
            message.append(sep).append(errorMessage);
            sep = ", ";
        }
        return message.toString();
    }
}
