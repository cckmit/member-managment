package com.mmiholdings.member.money.service;

import com.google.common.base.Optional;
import com.mmiholdings.ces.basicauthclient.BasicAuth;
import com.mmiholdings.ces.basicauthclient.BasicAuthMode;
import com.mmiholdings.member.money.api.LinkCardRequest;
import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.member.money.api.MoneyMemberService;
import com.mmiholdings.member.money.api.domain.ApplicationResult;
import com.mmiholdings.member.money.api.dto.CashbackDeposit;
import com.mmiholdings.member.money.service.dto.Customer;
import com.mmiholdings.member.money.service.dto.DepositCashbackRequest;
import com.mmiholdings.multiply.service.entity.BusinessKeyType;
import com.mmiholdings.multiply.service.entity.FootPrint;
import com.mmiholdings.service.money.commerce.member.SouthAfricanIdDocument;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.java.Log;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;

import static javax.ws.rs.core.Response.ok;

@Stateless
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Money management member api")
@Log
@Path("/member")
public class MoneyMemberServiceRestApi {

    @EJB
    private MoneyMemberService memberService;

    @POST
    @Path("/apply/card/{cmsClientNumber}")
    @ApiOperation("Apply for new multiply money account")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applyVisaCard(@ApiParam(name = "customer", value = "Customer information applying", required = true)
                                                             Customer customerDTO,
                                                     @PathParam(value = "cmsClientNumber") String cmsClientNumber,
                                                     @ApiParam(name = "termsAndConditions", value = "Terms and conditions excepted")
                                                     @QueryParam(value = "termsAndConditions") boolean termsAndConditions,
                                                     @ApiParam(name = "agentId", value = "User applying for account")
                                                     @QueryParam(value = "agentId") String agentId) throws Exception {

        log.log(Level.FINER, "Apply for new Visa Card, CustomerDTO {0}", customerDTO);
        com.mmiholdings.service.money.commerce.member.Customer customer = customerDTO.toCustomer();

        Optional<com.mmiholdings.service.money.commerce.member.Customer> customerSaved = memberService.applyVisaCard(cmsClientNumber, customer, termsAndConditions, agentId);

        log.log(Level.FINER, "Multiply Visa Card result is: {0}", customerSaved.isPresent());

        return Response.ok().build();
    }

    @POST
    @Path("/apply/multiply-money/{cmsClientNumber}")
    @ApiOperation("Apply for new multiply money account")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applyForMultiplyMoney(@ApiParam(name = "customer", value = "Customer information applying", required = true)
                                                  Customer customerDTO,
                                          @PathParam(value = "cmsClientNumber") String cmsClientNumber,
                                          @ApiParam(name = "termsAndConditions", value = "Terms and conditions excepted")
                                          @QueryParam(value = "termsAndConditions") boolean termsAndConditions,
                                          @ApiParam(name = "agentId", value = "User applying for account")
                                          @QueryParam(value = "agentId") String agentId) throws Exception {

        log.log(Level.FINER, "Apply for new account, CustomerDTO {0}", customerDTO.toString());
        com.mmiholdings.service.money.commerce.member.Customer customer = customerDTO.toCustomer();

        ApplicationResult applicationResult = memberService.applyForNewAccount(cmsClientNumber, customer, termsAndConditions, agentId);

        log.log(Level.FINER, "Multiply Money Application result is: {0}", applicationResult);
        if (applicationResult != null && applicationResult.getErrorMessages().size() > 0) {
            throw new MemberManagementException(applicationResult.getFullErrorMessage());
        }

        return Response.ok().header("Reference", applicationResult.getApplicationId()).build();
    }

    @POST
    @Path("/validate/fica/{cmsClientNumber}/")
    @ApiOperation("Validate fica for Customer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validateFica(@ApiParam(name = "customer", value = "Customer whose FICA status is to be verified", required = true)
                                         Customer customerDTO,
                                 @PathParam(value = "cmsClientNumber") String cmsClientNumber,
                                 @ApiParam(name = "agentId", value = "User applying for account")
                                 @QueryParam(value = "agentId") String agentId) throws Exception {

        log.log(Level.FINER, "Performing FICA verification for CustomerDTO {0}", customerDTO.toString());
        com.mmiholdings.service.money.commerce.member.Customer customer = customerDTO.toCustomer();

        memberService.validateFica(customer, cmsClientNumber, agentId);

        return Response.ok().build();
    }

    @POST
    @Path("/validate/multiply-money/{cmsClientNumber}/{memberReference}")
    @ApiOperation("Apply for new multiply money account")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validateElegibility(@ApiParam(name = "customer", value = "Customer containing date of birth and identification", required = true)
                                                Customer customerDTO,
                                        @PathParam(value = "cmsClientNumber") String cmsClientNumber,
                                        @PathParam(value = "memberReference") String memberReference,
                                        @ApiParam(name = "agentId", value = "User applying for account")
                                        @QueryParam(value = "agentId") String agentId) throws Exception {

        log.log(Level.FINER, "Apply for new account, CustomerDTO {0}", customerDTO.toString());
        com.mmiholdings.service.money.commerce.member.Customer customer = customerDTO.toCustomer();

        FootPrint footprint = FootPrint.builder().build();
        footprint.putKey(BusinessKeyType.CMS_CDI_KEY, cmsClientNumber);
        footprint.putKey(BusinessKeyType.MONEY_CDI_KEY, memberReference);

        memberService.checkEligibility(footprint, customer);

        return Response.ok().build();
    }

