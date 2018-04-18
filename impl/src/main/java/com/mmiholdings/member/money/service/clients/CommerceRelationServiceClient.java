package com.mmiholdings.member.money.service.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmiholdings.member.money.api.rule.CardLinkingException;
import com.mmiholdings.member.money.service.clients.dto.MemberUpdateRequest;
import com.mmiholdings.service.money.commerce.member.CommerceException;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.CustomerNotFound;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import com.mmiholdings.service.money.commerce.relation.CommerceResponse;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;

import static com.mmiholdings.service.money.commerce.relation.CommerceResponse.Response.FAILURE;

@Log
@NoArgsConstructor
public class CommerceRelationServiceClient extends CommerceClient {

    public CommerceRelationServiceClient(String url) {
        super(url);
    }

    /**
     * Creates or update customer in Traderoot
     *
     * @param updateRequest
     * @return member reference
     * @throws CommerceException
     */
    public String createOrUpdateMember(MemberUpdateRequest updateRequest) throws CommerceException {
        String uriTemplate = getUrl() + "/relation/createOrUpdateMember/";
        ClientRequest clientRequest = new ClientRequest(uriTemplate);
        Customer customer = updateRequest.getCustomer();
        log.log(Level.INFO, "Calling Create / Update Member Service... {0} : {1}", new Object[]{uriTemplate
                , customer});
        ObjectMapper objectMapper = new ObjectMapper();
        String customerJson;

        try {
            customerJson = objectMapper.writeValueAsString(customer);
        } catch (JsonProcessingException e) {
            throw new CommerceException("Unable to parse Customer");
        }

        clientRequest.accept("application/json");
        clientRequest.body("application/json", customerJson);

        log.log(Level.INFO, "JSON ... {0} : {1}", new Object[]{uriTemplate, customerJson});

        ClientResponse<CommerceResponse> response = null;
        try {
            response = clientRequest.post(CommerceResponse.class);
            checkStatusCodes(response);
        } catch (Throwable e) {
            throw new CommerceException(e.getMessage());
        }

        CommerceResponse commerceResponse = response.getEntity();

        if (commerceResponse.getResponse() == FAILURE) {
            throw new CommerceException(commerceResponse.getMessage());
        }
        return commerceResponse.getReference();

    }

    /**
     * Finds customer using Traderoot Member Reference number (i.e. Money Management Number)
     *
     * @param memberReference
     * @return customer
     */
    public Customer findMemberUsingMemberReference(String memberReference) throws CustomerNotFound {
        WebTarget resource = getResource(getUrl(), null,
                "member", "customerReference", memberReference);

        log.log(Level.INFO, "Calling Find Member By Member Number Service... {0}", resource.getUri());
        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();
        checkStatusCodes(response);

        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            throw new CustomerNotFound(memberReference);
        }
        Customer customer = response.readEntity(Customer.class);

        customer = CustomerConverter.convertSubtypes(customer);

        return customer;
    }

    /**
     * Finds customer using Traderoot Member Reference number (i.e. Money Management Number)
     *
     * @param idNumber
     * @return customer
     */
    public Customer findMemberUsingIdNumber(String idNumber, IdentificationDocument.Type idType) throws CustomerNotFound {
        WebTarget resource = getResource(getUrl(), null,
                "relation", "find", "idNumber", idType.name(), idNumber);

        log.log(Level.INFO, "Calling Find Member By Member Number Service... {0}", resource.getUri());
        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();
        checkStatusCodes(response);

        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            throw new CustomerNotFound(IdentificationDocument.getIdentificationDocument(idNumber, idType));
        }
        Customer customer = response.readEntity(Customer.class);
        return CustomerConverter.convertSubtypes(customer);
    }

    private String parseResponse(CommerceResponse commerceResponse) throws CommerceException {
        log.log(Level.INFO, "Calling Find Member By Member Number Service response... {0} {1} {2} {3}",
                new Object[]{
                commerceResponse.getResponse(), commerceResponse.getMessage(), commerceResponse.getErrors(),
                commerceResponse.getResponseCode()});
        if (commerceResponse.getResponse() == FAILURE) {
            throw new CommerceException(commerceResponse.getResponse().name(),commerceResponse.getMessage(),commerceResponse.getErrors());
        }
        return commerceResponse.getReference();
    }


    private void checkStatusCodes(Response response) {
        if (!(response.getStatus() == Response.Status.OK.getStatusCode()
                || response.getStatus() == Response.Status.NO_CONTENT.getStatusCode())) {
            throw new RuntimeException(String.format("Unexpected status code from %s: %d",
                    getClass().getSimpleName(),
                    response.getStatus()));
        }
    }

    private WebTarget getWebTarget(MemberUpdateRequest updateRequest, WebTarget webResource) {
        if (updateRequest.getUserId() != null) {
            webResource = webResource.queryParam("agentId", updateRequest.getUserId());
        }
        return webResource;
    }


    public void linkCard(String cardReference, String accountReference, String userId) throws CardLinkingException {
        WebTarget resource = getResource(getUrl(), null,
                "relation", "link", cardReference, accountReference).queryParam("userId", userId);

        log.log(Level.INFO, "Calling Card-Account linking... {0}", resource.getUri());
        Response response = resource.request().post(null);
        int status = response.getStatus();
        log.log(Level.INFO, "Card-Account linking returned: {0}", status);
        if (status != 204) {
            throw new CardLinkingException(response.getHeaderString("Reason"));
        }
    }

    public Customer findMemberUsingAccountReference(String accountReference, String userId) throws CustomerNotFound {
        WebTarget resource = getResource(getUrl(), null,
                "relation", "find", "account", accountReference);

        log.log(Level.INFO, "Calling find member usiong account reference ... {0}", resource.getUri());
        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();
        checkStatusCodes(response);

        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            throw new CustomerNotFound(accountReference);
        }
        Customer customer = response.readEntity(Customer.class);
        return CustomerConverter.convertSubtypes(customer);
    }
}
