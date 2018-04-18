package com.mmiholdings.member.money.api.domain;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(schema = "money", name = "account_creation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreation implements Serializable {

    public static final String ACCOUNT_TYPE = "account_type";
    public static final String ACCOUNT_CREATION_ID = "account_creation_id";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = ACCOUNT_CREATION_ID)
    private Long memberApplicationId;

    @Column(name = ACCOUNT_TYPE)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(name = "cms_client_number")
    private String cmsClientNumber;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified_date")
    private Date modifiedDate;

    @Column(name = "request_data")
    @Lob
    private String requestData;

    @Column(name = "result_code")
    private String resultCode;

    @Column(name = "message")
    private String message;
}
