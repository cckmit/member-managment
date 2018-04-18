package com.mmiholdings.member.money.service.clients.dto;

import com.mmiholdings.service.money.commerce.member.Customer;
import lombok.Data;

import java.io.Serializable;

@Data
public class MemberUpdateRequest extends CommerceApiRequest implements Serializable {
    private Customer customer;
}
