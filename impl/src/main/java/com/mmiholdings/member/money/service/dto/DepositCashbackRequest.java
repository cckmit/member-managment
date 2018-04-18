package com.mmiholdings.member.money.service.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DepositCashbackRequest implements Serializable {
    private long amount;
    private String description;
    private String userId;
}
