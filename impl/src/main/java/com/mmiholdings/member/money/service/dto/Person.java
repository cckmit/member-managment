package com.mmiholdings.member.money.service.dto;

import com.mmiholdings.service.money.commerce.member.Country;
import com.mmiholdings.service.money.commerce.member.Gender;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import lombok.Data;
import lombok.ToString;
import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by prince on 7/10/17.
 */
@Data
@ToString
public class Person implements Serializable {

    private String title;
    private String initials;
    private String name;
    private String surname;
    private Date dateOfBirth;
    private Gender gender;
    private String email;
    private String mobilePhoneNumber;
    private String idNumber;
    private String passportNumber;
    private String countryOfIssue;
    private Date issueDate;
    private Date expirationDate;

    /**
     * If no passport expiry date use today date plus 6 months
     * @return Expiry date
     */
    public Date getExpirationDate(){
        if(expirationDate == null){
            return new DateTime(new Date()).plusMonths(12).toDate();
        }
        else
            return expirationDate;
    }

    /**
     * If no issue date use current date
     * @return
     */
    public Date getIssueDate(){
        if(issueDate == null){

            return new DateTime(new Date()).toDate();
        }
        else
            return issueDate;
    }


    protected IdentificationDocument createIdentification() {
        if (getIdNumber() != null) {
            return IdentificationDocument.createSouthAfricanId(this.getIdNumber());
        } else if (getPassportNumber() != null) {
            return IdentificationDocument.createPassport(getPassportNumber(),
                    Country.valueOfAnyCode(getCountryOfIssue()), getExpirationDate(), getIssueDate());
        }
        return  null;
    }
}
