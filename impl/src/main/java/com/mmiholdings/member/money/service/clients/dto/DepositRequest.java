package com.mmiholdings.member.money.service.clients.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DepositRequest extends CommerceApiRequest implements Serializable {
    private String accountReference;
    private long amount;
    private String transactionId;
    private String description;
    private String userId;
}
