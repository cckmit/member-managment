package com.mmiholdings.member.money.service.clients.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class CommerceApiRequest implements Serializable {
    private String userId;
}