    @POST
    @Path("/apply/multiply-money/light/{cmsClientNumber}")
    @ApiOperation("Apply for new multiply money account")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applyForMultiplyMoneyLightweight(@ApiParam(name = "customer", value = "Customer information applying", required = true)
                                                             Customer customerDTO,
                                                     @PathParam(value = "cmsClientNumber") String cmsClientNumber,
                                                     @ApiParam(name = "termsAndConditions", value = "Terms and conditions excepted")
                                                     @QueryParam(value = "termsAndConditions") boolean termsAndConditions,
                                                     @ApiParam(name = "agentId", value = "User applying for account")
                                                     @QueryParam(value = "agentId") String agentId) throws Exception {

        log.log(Level.FINER, "Apply for new account, CustomerDTO {0} - terms ", new Object[]{customerDTO, termsAndConditions});
        com.mmiholdings.service.money.commerce.member.Customer customer = customerDTO.toCustomer();

        ApplicationResult applicationResult = memberService.applyForLightWeightAccount(cmsClientNumber, customer, termsAndConditions, agentId);

        log.log(Level.FINER, "Multiply Money Application result is: {0}", applicationResult);
        if (applicationResult != null && applicationResult.getErrorMessages().size() > 0) {
            throw new MemberManagementException(applicationResult.getFullErrorMessage());
        }

        return Response.ok().header("Reference", applicationResult.getApplicationId()).build();
    }

    @POST
    @Path("/link-card/{accountReferenceOfCard}/{cardReference}/{accountReferenceToLinkTo}")
    @ApiOperation("Apply for new multiply money account")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response linkCard(@ApiParam(name = "accountReferenceToLinkTo", value = "Account reference to link card to")
                             @PathParam(value = "accountReferenceToLinkTo") String accountReferenceToLinkTo,
                             @ApiParam(name = "accountReferenceOfCard", value = "Account reference of card")
                             @PathParam(value = "accountReferenceOfCard") String accountReferenceOfCard,
                             @ApiParam(name = "cardReference", value = "Card reference of card to link.")
                             @PathParam(value = "cardReference") String cardReference,
                             @ApiParam(name = "agentId", value = "User applying for account")
                             @QueryParam(value = "agentId") String agentId) throws Exception {

        log.log(Level.FINER, "Linking card {0} of account {1} to account with reference {2}",
                new Object[]{cardReference, accountReferenceOfCard, accountReferenceToLinkTo});

        memberService.linkCard(new LinkCardRequest(accountReferenceToLinkTo, accountReferenceOfCard, cardReference, agentId));

        log.log(Level.FINER, "Linking card {0} of account {1} to account with reference {2} is successful.",
                new Object[]{cardReference, accountReferenceOfCard, accountReferenceToLinkTo});

        return Response.ok().build();
    }


    @POST
    @Path("/apply/multiply-money/auto/{cmsClientNumber}")
    @ApiOperation("Apply for new multiply money account")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response autoApplyForMultiplyMoney(@ApiParam(name = "customer", value = "Customer information applying", required = true)
                                                      Customer customerDTO,
                                              @PathParam(value = "cmsClientNumber") String cmsClientNumber,
                                              @ApiParam(name = "termsAndConditions", value = "Terms and conditions excepted")
                                              @QueryParam(value = "termsAndConditions") boolean termsAndConditions,
                                              @ApiParam(name = "agentId", value = "User applying for account")
                                              @QueryParam(value = "agentId") String agentId) throws Exception {

        log.log(Level.FINER, "Apply for new account, CustomerDTO {0}", customerDTO);
        com.mmiholdings.service.money.commerce.member.Customer customer = customerDTO.toCustomer();

        ApplicationResult applicationResult =
                memberService.autoApply(cmsClientNumber, customer, termsAndConditions, agentId);

        log.log(Level.FINER, "Multiply Money Application result is: {0}", applicationResult);
        if (applicationResult != null && applicationResult.getErrorMessages().size() > 0) {
            throw new MemberManagementException(applicationResult.getFullErrorMessage());
        }

        return Response.ok().header("Reference", applicationResult.getApplicationId()).build();
    }


    @POST
    @Path("/apply/multiply-money/auto/policy/{cmsClientNumber}")
    @ApiOperation("Apply for new multiply money account")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response autoApplyFromPolicy(@PathParam(value = "cmsClientNumber") String cmsClientNumber,
                                        @ApiParam(name = "agentId", value = "User applying for account")
                                        @QueryParam(value = "agentId") String agentId) throws Exception {

        ApplicationResult applicationResult = memberService.autoApply(cmsClientNumber, null, false, agentId);

        log.log(Level.FINER, "Multiply Money Application result is: {0}", applicationResult);
        if (applicationResult != null && applicationResult.getErrorMessages().size() > 0) {
            throw new MemberManagementException(applicationResult.getFullErrorMessage());
        }

        return Response.ok().header("Reference", applicationResult.getApplicationId()).build();
    }


    @POST
    @Path("/money/{memberReference}/cashback/{transactionId}")
    @ApiOperation("Deposit cashback in member designated account")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @BasicAuth(mode = BasicAuthMode.WHITELIST, allowedCredentialsProperty = "cashback.allowedcredentials", allowedHostsProperty = "cashback.allowedhosts")
    public Response depositCashback(@NotNull
                                    @PathParam(value = "memberReference")
                                    @ApiParam(value = "Member Reference", required = true)
                                            String memberReference,
                                    @PathParam(value = "transactionId")
                                    @ApiParam(value = "Transaction Id", required = true)
                                            String transactionId,
                                    DepositCashbackRequest depositCashbackRequest) throws Exception {

        String reference = memberService.depositCashback(CashbackDeposit.builder()
                .memberReference(memberReference)
                .amount(depositCashbackRequest.getAmount())
                .description(depositCashbackRequest.getDescription())
                .transactionId(transactionId)
                .userId(depositCashbackRequest.getUserId())
                .build());

        return ok(reference).build();
    }
}
