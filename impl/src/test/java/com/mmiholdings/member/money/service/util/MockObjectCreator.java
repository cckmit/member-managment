package com.mmiholdings.member.money.service.util;

import com.mmiholdings.member.money.service.clients.PolicyClient;
import com.mmiholdings.multiply.service.policy.Policy;
import com.mmiholdings.multiply.service.policy.PolicyNumber;
import com.mmiholdings.multiply.service.policy.entity.Address;
import com.mmiholdings.multiply.service.policy.entity.ContactDetails;
import com.mmiholdings.multiply.service.policy.entity.Entity;
import com.mmiholdings.multiply.service.policy.entity.Gender;
import org.mockito.Mockito;

import java.util.*;

public class MockObjectCreator {

    public static Entity createPolicyEntity() {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(1946,5,14);
        String idNumber = "4606145203089";
        int age = Calendar.getInstance().get(Calendar.YEAR)-1946;
        Date date = cal.getTime();
        Entity policyEntity = new Entity();
        policyEntity.setNames(new String[]{"Donald","John"});
        policyEntity.setSurname("Trump");
        policyEntity.setInitials("D.J");
        policyEntity.setAge(age);
        policyEntity.setIdNumber(idNumber);

        ContactDetails contactDetails = new ContactDetails();
        contactDetails.setPersonalEmail("potus@gmail.com");
        contactDetails.setHomePhone("01199090000");
        contactDetails.setWorkPhone("0119900001");
        contactDetails.setCellphone("+27728501000");

        com.mmiholdings.multiply.service.policy.entity.Address residentialAddress  =
                new Address();
        residentialAddress.setCode("3855");
        residentialAddress.setLines(
                new LinkedList<>(Arrays.asList("Unit 25","Lot 292, Maree Rd","Nkandla","Nkandla","Eastern Cape")));
        contactDetails.setResidentialAddress(residentialAddress);
        contactDetails.setPostalAddress(residentialAddress);

        policyEntity.setDateOfBirth((GregorianCalendar) cal);
        policyEntity.setContactDetails(contactDetails);

        policyEntity.setGender(Gender.male);

        return policyEntity;
    }

    public static Policy createMockPolicy() {
        Policy policy = new Policy() {
            public PolicyNumber getPolicyNumber() {
                PolicyNumber policyNumber = new PolicyNumber();
                policyNumber.setNumber(1234567L);
                return policyNumber;
            }

            public Entity getPolicyHolder() {
                return createPolicyEntity();
            }
        };
        return policy;
    }

    public static PolicyClient createPolicyClientMock() {
        PolicyClient policyClient = Mockito.mock(PolicyClient.class);
        Mockito.when(policyClient.getPolicy(Mockito.anyLong())).thenReturn(createMockPolicy());
        return policyClient;
    }

    public static MemberUtility createMemberUtilityMock() {
        MemberUtility memberUtility = Mockito.mock(MemberUtility.class);
        return memberUtility;
    }
}
