package com.mmiholdings.member.money.service.dto;

import com.mmiholdings.member.money.api.util.SouthAfricanMobileFormatter;
import com.mmiholdings.service.money.commerce.member.CardHolder;
import com.mmiholdings.service.money.commerce.member.RelationCode;
import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by prince on 7/10/17.
 */
@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountUser extends Person implements Serializable {

    @XmlAttribute(required = true)
    private RelationCode relationCode;

    public CardHolder toCardHolder() {
        CardHolder cardHolder = new CardHolder();

        cardHolder.setTitle(this.getTitle());
        cardHolder.setInitials(this.getInitials());
        cardHolder.setName(this.getName());
        cardHolder.setSurname(this.getSurname());
        cardHolder.setIdentificationDocument(createIdentification());
        cardHolder.setGender(this.getGender());
        cardHolder.setDateOfBirth(this.getDateOfBirth());
        cardHolder.setEmail(this.getEmail());
        cardHolder.setMobilePhoneNumber(SouthAfricanMobileFormatter.addSouthAfricanCodeToPhoneNumber(this.getMobilePhoneNumber()));
        cardHolder.setRelationshipCode(this.getRelationCode());
        return cardHolder;
    }

    public void applyDefaults() {
        if(getRelationCode() == null){
            setRelationCode(RelationCode.MAIN_MEMBER);
        }
    }
}
