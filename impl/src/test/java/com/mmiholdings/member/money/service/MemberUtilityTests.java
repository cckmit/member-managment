package com.mmiholdings.member.money.service;

import com.mmiholdings.member.money.service.clients.PolicyClient;
import com.mmiholdings.member.money.service.util.MemberUtility;
import com.mmiholdings.member.money.service.util.MockObjectCreator;
import com.mmiholdings.multiply.service.policy.entity.Entity;
import com.mmiholdings.service.money.commerce.member.Address;
import com.mmiholdings.service.money.commerce.member.BusinessNotFoundException;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import lombok.extern.java.Log;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mmiholdings.multiply.service.policy.Policy;

import java.util.LinkedList;

@Log
@RunWith(MockitoJUnitRunner.Silent.class)
public class MemberUtilityTests {

    @Test
    public void customerFromPolicy_DefaultValues() throws IdentificationDocument.InvalidRsaIdCheckDigit, BusinessNotFoundException {

        final String cmsClient = "12345678";

        Entity policyEntity = MockObjectCreator.createPolicyEntity();
        policyEntity.getContactDetails().getResidentialAddress().setLines(new LinkedList<>());
        policyEntity.getContactDetails().getResidentialAddress().setCode("");

        PolicyClient policyClient = Mockito.mock(PolicyClient.class);
        Policy policy = new Policy() {
            @Override
            public Entity getPolicyHolder() {
                return policyEntity;
            }
        };

        Mockito.when(policyClient.getPolicy(Long.valueOf(cmsClient))).thenReturn(policy);

        MemberUtility memberUtility = new MemberUtility();
        memberUtility.setPolicyClient(policyClient);

        Customer customer = memberUtility.getCustomer(cmsClient);
        Address physicalAddress = customer.getPhysicalAddress();

        Assert.assertNotNull(customer);
        Assert.assertNotNull(physicalAddress);
        Assert.assertEquals(physicalAddress.getLine1(),"DEFAULT999");
        Assert.assertEquals(physicalAddress.getLine2(),"DEFAULT999");
        Assert.assertEquals(physicalAddress.getSuburb(),"DEFAULT999");
        Assert.assertEquals(physicalAddress.getPostalCode(),"9999");
    }

    @Test
    public void customerFromPolicy() throws IdentificationDocument.InvalidRsaIdCheckDigit, BusinessNotFoundException {

        final String cmsClient = "12345678";

        Entity policyEntity = MockObjectCreator.createPolicyEntity();

        PolicyClient policyClient = Mockito.mock(PolicyClient.class);
        Policy policy = new Policy() {
            @Override
            public Entity getPolicyHolder() {
                return policyEntity;
            }
        };

        Mockito.when(policyClient.getPolicy(Long.valueOf(cmsClient))).thenReturn(policy);

        MemberUtility memberUtility = new MemberUtility();
        memberUtility.setPolicyClient(policyClient);

        Customer customer = memberUtility.getCustomer(cmsClient);
        Address physicalAddress = customer.getPhysicalAddress();

        com.mmiholdings.multiply.service.policy.entity.Address residentialAddress =
                policyEntity.getContactDetails().getResidentialAddress();
        System.out.println(residentialAddress.getLines());
        System.out.println(customer);
        Assert.assertNotNull(customer);
        Assert.assertNotNull(physicalAddress);
        Assert.assertEquals(physicalAddress.getLine1(),residentialAddress.getLine(1));
        Assert.assertEquals(physicalAddress.getLine2(),residentialAddress.getLine(2));
        Assert.assertEquals(physicalAddress.getSuburb(),residentialAddress.getLine(residentialAddress.getLines().size() - 1));
        Assert.assertEquals(physicalAddress.getPostalCode(),residentialAddress.getCode());
    }
}
