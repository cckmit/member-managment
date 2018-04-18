package com.mmiholdings.member.money.api.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class CashbackDeposit implements Serializable {
    private String memberReference;
    private long amount;
    private String transactionId;
    private String description;
    private String userId;
}
